package com.todayoutfit.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody AuthRequests.Signup request) {
        return authService.signup(request);
    }

    @PostMapping("/auth/login")
    public AuthTokenResponse login(@Valid @RequestBody AuthRequests.Login request) {
        return authService.login(request);
    }

    @PostMapping("/auth/demo")
    public AuthTokenResponse loginDemo() {
        return authService.loginDemo();
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt);
    }

    @GetMapping("/users/me")
    public UserResponse me(@LoginUser Long userId) {
        return authService.me(userId);
    }

    @PostMapping("/users/me/withdrawal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@LoginUser Long userId, @Valid @RequestBody AuthRequests.Withdrawal request,
            @AuthenticationPrincipal Jwt jwt) {
        authService.withdraw(userId, request, jwt);
    }
}
