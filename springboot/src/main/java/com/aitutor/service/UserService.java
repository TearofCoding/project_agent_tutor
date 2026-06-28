package com.aitutor.service;

import com.aitutor.domain.LearningSession;
import com.aitutor.domain.SessionInteraction;
import com.aitutor.domain.User;
import com.aitutor.domain.UserProfile;
import com.aitutor.dto.*;
import com.aitutor.exception.UserException;
import com.aitutor.repository.LearningSessionRepository;
import com.aitutor.repository.SessionInteractionRepository;
import com.aitutor.repository.UserProfileRepository;
import com.aitutor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final LearningSessionRepository sessionRepository;
    private final SessionInteractionRepository interactionRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound("사용자를 찾을 수 없습니다."));
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .declaredLevel(profile != null ? profile.getDeclaredLevel() : null)
                .calculatedLevel(profile != null ? profile.getCalculatedLevel() : null)
                .goal(profile != null ? profile.getGoal() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound("사용자를 찾을 수 없습니다."));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().user(user).build());

        if (request.getDeclaredLevel() != null) {
            profile.setDeclaredLevel(request.getDeclaredLevel());
        }
        if (request.getGoal() != null) {
            profile.setGoal(request.getGoal());
        }
        userProfileRepository.save(profile);

        return getMyProfile(userId);
    }

    @Transactional(readOnly = true)
    public Page<SessionSummaryResponse> getHistory(Long userId, Long subjectId, Pageable pageable) {
        return sessionRepository.findByUserIdAndOptionalSubjectId(userId, subjectId, pageable)
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse getHistoryDetail(Long userId, Long sessionId) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> UserException.notFound("세션을 찾을 수 없습니다."));

        if (!session.getUser().getId().equals(userId)) {
            throw UserException.forbidden("접근 권한이 없습니다.");
        }

        List<InteractionResponse> interactions = interactionRepository
                .findBySessionIdOrderByInteractionOrderAsc(sessionId)
                .stream()
                .map(this::toInteractionResponse)
                .toList();

        return SessionDetailResponse.builder()
                .id(session.getId())
                .subjectName(session.getSubject().getName())
                .selectedDifficulty(session.getSelectedDifficulty())
                .status(session.getStatus())
                .score(session.getScore())
                .correctRate(session.getCorrectRate())
                .recommendation(session.getRecommendation())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .interactions(interactions)
                .build();
    }

    private SessionSummaryResponse toSummary(LearningSession s) {
        return SessionSummaryResponse.builder()
                .id(s.getId())
                .subjectName(s.getSubject().getName())
                .selectedDifficulty(s.getSelectedDifficulty())
                .status(s.getStatus())
                .score(s.getScore())
                .correctRate(s.getCorrectRate())
                .startedAt(s.getStartedAt())
                .endedAt(s.getEndedAt())
                .build();
    }

    private InteractionResponse toInteractionResponse(SessionInteraction i) {
        return InteractionResponse.builder()
                .id(i.getId())
                .interactionOrder(i.getInteractionOrder())
                .question(i.getQuestion())
                .topicTag(i.getTopicTag())
                .difficulty(i.getDifficulty())
                .userAnswer(i.getUserAnswer())
                .isCorrect(i.getIsCorrect())
                .feedback(i.getFeedback())
                .build();
    }
}
