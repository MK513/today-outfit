package com.todayoutfit;

import static org.assertj.core.api.Assertions.assertThat;

import com.todayoutfit.ai.AiRequest;
import com.todayoutfit.ai.AiRequestStatus;
import com.todayoutfit.ai.AiRequestType;
import com.todayoutfit.clothing.Clothing;
import com.todayoutfit.clothing.ClothingCategory;
import com.todayoutfit.clothing.ClothingColor;
import com.todayoutfit.clothing.ClothingSource;
import com.todayoutfit.clothing.SeasonType;
import com.todayoutfit.outfit.Outfit;
import com.todayoutfit.outfit.OutfitAiTag;
import com.todayoutfit.outfit.OutfitItem;
import com.todayoutfit.outfit.OutfitSource;
import com.todayoutfit.planner.Schedule;
import com.todayoutfit.user.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Flyway 스키마 ↔ JPA 매핑과 DB 레벨 CASCADE 규칙을 실제 PostgreSQL로 검증한다. 각 테스트는 롤백된다. */
@IntegrationTest
@Transactional
class SchemaMappingTest {

    @Autowired
    EntityManager em;

    private User user;
    private Clothing top;
    private Clothing bottom;
    private Outfit outfit;

    private void seed() {
        user = new User("test-" + System.nanoTime() + "@today-outfit.app", "{bcrypt}hash", "테스터", false);
        em.persist(user);
        top = new Clothing(user, "블랙 맨투맨", ClothingCategory.TOP, ClothingColor.BLACK, null, null, null, ClothingSource.MANUAL);
        bottom = new Clothing(user, "블루 데님", ClothingCategory.BOTTOM, ClothingColor.BLUE, SeasonType.FALL, null, null, ClothingSource.TEXT);
        em.persist(top);
        em.persist(bottom);

        outfit = new Outfit(user, "주말 코디", null, OutfitSource.MANUAL);
        outfit.getItems().add(new OutfitItem(outfit, top, 1));
        outfit.getItems().add(new OutfitItem(outfit, bottom, 2));
        outfit.getTags().add(new OutfitAiTag(outfit, "캐주얼"));
        em.persist(outfit);
        em.persist(new Schedule(user, LocalDate.of(2026, 9, 26), outfit));
        em.persist(new AiRequest(user, AiRequestType.OUTFIT_RECOMMEND, AiRequestStatus.SUCCESS, 1200));
        em.flush();
        em.clear();
    }

    private long count(String sql, Long id) {
        return ((Number) em.createNativeQuery(sql).setParameter(1, id).getSingleResult()).longValue();
    }

    @Test
    void persistsAllEntitiesAndStoresColorAsKoreanLabel() {
        seed();

        Clothing loaded = em.find(Clothing.class, top.getId());
        assertThat(loaded.getColor()).isEqualTo(ClothingColor.BLACK);
        assertThat(loaded.getSeason()).isEqualTo(SeasonType.ALL);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNull();

        Object rawColor = em.createNativeQuery("select color from clothes where id = ?1")
                .setParameter(1, top.getId()).getSingleResult();
        assertThat(rawColor).isEqualTo("블랙");

        Outfit loadedOutfit = em.find(Outfit.class, outfit.getId());
        assertThat(loadedOutfit.getItems()).extracting(OutfitItem::getItemOrder).containsExactly(1, 2);
        assertThat(loadedOutfit.getTags()).extracting(OutfitAiTag::getTag).containsExactly("캐주얼");
    }

    @Test
    void deletingClothingKeepsOutfitButRemovesItsItem() {
        seed();

        em.createNativeQuery("delete from clothes where id = ?1").setParameter(1, top.getId()).executeUpdate();

        assertThat(count("select count(*) from outfits where id = ?1", outfit.getId())).isEqualTo(1);
        assertThat(count("select count(*) from outfit_items where outfit_id = ?1", outfit.getId())).isEqualTo(1);
    }

    @Test
    void deletingOutfitRemovesItsSchedules() {
        seed();

        em.createNativeQuery("delete from outfits where id = ?1").setParameter(1, outfit.getId()).executeUpdate();

        assertThat(count("select count(*) from schedules where outfit_id = ?1", outfit.getId())).isZero();
        assertThat(count("select count(*) from outfit_ai_tags where outfit_id = ?1", outfit.getId())).isZero();
    }

    @Test
    void deletingUserRemovesAllOwnedData() {
        seed();

        em.createNativeQuery("delete from users where id = ?1").setParameter(1, user.getId()).executeUpdate();

        for (String table : new String[] {"clothes", "outfits", "schedules", "ai_requests"}) {
            assertThat(count("select count(*) from " + table + " where user_id = ?1", user.getId()))
                    .as(table).isZero();
        }
    }
}
