package com.todayoutfit.auth;

import com.todayoutfit.common.ApiException;
import com.todayoutfit.common.ErrorCode;
import com.todayoutfit.image.ImageService;
import com.todayoutfit.user.User;
import com.todayoutfit.user.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String LOGIN_FAILED = "이메일 또는 비밀번호가 올바르지 않습니다.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DemoAccountSeeder demoAccountSeeder;
    private final ImageService imageService;

    /** 로그인 실패 시에도 BCrypt 비교를 한 번 수행해, 응답 시간으로 가입 여부가 드러나지 않게 한다. */
    private String dummyHash;

    @Transactional
    public UserResponse signup(AuthRequests.Signup request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new ApiException(ErrorCode.PASSWORD_MISMATCH);
        }
        String email = normalize(request.email());
        // 데모 계정 이메일은 예약되어 있어 일반 가입을 막는다.
        if (DemoAccountSeeder.DEMO_EMAIL.equals(email) || userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_DUPLICATED);
        }
        try {
            User user = userRepository.saveAndFlush(
                    new User(email, passwordEncoder.encode(request.password()), request.name(), false));
            return UserResponse.from(user);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 이메일로 가입한 경우 (users.email UNIQUE)
            throw new ApiException(ErrorCode.EMAIL_DUPLICATED);
        }
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse login(AuthRequests.Login request) {
        User user = userRepository.findByEmail(normalize(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash());
            throw new ApiException(ErrorCode.UNAUTHORIZED, LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, LOGIN_FAILED);
        }
        return tokenService.issue(user);
    }

    public AuthTokenResponse loginDemo() {
        User demo;
        try {
            demo = userRepository.findByEmail(DemoAccountSeeder.DEMO_EMAIL)
                    .orElseGet(demoAccountSeeder::createDemoAccount);
        } catch (DataIntegrityViolationException e) {
            // 최초 체험 요청이 동시에 들어와 다른 요청이 먼저 생성한 경우
            demo = userRepository.findByEmail(DemoAccountSeeder.DEMO_EMAIL)
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR));
        }
        return tokenService.issue(demo);
    }

    @Transactional
    public void logout(Jwt jwt) {
        tokenService.revoke(jwt);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    /** 사용자 삭제 시 의류 · 코디 · 배치 · AI 이력은 DB의 ON DELETE CASCADE로 함께 삭제되고, 업로드 사진은 커밋 후 지운다. */
    @Transactional
    public void withdraw(Long userId, AuthRequests.Withdrawal request, Jwt jwt) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD);
        }
        userRepository.delete(user);
        tokenService.revoke(jwt);
        imageService.deleteAllOfUserAfterCommit(userId);
    }

    /** 토큰은 유효하지만 이미 탈퇴한 사용자는 401로 처리한다. */
    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("dummy-password-for-timing");
        }
        return dummyHash;
    }

    private static String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
