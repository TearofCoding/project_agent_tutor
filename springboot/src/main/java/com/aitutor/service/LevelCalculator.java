package com.aitutor.service;

import com.aitutor.domain.Level;
import com.aitutor.domain.LearningSession;
import com.aitutor.domain.SessionStatus;
import com.aitutor.repository.LearningSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Derives calculated_level from the user's last 3 completed sessions.
 * avg correct_rate >= 70% → level up; <= 40% → level down; else unchanged.
 * Returns null when fewer than 3 sessions exist (caller should use declared_level).
 */
@Component
@RequiredArgsConstructor
public class LevelCalculator {

    private static final int REQUIRED_SESSIONS = 3;
    private static final double THRESHOLD_UP   = 70.0;
    private static final double THRESHOLD_DOWN = 40.0;

    private final LearningSessionRepository sessionRepository;

    public Level calculate(Long userId, Level currentLevel) {
        List<LearningSession> recent = sessionRepository.findRecentByUserAndStatuses(
                userId,
                List.of(SessionStatus.COMPLETED, SessionStatus.TIMEOUT),
                PageRequest.of(0, REQUIRED_SESSIONS));

        if (recent.size() < REQUIRED_SESSIONS) return null;

        double avg = recent.stream()
                .filter(s -> s.getCorrectRate() != null)
                .mapToDouble(s -> s.getCorrectRate().doubleValue())
                .average()
                .orElse(0.0);

        if (avg >= THRESHOLD_UP)   return levelUp(currentLevel);
        if (avg <= THRESHOLD_DOWN) return levelDown(currentLevel);
        return currentLevel;
    }

    private Level levelUp(Level level) {
        return level == Level.ADVANCED ? Level.ADVANCED
                : Level.values()[level.ordinal() + 1];
    }

    private Level levelDown(Level level) {
        return level == Level.BEGINNER ? Level.BEGINNER
                : Level.values()[level.ordinal() - 1];
    }
}
