package com.todayoutfit.common;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 존재하지 않거나 본인 소유가 아닌 리소스. 타인 리소스의 존재 여부를 드러내지 않도록 둘 다 404로 응답한다. */
    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.NOT_FOUND, message);
    }
}
