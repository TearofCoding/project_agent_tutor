package com.aitutor.service;

import com.aitutor.client.AIAgentClient;
import com.aitutor.domain.*;
import com.aitutor.dto.*;
import com.aitutor.exception.SessionException;
import com.aitutor.exception.UserException;
import com.aitutor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final LearningSessionRepository sessionRepository;
    private final SessionInteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SubjectRepository subjectRepository;
    private final SkillVersionRepository skillVersionRepository;
    private final AIAgentClient aiAgentClient;
    private final LevelCalculator levelCalculator;

    @Transactional
    public StartSessionResponse startSession(Long userId, StartSessionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound("사용자를 찾을 수 없습니다."));
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> SessionException.notFound("과목을 찾을 수 없습니다."));
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        String skillVersion = skillVersionRepository
                .findFirstByStatusOrderByCreatedAtDesc(SkillStatus.SUCCESS)
                .map(SkillVersion::getVersion).orElse("unknown");

        LearningSession session = LearningSession.builder()
                .user(user)
                .subject(subject)
                .selectedDifficulty(request.getSelectedDifficulty())
                .skillVersion(skillVersion)
                .build();
        sessionRepository.save(session);

        SessionStartPayload payload = buildStartPayload(session.getId(), user, profile, subject,
                request.getSelectedDifficulty());
        FastApiSessionResponse apiResponse = aiAgentClient.startSession(payload);

        FastApiSessionResponse.ProblemInfo problem = apiResponse.nextProblem();
        interactionRepository.save(SessionInteraction.builder()
                .session(session)
                .interactionOrder(1)
                .question(problem.question())
                .topicTag(problem.topicTag())
                .difficulty(request.getSelectedDifficulty())
                .build());

        return StartSessionResponse.builder()
                .sessionId(session.getId())
                .firstProblem(new ProblemDto(problem.question(), problem.topicTag()))
                .skillVersion(skillVersion)
                .build();
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(Long userId, Long sessionId, SubmitAnswerRequest request) {
        LearningSession session = findSessionForUser(userId, sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw SessionException.badRequest("진행 중인 세션이 아닙니다.");
        }

        SessionInteraction pending = interactionRepository
                .findFirstBySession_IdAndUserAnswerIsNullOrderByInteractionOrderDesc(sessionId)
                .orElseThrow(() -> SessionException.notFound("진행 중인 문제가 없습니다."));

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        List<String> weakTopics = interactionRepository
                .findIncorrectTopicsByUserId(userId, PageRequest.of(0, 5));

        long startMs = System.currentTimeMillis();
        FastApiSessionResponse apiResponse = aiAgentClient.submitAnswer(sessionId,
                SessionAskPayload.builder()
                        .userAnswer(request.getUserAnswer())
                        .currentQuestion(pending.getQuestion())
                        .currentTopicTag(pending.getTopicTag())
                        .subjectName(session.getSubject().getName())
                        .selectedDifficulty(session.getSelectedDifficulty().name())
                        .effectiveLevel(effectiveLevel(profile).name())
                        .weakTopics(weakTopics)
                        .goal(profile != null ? profile.getGoal() : null)
                        .interactionCount(pending.getInteractionOrder())
                        .build());

        int responseTimeMs = (int) (System.currentTimeMillis() - startMs);

        pending.setUserAnswer(request.getUserAnswer());
        pending.setIsCorrect(apiResponse.isCorrect());
        pending.setFeedback(apiResponse.feedback());
        pending.setResponseTimeMs(responseTimeMs);
        interactionRepository.save(pending);

        FastApiSessionResponse.ProblemInfo next = apiResponse.nextProblem();
        interactionRepository.save(SessionInteraction.builder()
                .session(session)
                .interactionOrder(pending.getInteractionOrder() + 1)
                .question(next.question())
                .topicTag(next.topicTag())
                .difficulty(session.getSelectedDifficulty())
                .build());

        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        return SubmitAnswerResponse.builder()
                .interactionId(pending.getId())
                .isCorrect(apiResponse.isCorrect())
                .feedback(apiResponse.feedback())
                .nextProblem(new ProblemDto(next.question(), next.topicTag()))
                .skillVersion(session.getSkillVersion())
                .responseTimeMs(responseTimeMs)
                .build();
    }

    @Transactional
    public EndSessionResponse endSession(Long userId, Long sessionId) {
        LearningSession session = findSessionForUser(userId, sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw SessionException.badRequest("진행 중인 세션이 아닙니다.");
        }

        List<SessionInteraction> answered = interactionRepository
                .findBySession_IdAndUserAnswerIsNotNull(sessionId);
        long correctCount = answered.stream().filter(i -> Boolean.TRUE.equals(i.getIsCorrect())).count();

        BigDecimal correctRate = answered.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf((double) correctCount / answered.size()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal score = correctRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(now);
        session.setCorrectRate(correctRate);
        session.setScore(score);
        session.setDurationSec((int) ChronoUnit.SECONDS.between(session.getStartedAt(), now));
        sessionRepository.save(session);

        updateCalculatedLevel(userId);

        List<String> weakTopics = interactionRepository
                .findIncorrectTopicsByUserId(userId, PageRequest.of(0, 5));
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        long totalCompleted = sessionRepository.countByStatusIn(
                List.of(SessionStatus.COMPLETED, SessionStatus.TIMEOUT));

        aiAgentClient.endSession(sessionId, SessionEndPayload.builder()
                .sessionId(sessionId)
                .subjectName(session.getSubject().getName())
                .correctRate(correctRate.doubleValue())
                .totalInteractions(answered.size())
                .weakTopics(weakTopics)
                .goal(profile != null ? profile.getGoal() : null)
                .totalCompletedSessions(totalCompleted)
                .build());

        return EndSessionResponse.builder()
                .sessionId(sessionId)
                .score(session.getScore())
                .correctRate(session.getCorrectRate())
                .durationSec(session.getDurationSec())
                .recommendation(null)
                .build();
    }

    private LearningSession findSessionForUser(Long userId, Long sessionId) {
        LearningSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> SessionException.notFound("세션을 찾을 수 없습니다."));
        if (!session.getUser().getId().equals(userId)) {
            throw SessionException.forbidden("접근 권한이 없습니다.");
        }
        return session;
    }

    private void updateCalculatedLevel(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return;
        Level current = profile.getDeclaredLevel() != null ? profile.getDeclaredLevel() : Level.BEGINNER;
        Level calculated = levelCalculator.calculate(userId, current);
        if (calculated != null) {
            profile.setCalculatedLevel(calculated);
            userProfileRepository.save(profile);
        }
    }

    private Level effectiveLevel(UserProfile profile) {
        if (profile == null) return Level.BEGINNER;
        if (profile.getCalculatedLevel() != null) return profile.getCalculatedLevel();
        if (profile.getDeclaredLevel() != null) return profile.getDeclaredLevel();
        return Level.BEGINNER;
    }

    private SessionStartPayload buildStartPayload(Long sessionId, User user, UserProfile profile,
                                                   Subject subject, Level selectedDifficulty) {
        Level declaredLevel = profile != null && profile.getDeclaredLevel() != null
                ? profile.getDeclaredLevel() : Level.BEGINNER;
        Level effective = effectiveLevel(profile);

        List<String> weakTopics = interactionRepository
                .findIncorrectTopicsByUserId(user.getId(), PageRequest.of(0, 5));

        List<LearningSession> lastSessions = sessionRepository
                .findTopByUserId(user.getId(), PageRequest.of(0, 1));
        LearningSession lastSession = lastSessions.isEmpty() ? null : lastSessions.get(0);

        double recentCorrectRate = lastSession != null && lastSession.getCorrectRate() != null
                ? lastSession.getCorrectRate().doubleValue() : 0.0;
        String lastSubject = lastSession != null ? lastSession.getSubject().getName() : null;
        long totalCount = sessionRepository.countByUserIdAndSubjectId(user.getId(), subject.getId());

        return SessionStartPayload.builder()
                .sessionId(sessionId)
                .userId(user.getId())
                .declaredLevel(declaredLevel.name())
                .effectiveLevel(effective.name())
                .selectedDifficulty(selectedDifficulty.name())
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .weakTopics(weakTopics)
                .recentCorrectRate(recentCorrectRate)
                .totalSessionCount((int) totalCount)
                .lastSessionSubject(lastSubject)
                .goal(profile != null ? profile.getGoal() : null)
                .build();
    }
}
