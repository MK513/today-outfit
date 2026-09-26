package com.todayoutfit.auth;

import com.todayoutfit.user.User;
import java.time.LocalDateTime;

/** 명세의 User 스키마 */
public record UserResponse(Long id, String email, String name, boolean isDemo, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.isDemo(), user.getCreatedAt());
    }
}
