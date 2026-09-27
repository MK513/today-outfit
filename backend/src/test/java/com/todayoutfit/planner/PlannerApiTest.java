package com.todayoutfit.planner;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayoutfit.IntegrationTest;
import com.todayoutfit.TestApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
@AutoConfigureMockMvc
class PlannerApiTest {

    @Autowired
    MockMvc mockMvc;

    private TestApi api;
    private String token;
    private String otherToken;
    private long top;
    private long bottom;
    private long outfitA;
    private long outfitB;

    @BeforeEach
    void setUp() throws Exception {
        api = new TestApi(mockMvc);
        token = api.signupAndLogin();
        otherToken = api.signupAndLogin();
        top = api.createClothing(token, "셔츠", "TOP");
        bottom = api.createClothing(token, "슬랙스", "BOTTOM");
        outfitA = api.createOutfit(token, "코디 A", top, bottom);
        outfitB = api.createOutfit(token, "코디 B", bottom);
    }

    private ResultActions place(String token, String date, long outfitId) throws Exception {
        return api.putAs(token, "/planner/schedules/" + date, "{\"outfit_id\":" + outfitId + "}");
    }

    // ---------- 기간 조회 ----------

    @Test
    void listReturnsRangeInDateOrderExcludingOthers() throws Exception {
        place(token, "2026-09-30", outfitB).andExpect(status().isCreated());
        place(token, "2026-09-28", outfitA).andExpect(status().isCreated());
        long others = api.createOutfit(otherToken, "남의 코디", api.createClothing(otherToken, "옷", "TOP"));
        place(otherToken, "2026-09-29", others).andExpect(status().isCreated());

        api.getAs(token, "/planner/schedules?start_date=2026-09-28&end_date=2026-10-04")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].plan_date").value("2026-09-28"))
                .andExpect(jsonPath("$[0].outfit.id").value(outfitA))
                .andExpect(jsonPath("$[0].outfit.name").value("코디 A"))
                .andExpect(jsonPath("$[0].outfit.thumbnails.length()").value(2))
                .andExpect(jsonPath("$[0].outfit.thumbnails[0].id").value(top))
                .andExpect(jsonPath("$[0].updated_at").isEmpty())
                .andExpect(jsonPath("$[1].plan_date").value("2026-09-30"));
    }

    @Test
    void listRejectsInvalidRange() throws Exception {
        api.getAs(token, "/planner/schedules?start_date=2026-09-01&end_date=2026-10-01")
                .andExpect(status().isOk());
        api.getAs(token, "/planner/schedules?start_date=2026-09-01&end_date=2026-10-02")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("조회 기간은 최대 31일이에요."));
        api.getAs(token, "/planner/schedules?start_date=2026-10-04&end_date=2026-09-28")
                .andExpect(status().isBadRequest());
        api.getAs(token, "/planner/schedules?end_date=2026-09-28")
                .andExpect(status().isBadRequest());
        api.getAs(token, "/planner/schedules?start_date=2026-09-XX&end_date=2026-09-28")
                .andExpect(status().isBadRequest());
    }

    // ---------- 배치 · 교체 ----------

    @Test
    void upsertCreatesThenReplaces() throws Exception {
        place(token, "2026-09-28", outfitA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plan_date").value("2026-09-28"))
                .andExpect(jsonPath("$.outfit.id").value(outfitA))
                .andExpect(jsonPath("$.updated_at").isEmpty());

        place(token, "2026-09-28", outfitB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outfit.id").value(outfitB))
                .andExpect(jsonPath("$.updated_at").isNotEmpty());

        api.getAs(token, "/planner/schedules?start_date=2026-09-28&end_date=2026-09-28")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].outfit.id").value(outfitB));
        // 교체되어 빠진 코디는 삭제되지 않는다.
        api.getAs(token, "/outfits/{id}", outfitA).andExpect(status().isOk());
    }

    @Test
    void upsertOthersOutfitIsNotFound() throws Exception {
        long others = api.createOutfit(otherToken, "남의 코디", api.createClothing(otherToken, "옷", "TOP"));

        place(token, "2026-09-28", others)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("삭제되었거나 존재하지 않는 코디입니다."));
    }

    @Test
    void upsertWithoutOutfitIdIsBadRequest() throws Exception {
        api.putAs(token, "/planner/schedules/2026-09-28", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
    }

    @Test
    void upsertInvalidDateIsBadRequest() throws Exception {
        place(token, "2026-13-40", outfitA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_REQUEST"));
    }

    // ---------- 해제 ----------

    @Test
    void removeUnschedules() throws Exception {
        place(token, "2026-09-28", outfitA).andExpect(status().isCreated());

        api.deleteAs(token, "/planner/schedules/2026-09-28").andExpect(status().isNoContent());
        api.deleteAs(token, "/planner/schedules/2026-09-28")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("배치된 코디가 없는 날짜예요."));
        api.getAs(token, "/planner/schedules?start_date=2026-09-28&end_date=2026-09-28")
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotRemoveOthersSchedule() throws Exception {
        long others = api.createOutfit(otherToken, "남의 코디", api.createClothing(otherToken, "옷", "TOP"));
        place(otherToken, "2026-09-28", others).andExpect(status().isCreated());

        api.deleteAs(token, "/planner/schedules/2026-09-28").andExpect(status().isNotFound());
        api.getAs(otherToken, "/planner/schedules?start_date=2026-09-28&end_date=2026-09-28")
                .andExpect(jsonPath("$.length()").value(1));
    }
}
