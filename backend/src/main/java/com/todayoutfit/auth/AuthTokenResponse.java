package com.todayoutfit.auth;

/** 명세의 AuthToken 스키마 */
public record AuthTokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
