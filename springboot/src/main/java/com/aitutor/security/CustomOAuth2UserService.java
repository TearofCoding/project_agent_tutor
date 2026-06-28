package com.aitutor.security;

import com.aitutor.domain.Level;
import com.aitutor.domain.Provider;
import com.aitutor.domain.User;
import com.aitutor.domain.UserProfile;
import com.aitutor.repository.UserProfileRepository;
import com.aitutor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email      = (String) attributes.get("email");
        String name       = (String) attributes.get("name");
        String providerId = (String) attributes.get("sub");

        User user = findOrCreateUser(email, name, providerId);

        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("dbUserId", user.getId().toString());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                enrichedAttributes,
                "sub"
        );
    }

    private User findOrCreateUser(String email, String name, String providerId) {
        return userRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> linkGoogle(existing, providerId))
                        .orElseGet(() -> createGoogleUser(email, name, providerId)));
    }

    private User linkGoogle(User existing, String providerId) {
        existing.setProvider(Provider.GOOGLE);
        existing.setProviderId(providerId);
        return userRepository.save(existing);
    }

    private User createGoogleUser(String email, String name, String providerId) {
        User user = User.builder()
                .email(email)
                .name(name)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build();
        User saved = userRepository.save(user);
        userProfileRepository.save(UserProfile.builder()
                .user(saved)
                .declaredLevel(Level.BEGINNER)
                .build());
        log.info("Created new Google OAuth2 user: {}", email);
        return saved;
    }
}
