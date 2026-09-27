package com.todayoutfit.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayoutfit.IntegrationTest;
import com.todayoutfit.TestApi;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** 주간 일정 조회의 SQL 실행 수가 배치된 날짜 수에 비례해 늘지 않는지(N+1 없음) 확인한다. */
@IntegrationTest
@AutoConfigureMockMvc
class ScheduleQueryCountTest {

    private static final String WEEK = "/planner/schedules?start_date=2026-10-05&end_date=2026-10-11";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private TestApi api;

    private String userWithScheduledDays(int days) throws Exception {
        String token = api.signupAndLogin();
        long top = api.createClothing(token, "상의", "TOP");
        long bottom = api.createClothing(token, "하의", "BOTTOM");
        long shoes = api.createClothing(token, "신발", "SHOES");
        for (int d = 0; d < days; d++) {
            long outfit = api.createOutfit(token, "코디 " + d, top, bottom, shoes);
            api.putAs(token, "/planner/schedules/2026-10-%02d".formatted(5 + d), "{\"outfit_id\":" + outfit + "}")
                    .andExpect(status().isCreated());
        }
        return token;
    }

    private long statementsFor(String token) throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        api.getAs(token, WEEK).andExpect(status().isOk());
        return stats.getPrepareStatementCount();
    }

    @Test
    void weekQueryCountDoesNotGrowWithSchedules() throws Exception {
        api = new TestApi(mockMvc);
        String twoDays = userWithScheduledDays(2);
        String fullWeek = userWithScheduledDays(7);

        long few = statementsFor(twoDays);
        long many = statementsFor(fullWeek);

        assertThat(few).isPositive();
        assertThat(many).isEqualTo(few);
    }
}
