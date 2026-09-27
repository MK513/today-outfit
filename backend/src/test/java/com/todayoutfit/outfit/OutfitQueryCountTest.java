package com.todayoutfit.outfit;

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

/** 코디 목록 조회의 SQL 실행 수가 코디 수에 비례해 늘지 않는지(N+1 없음) 확인한다. */
@IntegrationTest
@AutoConfigureMockMvc
class OutfitQueryCountTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private TestApi api;

    private String userWithOutfits(int count) throws Exception {
        String token = api.signupAndLogin();
        long top = api.createClothing(token, "상의", "TOP");
        long bottom = api.createClothing(token, "하의", "BOTTOM");
        long shoes = api.createClothing(token, "신발", "SHOES");
        for (int i = 0; i < count; i++) {
            api.createOutfit(token, "코디 " + i, top, bottom, shoes);
        }
        return token;
    }

    private long statementsFor(String token, String path) throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        api.getAs(token, path).andExpect(status().isOk());
        return stats.getPrepareStatementCount();
    }

    @Test
    void listQueryCountDoesNotGrowWithOutfits() throws Exception {
        api = new TestApi(mockMvc);
        String few = userWithOutfits(5);
        String many = userWithOutfits(20);

        // 두 경우 모두 한 페이지에 다 들어오게 해, 페이지가 꽉 찼을 때만 나가는 count 쿼리 차이를 없앤다.
        long fewCount = statementsFor(few, "/outfits?size=100");
        long manyCount = statementsFor(many, "/outfits?size=100");

        assertThat(fewCount).isPositive();
        assertThat(manyCount).isEqualTo(fewCount);
    }
}
