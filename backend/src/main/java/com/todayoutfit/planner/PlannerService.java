package com.todayoutfit.planner;

import com.todayoutfit.common.ApiException;
import com.todayoutfit.common.ErrorCode;
import com.todayoutfit.outfit.Outfit;
import com.todayoutfit.outfit.OutfitService;
import com.todayoutfit.user.User;
import com.todayoutfit.user.UserRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PlannerService {

    static final int MAX_RANGE_DAYS = 31;
    private static final String RANGE_MESSAGE = "조회 기간은 최대 31일이에요.";
    private static final String NO_SCHEDULE_MESSAGE = "배치된 코디가 없는 날짜예요.";

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final OutfitService outfitService;
    private final TransactionTemplate transactionTemplate;

    public record UpsertResult(boolean created, ScheduleResponse schedule) {
    }

    /** 두 날짜를 포함한 기간(최대 31일)의 일정, 날짜 오름차순. */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(Long userId, LocalDate start, LocalDate end) {
        if (start.isAfter(end) || ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, RANGE_MESSAGE);
        }
        return scheduleRepository.findAllByUserIdAndPlanDateBetweenOrderByPlanDateAsc(userId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * (user_id, plan_date) 기준 업서트. 같은 날짜에 동시 요청이 들어와 유니크 제약에 걸리면
     * 먼저 들어간 배치를 교체하도록 새 트랜잭션에서 한 번 다시 시도한다.
     */
    public UpsertResult upsert(Long userId, LocalDate date, Long outfitId) {
        try {
            return transactionTemplate.execute(status -> upsertInTransaction(userId, date, outfitId));
        } catch (DataIntegrityViolationException e) {
            return transactionTemplate.execute(status -> upsertInTransaction(userId, date, outfitId));
        }
    }

    @Transactional
    public void remove(Long userId, LocalDate date) {
        Schedule schedule = scheduleRepository.findByUserIdAndPlanDate(userId, date)
                .orElseThrow(() -> ApiException.notFound(NO_SCHEDULE_MESSAGE));
        scheduleRepository.delete(schedule);
    }

    /** 이 날짜에 코디를 배치한다. 새로 배치했으면 true, 기존 배치를 교체했으면 false. 트랜잭션 안에서 호출한다. */
    public boolean place(User user, LocalDate date, Outfit outfit) {
        Optional<Schedule> existing = scheduleRepository.findByUserIdAndPlanDate(user.getId(), date);
        if (existing.isPresent()) {
            existing.get().changeOutfit(outfit);
            return false;
        }
        scheduleRepository.save(new Schedule(user, date, outfit));
        return true;
    }

    ScheduleResponse toResponse(Schedule schedule) {
        return ScheduleResponse.from(schedule, outfitService::toSummary);
    }

    private UpsertResult upsertInTransaction(Long userId, LocalDate date, Long outfitId) {
        Outfit outfit = outfitService.findOwned(userId, outfitId);
        User user = userRepository.getReferenceById(userId);
        boolean created = place(user, date, outfit);
        scheduleRepository.flush(); // created_at · updated_at이 채워진 상태로 응답하려고 먼저 반영한다.
        Schedule schedule = scheduleRepository.findByUserIdAndPlanDate(userId, date).orElseThrow();
        return new UpsertResult(created, toResponse(schedule));
    }
}
