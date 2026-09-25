package com.todayoutfit.planner;

import com.todayoutfit.common.BaseTimeEntity;
import com.todayoutfit.outfit.Outfit;
import com.todayoutfit.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 날짜별 코디 배치. (user_id, plan_date)가 유일하며 PUT 업서트의 기준 키다. */
@Getter
@Entity
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    public Schedule(User user, LocalDate planDate, Outfit outfit) {
        this.user = user;
        this.planDate = planDate;
        this.outfit = outfit;
    }
}
