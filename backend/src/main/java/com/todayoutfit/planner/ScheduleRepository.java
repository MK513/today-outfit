package com.todayoutfit.planner;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findAllByUserIdAndPlanDateBetweenOrderByPlanDateAsc(Long userId, LocalDate start, LocalDate end);

    Optional<Schedule> findByUserIdAndPlanDate(Long userId, LocalDate planDate);
}
