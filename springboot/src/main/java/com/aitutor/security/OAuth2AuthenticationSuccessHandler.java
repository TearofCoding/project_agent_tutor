package com.aitutor.security;

import com.aitutor.domain.RefreshToken;
import com.aitutor.domain.User;
import com.aitutor.repository.RefreshTokenRepository;
import com.aitutor.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String dbUserIdStr = oAuth2User.getAttribute("dbUserId");

        if (dbUserIdStr == null) {
            log.error("OAuth2 success but dbUserId attribute is missing");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        User user = userRepository.findById(Long.parseLong(dbUserIdStr))
                .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 login"));

        refreshTokenRepository.findAllByUserAndRevokedFalse(user)
                .forEach(t -> t.setRevoked(true));

        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshExpiryMs() / 1000))
                .build());

        String redirectUrl = UriComponentsBuilder.fromUriString("/oauth2/success")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
