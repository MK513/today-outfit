package com.todayoutfit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** API 통합 테스트 공통 도우미: 가입 · 로그인, 옷 · 코디 생성, 토큰을 붙인 요청. */
public class TestApi {

    private final MockMvc mockMvc;

    public TestApi(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /** 새 사용자를 만들고 access token을 돌려준다. */
    public String signupAndLogin() throws Exception {
        String email = "t-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"password1","password_confirm":"password1","name":"테스터"}
                """.formatted(email))).andExpect(status().isCreated());
        String body = mockMvc.perform(MockMvcRequestBuilders.post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"password1\"}".formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.access_token");
    }

    public ResultActions getAs(String token, String path, Object... vars) throws Exception {
        return mockMvc.perform(withAuth(MockMvcRequestBuilders.get(path, vars), token));
    }

    public ResultActions postAs(String token, String path, String json) throws Exception {
        return mockMvc.perform(withAuth(MockMvcRequestBuilders.post(path), token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions putAs(String token, String path, String json) throws Exception {
        return mockMvc.perform(withAuth(MockMvcRequestBuilders.put(path), token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public ResultActions deleteAs(String token, String path, Object... vars) throws Exception {
        return mockMvc.perform(withAuth(MockMvcRequestBuilders.delete(path, vars), token));
    }

    /** 사진 없는 옷을 만들고 id를 돌려준다. */
    public long createClothing(String token, String name, String category) throws Exception {
        return id(postAs(token, "/clothes", """
                {"name":"%s","category":"%s","color":"블랙","season":"ALL","source":"MANUAL"}
                """.formatted(name, category)).andExpect(status().isCreated()), "$.id");
    }

    /** 코디 저장 요청 JSON. extra는 쉼표 없이 추가 필드를 넣는다(예: "\"ai_score\":87"). */
    public static String outfitJson(String name, String source, String extra, long... clothingIds) {
        String ids = Arrays.stream(clothingIds).mapToObj(String::valueOf).collect(Collectors.joining(","));
        return "{\"name\":\"%s\",\"source\":\"%s\",\"clothing_ids\":[%s]%s}"
                .formatted(name, source, ids, extra == null ? "" : "," + extra);
    }

    /** MANUAL 코디를 만들고 id를 돌려준다. */
    public long createOutfit(String token, String name, long... clothingIds) throws Exception {
        return id(postAs(token, "/outfits", outfitJson(name, "MANUAL", null, clothingIds))
                .andExpect(status().isCreated()), "$.id");
    }

    public static long id(ResultActions result, String path) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), path)).longValue();
    }

    private static MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder, String token) {
        return token == null ? builder : builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
