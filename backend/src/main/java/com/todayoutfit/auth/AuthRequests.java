package com.todayoutfit.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 인증 · 계정 API 요청 본문. JSON 필드명은 snake_case(password_confirm 등)로 받는다. */
public final class AuthRequests {

    private AuthRequests() {
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    // BCrypt는 72바이트까지만 사용하므로 그 이상은 거부한다.
    public record Signup(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
            String email,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하로 입력해주세요.")
            String password,

            @NotBlank(message = "비밀번호 확인을 입력해주세요.")
            String passwordConfirm,

            @NotBlank(message = "이름을 입력해주세요.")
            @Size(max = 30, message = "이름은 30자 이하로 입력해주세요.")
            String name) {

        // 형식 검증 전에 앞뒤 공백을 제거한다 (복사·붙여넣기 입력 대비).
        public Signup {
            email = trim(email);
            name = trim(name);
        }
    }

    public record Login(
            @NotBlank(message = "이메일을 입력해주세요.") String email,
            @NotBlank(message = "비밀번호를 입력해주세요.") String password) {

        public Login {
            email = trim(email);
        }
    }

    public record Withdrawal(
            @NotBlank(message = "현재 비밀번호를 입력해주세요.") String password) {
    }
}
