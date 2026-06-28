package com.aitutor.controller;

import com.aitutor.common.ApiResponse;
import com.aitutor.dto.AuthResponse;
import com.aitutor.dto.LoginRequest;
import com.aitutor.dto.RefreshRequest;
import com.aitutor.dto.RegisterRequest;
import com.aitutor.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "로그인이 완료되었습니다.");
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request), "토큰이 갱신되었습니다.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request,
                                    Authentication authentication) {
        authService.logout(request);
        return ApiResponse.ok(null, "로그아웃이 완료되었습니다.");
    }

    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail() {
        // Phase 1 stub: EMAIL_VERIFICATION_ENABLED=false, email_verified defaults to true
        return ApiResponse.ok(null, "이메일 인증이 완료되었습니다.");
    }
}
