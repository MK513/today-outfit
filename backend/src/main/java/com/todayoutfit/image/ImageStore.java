package com.todayoutfit.image;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * 사진 파일을 실제로 보관하는 곳. 검증 · 소유권 · 정리 규칙은 {@link ImageService}가 맡고,
 * 구현체는 저장 · 조회 · 삭제 · URL 변환만 한다.
 *
 * <p>구현체는 {@code app.image.storage} 값으로 고른다 (현재 {@code local}).
 * S3 등을 추가할 때는 이 인터페이스를 구현하고 {@code ImageStoreContractTest}를 상속해 같은 테스트를 통과시킨다.
 */
public interface ImageStore {

    void put(String key, InputStream content, long size, MediaType type);

    Optional<Resource> get(String key);

    boolean exists(String key);

    void delete(String key);

    /** prefix로 시작하는 모든 파일 삭제 (예: 탈퇴한 사용자의 "{userId}/") */
    void deleteAll(String prefix);

    /** 저장된 모든 파일의 키와 마지막 수정 시각 (미사용 사진 정리용) */
    List<StoredImage> list();

    /** 클라이언트가 사진을 불러올 URL */
    String publicUrl(String key);

    /** {@link #publicUrl}의 역변환. 이 저장소가 만든 URL이 아니면 empty */
    Optional<String> keyOf(String publicUrl);

    record StoredImage(String key, Instant lastModified) {
    }
}
