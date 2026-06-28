package com.aitutor.service;

import com.aitutor.domain.Level;
import com.aitutor.domain.RefreshToken;
import com.aitutor.domain.User;
import com.aitutor.domain.UserProfile;
import com.aitutor.dto.AuthResponse;
import com.aitutor.dto.LoginRequest;
import com.aitutor.dto.RefreshRequest;
import com.aitutor.dto.RegisterRequest;
import com.aitutor.exception.UserException;
import com.aitutor.repository.RefreshTokenRepository;
import com.aitutor.repository.UserProfileRepository;
import com.aitutor.repository.UserRepository;
import com.aitutor.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw UserException.conflict("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();
        userRepository.save(user);

        userProfileRepository.save(UserProfile.builder()
                .user(user)
                .declaredLevel(Level.BEGINNER)
                .build());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> UserException.unauthorized("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw UserException.unauthorized("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> UserException.unauthorized("유효하지 않은 refresh token입니다."));

        if (storedToken.getRevoked()) {
            throw UserException.unauthorized("이미 폐기된 refresh token입니다.");
        }
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw UserException.unauthorized("만료된 refresh token입니다.");
        }

        storedToken.setRevoked(true);
        return issueTokens(storedToken.getUser());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .filter(t -> !t.getRevoked())
                .ifPresent(t -> t.setRevoked(true));
    }

    private AuthResponse issueTokens(User user) {
        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshExpiryMs() / 1000))
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessExpiryMs() / 1000)
                .build();
    }
}
