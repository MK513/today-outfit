package com.todayoutfit.auth;

import com.todayoutfit.clothing.Clothing;
import com.todayoutfit.clothing.ClothingCategory;
import com.todayoutfit.clothing.ClothingColor;
import com.todayoutfit.clothing.ClothingRepository;
import com.todayoutfit.clothing.ClothingSource;
import com.todayoutfit.clothing.SeasonType;
import com.todayoutfit.outfit.Outfit;
import com.todayoutfit.outfit.OutfitRepository;
import com.todayoutfit.outfit.OutfitSource;
import com.todayoutfit.planner.Schedule;
import com.todayoutfit.planner.ScheduleRepository;
import com.todayoutfit.user.User;
import com.todayoutfit.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데모 계정과 샘플 데이터(의류 14벌, 코디 2개, 내일 날짜 배치 1건)를 만든다.
 * 프론트엔드의 src/lib/seedDemo.js와 같은 데이터다.
 */
@Component
@RequiredArgsConstructor
public class DemoAccountSeeder {

    public static final String DEMO_EMAIL = "demo@today-outfit.app";
    static final String DEMO_PASSWORD = "demo1234";

    private record SampleClothing(String name, ClothingCategory category, String color, SeasonType season) {
    }

    // 실사 대표 사진이 매칭되는 카테고리+색상 조합만 사용한다.
    private static final List<SampleClothing> SAMPLE_CLOTHES = List.of(
            new SampleClothing("화이트 코튼 셔츠", ClothingCategory.TOP, "화이트", SeasonType.ALL),
            new SampleClothing("블랙 맨투맨", ClothingCategory.TOP, "블랙", SeasonType.ALL),
            new SampleClothing("그레이 스웨트셔츠", ClothingCategory.TOP, "그레이", SeasonType.FALL),
            new SampleClothing("블랙 슬랙스", ClothingCategory.BOTTOM, "블랙", SeasonType.ALL),
            new SampleClothing("블루 와이드 데님", ClothingCategory.BOTTOM, "블루", SeasonType.ALL),
            new SampleClothing("브라운 치노 팬츠", ClothingCategory.BOTTOM, "브라운", SeasonType.SPRING),
            new SampleClothing("화이트 스니커즈", ClothingCategory.SHOES, "화이트", SeasonType.ALL),
            new SampleClothing("블랙 첼시부츠", ClothingCategory.SHOES, "블랙", SeasonType.WINTER),
            new SampleClothing("블랙 버킷햇", ClothingCategory.HAT, "블랙", SeasonType.ALL),
            new SampleClothing("브라운 볼캡", ClothingCategory.HAT, "브라운", SeasonType.SUMMER),
            new SampleClothing("그레이 머플러", ClothingCategory.ACC, "그레이", SeasonType.WINTER),
            new SampleClothing("블랙 가죽 벨트", ClothingCategory.ACC, "블랙", SeasonType.ALL),
            new SampleClothing("베이지 손수건", ClothingCategory.ACC, "베이지", SeasonType.ALL),
            new SampleClothing("골드 목걸이", ClothingCategory.ACC, "골드", SeasonType.ALL));

    private final UserRepository userRepository;
    private final ClothingRepository clothingRepository;
    private final OutfitRepository outfitRepository;
    private final ScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createDemoAccount() {
        User user = userRepository.saveAndFlush(
                new User(DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD), "데모 사용자", true));

        List<Clothing> clothes = clothingRepository.saveAll(SAMPLE_CLOTHES.stream()
                .map(c -> new Clothing(user, c.name(), c.category(), ClothingColor.fromLabel(c.color()), c.season(),
                        null, null, ClothingSource.MANUAL))
                .toList());

        Outfit office = new Outfit(user, "오피스 캐주얼", "무난한 출근룩", OutfitSource.MANUAL);
        office.addItem(nth(clothes, ClothingCategory.TOP, 0));
        office.addItem(nth(clothes, ClothingCategory.BOTTOM, 0));
        office.addItem(nth(clothes, ClothingCategory.SHOES, 0));
        office.addItem(nth(clothes, ClothingCategory.ACC, 0));
        outfitRepository.save(office);

        Outfit weekend = new Outfit(user, "주말 나들이룩", "편안한 주말 코디", OutfitSource.AI);
        weekend.addItem(nth(clothes, ClothingCategory.TOP, 1));
        weekend.addItem(nth(clothes, ClothingCategory.BOTTOM, 1));
        weekend.addItem(nth(clothes, ClothingCategory.SHOES, 1));
        weekend.recordAiRecommendation("가까운 공원 산책",
                "\"가까운 공원 산책\" 상황에 어울리도록 편안한 소재와 색상을 골랐습니다.");
        outfitRepository.save(weekend);

        scheduleRepository.save(new Schedule(user, LocalDate.now().plusDays(1), office));
        return user;
    }

    private static Clothing nth(List<Clothing> clothes, ClothingCategory category, int index) {
        return clothes.stream().filter(c -> c.getCategory() == category).skip(index).findFirst().orElseThrow();
    }
}
