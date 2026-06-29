package com.aitutor.service;

import com.aitutor.domain.RefreshToken;
import com.aitutor.domain.User;
import com.aitutor.dto.AuthResponse;
import com.aitutor.dto.LoginRequest;
import com.aitutor.dto.RefreshRequest;
import com.aitutor.dto.RegisterRequest;
import com.aitutor.exception.UserException;
import com.aitutor.repository.RefreshTokenRepository;
import com.aitutor.repository.UserProfileRepository;
import com.aitutor.repository.UserRepository;
import com.aitutor.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessExpiryMs()).thenReturn(1800000L);
        when(jwtTokenProvider.getRefreshExpiryMs()).thenReturn(604800000L);
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(any())).thenReturn(List.of());
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_happyPath_returnsTokens() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = registerRequest("new@test.com", "Test1234!", "Tester");
        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        RegisterRequest req = registerRequest("existing@test.com", "Test1234!", "Tester");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("이미 사용 중인 이메일");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = User.builder().email("user@test.com").passwordHash("hashed").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest req = loginRequest("user@test.com", "wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("비밀번호");
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequest req = loginRequest("nobody@test.com", "Test1234!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UserException.class);
    }

    @Test
    void refresh_revokedToken_throwsUnauthorized() {
        RefreshToken token = RefreshToken.builder()
                .token("old-token").revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .user(User.builder().build())
                .build();
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(token));

        RefreshRequest req = refreshRequest("old-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("폐기");
    }

    @Test
    void refresh_expiredToken_throwsUnauthorized() {
        RefreshToken token = RefreshToken.builder()
                .token("expired-token").revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .user(User.builder().build())
                .build();
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        RefreshRequest req = refreshRequest("expired-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(UserException.class)
                .hasMessageContaining("만료");
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static RegisterRequest registerRequest(String email, String password, String name) {
        RegisterRequest r = new RegisterRequest();
        ReflectionTestUtils.setField(r, "email", email);
        ReflectionTestUtils.setField(r, "password", password);
        ReflectionTestUtils.setField(r, "name", name);
        return r;
    }

    private static LoginRequest loginRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        ReflectionTestUtils.setField(r, "email", email);
        ReflectionTestUtils.setField(r, "password", password);
        return r;
    }

    private static RefreshRequest refreshRequest(String token) {
        RefreshRequest r = new RefreshRequest();
        ReflectionTestUtils.setField(r, "refreshToken", token);
        return r;
    }
}
