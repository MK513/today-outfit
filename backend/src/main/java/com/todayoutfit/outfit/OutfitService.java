package com.todayoutfit.outfit;

import com.todayoutfit.clothing.Clothing;
import com.todayoutfit.clothing.ClothingRepository;
import com.todayoutfit.common.ApiException;
import com.todayoutfit.common.ErrorCode;
import com.todayoutfit.image.ImageService;
import com.todayoutfit.user.User;
import com.todayoutfit.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutfitService {

    private static final String NOT_FOUND_MESSAGE = "삭제되었거나 존재하지 않는 코디입니다.";

    private final OutfitRepository outfitRepository;
    private final ClothingRepository clothingRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    /**
     * 생성 경로와 맞지 않는 필드는 저장하지 않는다:
     * request_text · ai_reason은 AI일 때만, ai_score · ai_comment · ai_tags는 CHALLENGE일 때만.
     */
    @Transactional
    public OutfitDetailResponse create(Long userId, OutfitRequests.Create request) {
        User user = userRepository.getReferenceById(userId);
        Outfit outfit = newOutfit(user, userId, request.name(), request.memo(), request.source(), request.clothingIds());
        if (request.source() == OutfitSource.AI) {
            outfit.recordAiRecommendation(request.requestText(), request.aiReason());
        }
        if (request.source() == OutfitSource.CHALLENGE) {
            List<String> tags = request.aiTags() == null ? List.of()
                    : List.copyOf(new LinkedHashSet<>(request.aiTags().stream().map(String::trim).toList()));
            outfit.recordChallengeEvaluation(request.aiScore(), request.aiComment(), tags);
        }
        return toDetail(outfitRepository.save(outfit));
    }

    @Transactional(readOnly = true)
    public OutfitDetailResponse get(Long userId, Long outfitId) {
        return toDetail(findOwned(userId, outfitId));
    }

    /**
     * 저장 전 코디 엔티티: 옷이 모두 이 사용자 것인지 확인하고, clothingIds 순서대로 구성 항목을 담는다.
     * 하나라도 남의 옷이거나 없으면 400 INVALID_CLOTHING.
     */
    public Outfit newOutfit(User user, Long userId, String name, String memo, OutfitSource source,
            List<Long> clothingIds) {
        Map<Long, Clothing> owned = clothingRepository.findAllByUserIdAndIdIn(userId, clothingIds).stream()
                .collect(Collectors.toMap(Clothing::getId, Function.identity()));
        if (owned.size() != clothingIds.size()) {
            throw new ApiException(ErrorCode.INVALID_CLOTHING);
        }
        Outfit outfit = new Outfit(user, name, memo, source);
        clothingIds.forEach(id -> outfit.addItem(owned.get(id)));
        return outfit;
    }

    /** 이 사용자의 코디. 없거나 남의 것이면 404. */
    public Outfit findOwned(Long userId, Long outfitId) {
        return outfitRepository.findByIdAndUserId(outfitId, userId)
                .orElseThrow(() -> ApiException.notFound(NOT_FOUND_MESSAGE));
    }

    private OutfitDetailResponse toDetail(Outfit outfit) {
        return OutfitDetailResponse.from(outfit, imageService::publicUrl);
    }
}
