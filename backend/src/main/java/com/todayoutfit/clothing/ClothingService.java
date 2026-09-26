package com.todayoutfit.clothing;

import com.todayoutfit.common.ApiException;
import com.todayoutfit.common.ErrorCode;
import com.todayoutfit.common.PageResponse;
import com.todayoutfit.image.ImageService;
import com.todayoutfit.user.User;
import com.todayoutfit.user.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClothingService {

    private static final String NOT_FOUND_MESSAGE = "삭제되었거나 존재하지 않는 옷입니다.";
    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ClothingRepository clothingRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    /** 최신 등록순. 클라이언트가 보낸 정렬 조건은 무시한다. */
    @Transactional(readOnly = true)
    public PageResponse<ClothingResponse> list(Long userId, ClothingCategory category, SeasonType season,
            ClothingColor color, Pageable pageable) {
        // 값이 없는 필터(null)는 제외하고 조합한다.
        Specification<Clothing> spec = Specification.allOf(Stream.of(
                        ClothingSpecs.ownedBy(userId),
                        ClothingSpecs.categoryIs(category),
                        ClothingSpecs.seasonIs(season),
                        ClothingSpecs.colorIs(color))
                .filter(Objects::nonNull)
                .toList());
        Pageable latest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), LATEST_FIRST);
        return PageResponse.from(clothingRepository.findAll(spec, latest), this::toResponse);
    }

    @Transactional(readOnly = true)
    public ClothingResponse get(Long userId, Long clothingId) {
        return toResponse(findOwned(userId, clothingId));
    }

    @Transactional
    public ClothingResponse create(Long userId, ClothingRequests.Create request) {
        User user = userRepository.getReferenceById(userId);
        return toResponse(clothingRepository.save(toEntity(user, userId, request)));
    }

    /** 한 건이라도 검증에 실패하면 전체를 저장하지 않는다. */
    @Transactional
    public List<ClothingResponse> createBulk(Long userId, ClothingRequests.Bulk request) {
        User user = userRepository.getReferenceById(userId);
        List<Clothing> entities = request.items().stream().map(item -> toEntity(user, userId, item)).toList();
        return clothingRepository.saveAll(entities).stream().map(this::toResponse).toList();
    }

    /** 이 의류를 포함한 코디에서는 해당 의류만 빠진다(outfit_items ON DELETE CASCADE). 사진 파일도 지운다. */
    @Transactional
    public void delete(Long userId, Long clothingId) {
        Clothing clothing = findOwned(userId, clothingId);
        clothingRepository.delete(clothing);
        imageService.deleteIfUnusedAfterCommit(clothing.getImageKey());
    }

    private Clothing findOwned(Long userId, Long clothingId) {
        return clothingRepository.findByIdAndUserId(clothingId, userId)
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND_MESSAGE));
    }

    private Clothing toEntity(User user, Long userId, ClothingRequests.Create request) {
        // 사진은 이 사용자가 POST /images(또는 사진 분석)로 올린 파일만 연결할 수 있다. DB에는 URL 대신 저장소 키를 둔다.
        String imageKey = null;
        if (request.imageUrl() != null) {
            imageKey = imageService.findOwnedKey(userId, request.imageUrl())
                    .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                            "업로드한 사진만 등록할 수 있어요. 사진을 다시 올려주세요."));
        }
        return new Clothing(user, request.name(), request.category(), request.color(), request.season(),
                imageKey, imageKey == null ? null : request.imageFileName(), request.source());
    }

    private ClothingResponse toResponse(Clothing clothing) {
        return ClothingResponse.from(clothing, imageService::publicUrl);
    }
}
