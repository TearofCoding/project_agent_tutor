package com.aitutor.scheduler;

import com.aitutor.domain.LearningSession;
import com.aitutor.domain.SessionStatus;
import com.aitutor.repository.LearningSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTimeoutScheduler {

    private static final int TIMEOUT_MINUTES = 30;

    private final LearningSessionRepository sessionRepository;

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void timeoutInactiveSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<LearningSession> stale = sessionRepository
                .findByStatusAndLastActivityAtBefore(SessionStatus.IN_PROGRESS, cutoff);

        if (stale.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        stale.forEach(s -> {
            s.setStatus(SessionStatus.TIMEOUT);
            s.setEndedAt(now);
        });
        sessionRepository.saveAll(stale);
        log.info("Timed out {} inactive sessions", stale.size());
    }
}
