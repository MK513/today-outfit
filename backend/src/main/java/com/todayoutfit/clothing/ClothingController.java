package com.todayoutfit.clothing;

import com.todayoutfit.auth.LoginUser;
import com.todayoutfit.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clothes")
@RequiredArgsConstructor
public class ClothingController {

    private final ClothingService clothingService;

    /** color는 명세대로 한글 표시값(예: 블랙)으로 받는다. */
    @GetMapping
    public PageResponse<ClothingResponse> list(@LoginUser Long userId,
            @RequestParam(required = false) ClothingCategory category,
            @RequestParam(required = false) SeasonType season,
            @RequestParam(required = false) ClothingColor color,
            Pageable pageable) {
        return clothingService.list(userId, category, season, color, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClothingResponse create(@LoginUser Long userId, @Valid @RequestBody ClothingRequests.Create request) {
        return clothingService.create(userId, request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ClothingResponse> createBulk(@LoginUser Long userId, @Valid @RequestBody ClothingRequests.Bulk request) {
        return clothingService.createBulk(userId, request);
    }

    @GetMapping("/{clothingId}")
    public ClothingResponse get(@LoginUser Long userId, @PathVariable Long clothingId) {
        return clothingService.get(userId, clothingId);
    }

    @DeleteMapping("/{clothingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser Long userId, @PathVariable Long clothingId) {
        clothingService.delete(userId, clothingId);
    }
}
