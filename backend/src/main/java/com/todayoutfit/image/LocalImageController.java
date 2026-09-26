package com.todayoutfit.image;

import com.todayoutfit.common.ApiException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 저장소의 사진 파일 제공. S3 등은 저장소가 직접 URL을 제공하므로 로컬일 때만 켜진다.
 * &lt;img&gt; 태그는 인증 헤더를 보낼 수 없어 토큰 없이 허용하며(SecurityConfig),
 * 파일명이 추측할 수 없는 UUID라 URL을 아는 사람만 볼 수 있다. 파일명이 바뀌지 않으므로 오래 캐시한다.
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.image.storage", havingValue = "local", matchIfMissing = true)
public class LocalImageController {

    private final LocalImageStore store;

    @GetMapping("/images/{userId}/{fileName:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String userId, @PathVariable String fileName) {
        ImageKey key = ImageKey.parse(userId + "/" + fileName)
                .orElseThrow(LocalImageController::notFound);
        Resource resource = store.get(key.value()).orElseThrow(LocalImageController::notFound);
        return ResponseEntity.ok()
                .contentType(key.type().mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    private static ApiException notFound() {
        return ApiException.notFound("사진을 찾을 수 없어요.");
    }
}
