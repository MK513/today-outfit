package com.todayoutfit.planner;

import com.todayoutfit.auth.LoginUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/planner")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;

    @GetMapping("/schedules")
    public List<ScheduleResponse> list(@LoginUser Long userId,
            @RequestParam("start_date") @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
            @RequestParam("end_date") @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        return plannerService.list(userId, startDate, endDate);
    }

    /** 새로 배치하면 201, 기존 배치를 교체하면 200 (명세) */
    @PutMapping("/schedules/{planDate}")
    public ResponseEntity<ScheduleResponse> upsert(@LoginUser Long userId,
            @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate planDate,
            @Valid @RequestBody PlannerRequests.Upsert request) {
        PlannerService.UpsertResult result = plannerService.upsert(userId, planDate, request.outfitId());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.schedule());
    }

    @DeleteMapping("/schedules/{planDate}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@LoginUser Long userId, @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate planDate) {
        plannerService.remove(userId, planDate);
    }
}
