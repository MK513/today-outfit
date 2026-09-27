package com.todayoutfit.outfit;

import com.todayoutfit.auth.LoginUser;
import com.todayoutfit.common.PageResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/outfits")
@RequiredArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OutfitDetailResponse create(@LoginUser Long userId, @Valid @RequestBody OutfitRequests.Create request) {
        return outfitService.create(userId, request);
    }

    @GetMapping
    public PageResponse<OutfitSummaryResponse> list(@LoginUser Long userId,
            @RequestParam(required = false) OutfitSource source, Pageable pageable) {
        return outfitService.list(userId, source, pageable);
    }

    @GetMapping("/{outfitId}")
    public OutfitDetailResponse get(@LoginUser Long userId, @PathVariable Long outfitId) {
        return outfitService.get(userId, outfitId);
    }

    @DeleteMapping("/{outfitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser Long userId, @PathVariable Long outfitId) {
        outfitService.delete(userId, outfitId);
    }
}
