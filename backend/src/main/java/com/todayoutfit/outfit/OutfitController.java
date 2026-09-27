package com.todayoutfit.outfit;

import com.todayoutfit.auth.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/{outfitId}")
    public OutfitDetailResponse get(@LoginUser Long userId, @PathVariable Long outfitId) {
        return outfitService.get(userId, outfitId);
    }
}
