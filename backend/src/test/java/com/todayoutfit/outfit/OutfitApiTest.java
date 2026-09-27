package com.todayoutfit.outfit;

import static com.todayoutfit.TestApi.outfitJson;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayoutfit.IntegrationTest;
import com.todayoutfit.TestApi;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class OutfitApiTest {

    @Autowired
    MockMvc mockMvc;

    private TestApi api;
    private String token;
    private String otherToken;
    private long a;
    private long b;
    private long c;

    @BeforeEach
    void setUp() throws Exception {
        api = new TestApi(mockMvc);
        token = api.signupAndLogin();
        otherToken = api.signupAndLogin();
        a = api.createClothing(token, "화이트 셔츠", "TOP");
        b = api.createClothing(token, "블랙 슬랙스", "BOTTOM");
        c = api.createClothing(token, "스니커즈", "SHOES");
    }

    // ---------- 저장 · 상세 ----------

    @Test
    void createReturnsDetailWithItemsInRequestOrder() throws Exception {
        api.postAs(token, "/outfits", outfitJson(" 주말룩 ", "MANUAL", "\"memo\":\"편하게\"", c, a, b))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("주말룩"))
                .andExpect(jsonPath("$.memo").value("편하게"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].item_order").value(1))
                .andExpect(jsonPath("$.items[0].clothing.id").value(c))
                .andExpect(jsonPath("$.items[1].clothing.id").value(a))
                .andExpect(jsonPath("$.items[2].item_order").value(3))
                .andExpect(jsonPath("$.items[2].clothing.id").value(b))
                .andExpect(jsonPath("$.items[0].clothing.name").value("스니커즈"))
                .andExpect(jsonPath("$.items[0].clothing.category").value("SHOES"))
                .andExpect(jsonPath("$.items[0].clothing.color").value("블랙"))
                .andExpect(jsonPath("$.items[0].clothing").value(org.hamcrest.Matchers.hasKey("image_url")))
                .andExpect(jsonPath("$.ai_tags", empty()))
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    void getReturnsSameDetail() throws Exception {
        long id = api.createOutfit(token, "출근룩", a, b);

        api.getAs(token, "/outfits/{id}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("출근룩"))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void aiFieldsSavedOnlyForAiSource() throws Exception {
        api.postAs(token, "/outfits", outfitJson("AI룩", "AI",
                        "\"request_text\":\"친구와 카페\",\"ai_reason\":\"활동성을 고려했어요\"", a, b))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.request_text").value("친구와 카페"))
                .andExpect(jsonPath("$.ai_reason").value("활동성을 고려했어요"));

        api.postAs(token, "/outfits", outfitJson("직접룩", "MANUAL",
                        "\"request_text\":\"무시됨\",\"ai_reason\":\"무시됨\",\"ai_score\":90", a, b))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.request_text").isEmpty())
                .andExpect(jsonPath("$.ai_reason").isEmpty())
                .andExpect(jsonPath("$.ai_score").isEmpty());
    }

    @Test
    void challengeFieldsSavedOnlyForChallengeSource() throws Exception {
        api.postAs(token, "/outfits", outfitJson("챌린지룩", "CHALLENGE",
                        "\"ai_score\":87,\"ai_comment\":\"깔끔해요\",\"ai_tags\":[\"미니멀\",\"미니멀\",\"오피스룩\"]", a, b))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ai_score").value(87))
                .andExpect(jsonPath("$.ai_comment").value("깔끔해요"))
                .andExpect(jsonPath("$.ai_tags", contains("미니멀", "오피스룩")));

        api.postAs(token, "/outfits", outfitJson("AI룩", "AI", "\"ai_score\":87,\"ai_tags\":[\"미니멀\"]", a))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ai_score").isEmpty())
                .andExpect(jsonPath("$.ai_tags", empty()));
    }

    @Test
    void challengeWithoutEvaluationIsSaved() throws Exception {
        api.postAs(token, "/outfits", outfitJson("평가 실패", "CHALLENGE", null, a, b, c))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ai_score").isEmpty())
                .andExpect(jsonPath("$.ai_comment").isEmpty())
                .andExpect(jsonPath("$.ai_tags", empty()));
    }

    // ---------- 검증 ----------

    @Test
    void othersOrMissingClothingIsInvalidClothing() throws Exception {
        long others = api.createClothing(otherToken, "남의 옷", "TOP");

        api.postAs(token, "/outfits", outfitJson("룩", "MANUAL", null, a, others))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_CLOTHING"))
                .andExpect(jsonPath("$.message").value("내 옷장에 없는 옷이 포함되어 있어요."));
        api.postAs(token, "/outfits", outfitJson("룩", "MANUAL", null, a, 999_999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_CLOTHING"));
    }

    @Test
    void validatesRequest() throws Exception {
        String twentyOne = LongStream.rangeClosed(1, 21).mapToObj(String::valueOf).collect(Collectors.joining(","));
        String[] invalid = {
                outfitJson("  ", "MANUAL", null, a),
                outfitJson("룩", "MANUAL", null),
                outfitJson("룩", "MANUAL", null, a, a),
                "{\"name\":\"룩\",\"source\":\"MANUAL\",\"clothing_ids\":[" + twentyOne + "]}",
                outfitJson("룩", "CHALLENGE", "\"ai_score\":101", a),
                outfitJson("룩", "CHALLENGE", "\"ai_tags\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"]", a),
                outfitJson("룩", "CHALLENGE", "\"ai_tags\":[\"" + "가".repeat(31) + "\"]", a),
                outfitJson("룩", "DRESS", null, a),
        };
        for (String json : invalid) {
            api.postAs(token, "/outfits", json)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
        }
    }

    @Test
    void blankTagIsRejected() throws Exception {
        api.postAs(token, "/outfits", outfitJson("룩", "CHALLENGE", "\"ai_tags\":[\"  \"]", a))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
    }

    @Test
    void othersOutfitIsNotFound() throws Exception {
        long others = api.createOutfit(otherToken, "남의 코디", api.createClothing(otherToken, "옷", "TOP"));

        api.getAs(token, "/outfits/{id}", others)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("삭제되었거나 존재하지 않는 코디입니다."));
    }

    @Test
    void createRequiresLogin() throws Exception {
        api.postAs(null, "/outfits", outfitJson("룩", "MANUAL", null, a))
                .andExpect(status().isUnauthorized());
    }
}
