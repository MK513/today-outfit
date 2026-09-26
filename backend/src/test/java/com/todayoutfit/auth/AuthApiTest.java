package com.todayoutfit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.todayoutfit.IntegrationTest;
import com.todayoutfit.user.User;
import com.todayoutfit.user.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbc;

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions signup(String email, String password, String confirm, String name) throws Exception {
        return postJson("/auth/signup", """
                {"email":"%s","password":"%s","password_confirm":"%s","name":"%s"}
                """.formatted(email, password, confirm, name));
    }

    private String loginToken(String email, String password) throws Exception {
        String body = postJson("/auth/login", """
                {"email":"%s","password":"%s"}
                """.formatted(email, password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.access_token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- 회원가입 ----------

    @Test
    void signupCreatesUserWithHashedPasswordAndNormalizedEmail() throws Exception {
        String email = uniqueEmail();
        signup("  " + email.toUpperCase() + " ", "password1", "password1", " 김민 ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("김민"))
                .andExpect(jsonPath("$.is_demo").value(false))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("password1").startsWith("$2");
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        signup(email, "password1", "password1", "a").andExpect(status().isCreated());

        signup(email.toUpperCase(), "password1", "password1", "b")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("EMAIL_DUPLICATED"));
    }

    @Test
    void signupReservesDemoEmail() throws Exception {
        signup(DemoAccountSeeder.DEMO_EMAIL, "password1", "password1", "x")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("EMAIL_DUPLICATED"));
    }

    @Test
    void signupRejectsPasswordMismatch() throws Exception {
        signup(uniqueEmail(), "password1", "password2", "a")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("PASSWORD_MISMATCH"));
    }

    @Test
    void signupValidatesPasswordLengthAndEmailFormat() throws Exception {
        signup(uniqueEmail(), "short", "short", "a")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상 72자 이하로 입력해주세요."));

        signup("not-an-email", "password1", "password1", "a")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("올바른 이메일 형식이 아닙니다."));
    }

    // ---------- 로그인 · 토큰 ----------

    @Test
    void loginReturnsTokenUsableForMe() throws Exception {
        String email = uniqueEmail();
        signup(email, "password1", "password1", "김민");

        String body = postJson("/auth/login", """
                {"email":"%s","password":"password1"}
                """.formatted(email.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.access_token");

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("김민"));
    }

    @Test
    void loginFailsWithSameMessageForWrongPasswordAndUnknownEmail() throws Exception {
        String email = uniqueEmail();
        signup(email, "password1", "password1", "a");

        for (String json : new String[] {
                "{\"email\":\"%s\",\"password\":\"wrong-pass\"}".formatted(email),
                "{\"email\":\"%s\",\"password\":\"password1\"}".formatted(uniqueEmail())}) {
            postJson("/auth/login", json)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    @Test
    void protectedEndpointRequiresValidToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer("not.a.jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"));
    }

    @Test
    void logoutRevokesToken() throws Exception {
        String email = uniqueEmail();
        signup(email, "password1", "password1", "a");
        String token = loginToken(email, "password1");

        mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());

        // 다시 로그인하면 새 토큰은 정상 동작
        String fresh = loginToken(email, "password1");
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(fresh)))
                .andExpect(status().isOk());
    }

    // ---------- 탈퇴 ----------

    @Test
    void withdrawalRequiresCurrentPassword() throws Exception {
        String email = uniqueEmail();
        signup(email, "password1", "password1", "a");
        String token = loginToken(email, "password1");

        mockMvc.perform(post("/users/me/withdrawal").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"wrong-pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_PASSWORD"));

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    void withdrawalDeletesUserAndAllOwnedDataAndAllowsReSignup() throws Exception {
        // 샘플 데이터가 있는 계정으로 연쇄 삭제까지 확인하기 위해 데모 계정을 사용한다.
        String token = JsonPath.read(postJson("/auth/demo", "").andReturn().getResponse().getContentAsString(),
                "$.access_token");
        Long demoId = userRepository.findByEmail(DemoAccountSeeder.DEMO_EMAIL).orElseThrow().getId();

        mockMvc.perform(post("/users/me/withdrawal").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"demo1234\"}"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(demoId)).isEmpty();
        for (String table : new String[] {"clothes", "outfits", "schedules"}) {
            assertThat(jdbc.queryForObject("select count(*) from " + table + " where user_id = ?", Long.class, demoId))
                    .as(table).isZero();
        }
        // 탈퇴에 쓴 토큰은 무효화된다.
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
        // 데모 체험을 다시 누르면 새 데모 계정이 만들어진다.
        postJson("/auth/demo", "").andExpect(status().isOk());
        assertThat(userRepository.findByEmail(DemoAccountSeeder.DEMO_EMAIL).orElseThrow().getId()).isNotEqualTo(demoId);
    }

    // ---------- 데모 ----------

    @Test
    void demoLoginCreatesSampleDataOnceAndReusesAccount() throws Exception {
        String first = postJson("/auth/demo", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.is_demo").value(true))
                .andExpect(jsonPath("$.user.email").value(DemoAccountSeeder.DEMO_EMAIL))
                .andReturn().getResponse().getContentAsString();
        Integer firstId = JsonPath.read(first, "$.user.id");

        String second = postJson("/auth/demo", "").andReturn().getResponse().getContentAsString();
        Integer secondId = JsonPath.read(second, "$.user.id");
        assertThat(secondId).isEqualTo(firstId);

        long userId = firstId.longValue();
        assertThat(jdbc.queryForObject("select count(*) from clothes where user_id = ?", Long.class, userId)).isEqualTo(14);
        assertThat(jdbc.queryForObject("select count(*) from outfits where user_id = ?", Long.class, userId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select plan_date from schedules where user_id = ?", LocalDate.class, userId))
                .isEqualTo(LocalDate.now().plusDays(1));
        assertThat(jdbc.queryForObject(
                "select count(*) from outfit_items i join outfits o on o.id = i.outfit_id where o.user_id = ?",
                Long.class, userId)).isEqualTo(7);
    }
}
