package com.todayoutfit.clothing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.todayoutfit.IntegrationTest;
import com.todayoutfit.image.ImageProperties;
import com.todayoutfit.image.ImageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class ClothingApiTest {

    /** PNG 시그니처로 시작하는 바이트 (서버는 시그니처로 형식을 판별한다) */
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ImageProperties imageProperties;

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        token = signupAndLogin();
        otherToken = signupAndLogin();
    }

    // ---------- helpers ----------

    private String signupAndLogin() throws Exception {
        String email = "c-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"password1","password_confirm":"password1","name":"옷장"}
                """.formatted(email))).andExpect(status().isCreated());
        String body = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"password1"}
                """.formatted(email))).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.access_token");
    }

    private static String auth(String token) {
        return "Bearer " + token;
    }

    private static String clothingJson(String name, String category, String color, String season, String imageUrl) {
        return """
                {"name":"%s","category":"%s","color":"%s","season":"%s","source":"MANUAL"%s}
                """.formatted(name, category, color, season,
                imageUrl == null ? "" : ",\"image_url\":\"" + imageUrl + "\",\"image_file_name\":\"my.png\"");
    }

    private ResultActions create(String token, String json) throws Exception {
        return mockMvc.perform(post("/clothes").header(HttpHeaders.AUTHORIZATION, auth(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long createId(String token, String name, String category, String color, String season) throws Exception {
        String body = create(token, clothingJson(name, category, color, season, null))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions upload(String token, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/images").file(file).header(HttpHeaders.AUTHORIZATION, auth(token)));
    }

    private String uploadPng(String token) throws Exception {
        String body = upload(token, new MockMultipartFile("image", "my.png", "image/png", PNG))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.image_url");
    }

    /** /api/images/{userId}/{file} → 디스크 경로 (로컬 저장소) */
    private Path fileOf(String imageUrl) {
        String key = imageUrl.substring(imageProperties.local().publicPath().length() + 1);
        return imageProperties.local().dir().resolve(key);
    }

    // ---------- 등록 ----------

    @Test
    void createReturnsClothingInSpecShape() throws Exception {
        create(token, clothingJson("  블랙 슬랙스 ", "BOTTOM", "블랙", "ALL", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("블랙 슬랙스"))
                .andExpect(jsonPath("$.category").value("BOTTOM"))
                .andExpect(jsonPath("$.color").value("블랙"))
                .andExpect(jsonPath("$.season").value("ALL"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.image_url").isEmpty())
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    void createValidatesFields() throws Exception {
        create(token, clothingJson(" ", "TOP", "블랙", "ALL", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("의류 이름을 입력해주세요."));
        create(token, clothingJson("셔츠", "TOP", "빨강", "ALL", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
        create(token, clothingJson("셔츠", "DRESS", "블랙", "ALL", null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequiresLogin() throws Exception {
        mockMvc.perform(post("/clothes").contentType(MediaType.APPLICATION_JSON)
                        .content(clothingJson("셔츠", "TOP", "블랙", "ALL", null)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- 목록 · 필터 ----------

    @Test
    void listFiltersByCategorySeasonAndKoreanColorNewestFirst() throws Exception {
        createId(token, "화이트 셔츠", "TOP", "화이트", "SPRING");
        createId(token, "블랙 맨투맨", "TOP", "블랙", "ALL");
        createId(token, "블랙 슬랙스", "BOTTOM", "블랙", "ALL");
        createId(otherToken, "남의 블랙 셔츠", "TOP", "블랙", "ALL");

        mockMvc.perform(get("/clothes").header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_elements").value(3))
                .andExpect(jsonPath("$.content[0].name").value("블랙 슬랙스"))
                .andExpect(jsonPath("$.content[2].name").value("화이트 셔츠"));

        mockMvc.perform(get("/clothes").param("category", "TOP").param("color", "블랙")
                        .header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(jsonPath("$.total_elements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("블랙 맨투맨"));

        mockMvc.perform(get("/clothes").param("season", "SPRING").header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(jsonPath("$.total_elements").value(1));

        mockMvc.perform(get("/clothes").param("season", "WINTER").header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/clothes").param("page", "2").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.total_pages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void listRejectsUnknownFilterValues() throws Exception {
        mockMvc.perform(get("/clothes").param("color", "빨강").header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
    }

    // ---------- 상세 · 삭제 · 소유권 ----------

    @Test
    void otherUsersClothingIsNotFound() throws Exception {
        long id = createId(otherToken, "남의 옷", "TOP", "블랙", "ALL");

        mockMvc.perform(get("/clothes/{id}", id).header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("NOT_FOUND"));
        mockMvc.perform(delete("/clothes/{id}", id).header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/clothes/{id}", id).header(HttpHeaders.AUTHORIZATION, auth(otherToken)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteRemovesClothing() throws Exception {
        long id = createId(token, "지울 옷", "HAT", "브라운", "SUMMER");

        mockMvc.perform(delete("/clothes/{id}", id).header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/clothes/{id}", id).header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(status().isNotFound());
    }

    // ---------- 일괄 등록 ----------

    @Test
    void bulkCreatesAllItems() throws Exception {
        mockMvc.perform(post("/clothes/bulk").header(HttpHeaders.AUTHORIZATION, auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[%s,%s]}".formatted(
                                clothingJson("니트", "TOP", "그레이", "WINTER", null).trim(),
                                clothingJson("데님", "BOTTOM", "블루", "ALL", null).trim())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("데님"));
    }

    @Test
    void bulkSavesNothingWhenAnyItemIsInvalid() throws Exception {
        mockMvc.perform(post("/clothes/bulk").header(HttpHeaders.AUTHORIZATION, auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[%s,%s]}".formatted(
                                clothingJson("니트", "TOP", "그레이", "WINTER", null).trim(),
                                clothingJson("", "BOTTOM", "블루", "ALL", null).trim())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/clothes").header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(jsonPath("$.total_elements").value(0));
    }

    @Test
    void bulkAllowsAtMostEightItems() throws Exception {
        String item = clothingJson("양말", "ACC", "화이트", "ALL", null).trim();
        String nine = String.join(",", java.util.Collections.nCopies(9, item));
        mockMvc.perform(post("/clothes/bulk").header(HttpHeaders.AUTHORIZATION, auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[" + nine + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("한 번에 8벌까지 등록할 수 있어요."));
        mockMvc.perform(post("/clothes/bulk").header(HttpHeaders.AUTHORIZATION, auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- 사진 업로드 ----------

    @Test
    void uploadStoresImageAndServesItWithoutToken() throws Exception {
        String body = upload(token, new MockMultipartFile("image", "C:\\photos\\내 슬랙스.png", "image/png", PNG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.image_file_name").value("내 슬랙스.png"))
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(body, "$.image_url");
        assertThat(url).matches("/api/images/\\d+/[0-9a-f-]{36}\\.png");
        assertThat(Files.readAllBytes(fileOf(url))).isEqualTo(PNG);

        // MockMvc는 context-path(/api)를 빼고 요청한다.
        mockMvc.perform(get(url.substring("/api".length())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void uploadDetectsJpegBySignatureRegardlessOfExtension() throws Exception {
        String body = upload(token, new MockMultipartFile("image", "photo.jpeg", "image/jpeg", JPEG))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(body, "$.image_url")).endsWith(".jpg");
    }

    @Test
    void uploadRejectsNonImagesEmptyAndOversizedFiles() throws Exception {
        byte[] html = "<html><script>alert(1)</script>".getBytes();
        upload(token, new MockMultipartFile("image", "evil.png", "image/png", html))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_FILE"))
                .andExpect(jsonPath("$.message").value("5MB 이하의 JPG 또는 PNG 파일만 업로드할 수 있어요."));

        upload(token, new MockMultipartFile("image", "empty.png", "image/png", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_FILE"));

        byte[] big = Arrays.copyOf(PNG, (int) ImageService.MAX_SIZE_BYTES + 1);
        upload(token, new MockMultipartFile("image", "big.png", "image/png", big))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_FILE"));

        mockMvc.perform(multipart("/images").header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_FILE"));
    }

    @Test
    void uploadRequiresLogin() throws Exception {
        mockMvc.perform(multipart("/images").file(new MockMultipartFile("image", "a.png", "image/png", PNG)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void servingRejectsUnknownOrMalformedFileNames() throws Exception {
        mockMvc.perform(get("/images/1/" + UUID.randomUUID() + ".png")).andExpect(status().isNotFound());
        // 인코딩된 경로 구분자는 Spring Security 방화벽이 먼저 400으로 거부한다.
        mockMvc.perform(get("/images/1/..%2F..%2Fsecret.png")).andExpect(status().is4xxClientError());
    }

    // ---------- 사진 연결 · 정리 ----------

    @Test
    void createWithOwnUploadedImage() throws Exception {
        String url = uploadPng(token);

        String body = create(token, clothingJson("사진 있는 옷", "TOP", "블랙", "ALL", url))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.image_url").value(url))
                .andExpect(jsonPath("$.image_file_name").value("my.png"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        // DB에는 공개 URL이 아니라 저장소 키만 저장되고, 조회 응답에서 다시 URL로 바뀐다.
        String stored = jdbc.queryForObject("select image_url from clothes where id = ?", String.class, id);
        assertThat(stored).isEqualTo(url.substring("/api/images/".length())).doesNotStartWith("/");
        mockMvc.perform(get("/clothes/{id}", id).header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(jsonPath("$.image_url").value(url));
    }

    @Test
    void createRejectsImageUrlsNotUploadedByThisUser() throws Exception {
        String othersUrl = uploadPng(otherToken);

        for (String url : new String[] {othersUrl, "https://evil.example.com/a.png", "/api/images/1/../../x.png"}) {
            create(token, clothingJson("옷", "TOP", "블랙", "ALL", url))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
        }
    }

    @Test
    void deletingClothingDeletesItsImageFile() throws Exception {
        String url = uploadPng(token);
        String body = create(token, clothingJson("사진 옷", "TOP", "블랙", "ALL", url))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        assertThat(fileOf(url)).exists();

        mockMvc.perform(delete("/clothes/{id}", id).header(HttpHeaders.AUTHORIZATION, auth(token)))
                .andExpect(status().isNoContent());

        assertThat(fileOf(url)).doesNotExist();
    }

    @Test
    void withdrawalDeletesAllUploadedImages() throws Exception {
        String url = uploadPng(token);
        Path userDir = fileOf(url).getParent();
        assertThat(userDir).isDirectory();

        mockMvc.perform(post("/users/me/withdrawal").header(HttpHeaders.AUTHORIZATION, auth(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"password1\"}"))
                .andExpect(status().isNoContent());

        assertThat(userDir).doesNotExist();
    }
}
