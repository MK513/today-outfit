package com.todayoutfit.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 명세의 error_code 목록. 프론트엔드는 이 값으로 화면을 분기한다.
 */
@Getter
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값을 다시 확인해주세요."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "5MB 이하의 JPG 또는 PNG 파일만 업로드할 수 있어요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "삭제되었거나 존재하지 않는 항목입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    NO_CLOTHES(HttpStatus.UNPROCESSABLE_CONTENT, "보유 의류가 부족해 추천을 만들 수 없어요."),
    AI_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "오늘 사용할 수 있는 AI 요청을 모두 사용했어요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요."),
    AI_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "AI 분석에 실패했어요. 수동 등록으로 이동해주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
