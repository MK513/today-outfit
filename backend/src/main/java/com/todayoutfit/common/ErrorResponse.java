package com.todayoutfit.common;

/** 공통 에러 응답. JSON 필드명은 전역 snake_case 설정에 따라 code / error_code / message가 된다. */
public record ErrorResponse(int code, String errorCode, String message) {

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.name(), message);
    }
}
