package com.aitutor.service;

import com.aitutor.domain.LearningSession;
import com.aitutor.domain.Level;
import com.aitutor.repository.LearningSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LevelCalculatorTest {

    @Mock
    private LearningSessionRepository sessionRepository;

    @InjectMocks
    private LevelCalculator calculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void returnsNullWhenFewerThanThreeSessions() {
        when(sessionRepository.findRecentByUserAndStatuses(any(), any(), any()))
                .thenReturn(List.of(session(80.0)));

        assertThat(calculator.calculate(1L, Level.BEGINNER)).isNull();
    }

    @Test
    void returnsNullWhenNoSessions() {
        when(sessionRepository.findRecentByUserAndStatuses(any(), any(), any()))
                .thenReturn(List.of());

        assertThat(calculator.calculate(1L, Level.INTERMEDIATE)).isNull();
    }

    @Test
    void levelsUpWhenAverageAtOrAbove70() {
        when(sessionRepository.findRecentByUserAndStatuses(any(), any(), any()))
                .thenReturn(List.of(session(70.0), session(75.0), session(80.0)));

        assertThat(calculator.calculate(1L, Level.BEGINNER)).isEqualTo(Level.INTERMEDIATE);
    }

    @Test
    void levelsDownWhenAverageAtOrBelow40() {
        when(sessionRepository.findRecentByUserAndStatuses(any(), any(), any()))
                .thenReturn(List.of(session(20.0), session(40.0), session(35.0)));

        assertThat(calculator.calculate(1L, Level.INTERMEDIATE)).isEqualTo(Level.BEGINNER);
    }

    @Test
    void keepsCurrentLevelWhenAverageBetween40And70() {
        when(sessionRepository.findRecentByUserAndStatuses(any(), any(), any()))
                .thenReturn(List.of(session(50.0), session(60.0), session(55.0)));

        assertThat(calculator.calculate(1L, Level.INTERMEDIATE)).isEqualTo(Level.INTERMEDIATE);
    }

    @Test
    void doesNotExceedAdvancedOnLevelUp() {
        when(sessionRepository.findRecentByUserAndStatuses(any(), any(), any()))
                .thenReturn(List.of(session(80.0), session(85.0), session(90.0)));

        assertThat(calculator.calculate(1L, Level.ADVANCED)).isEqualTo(Level.ADVANCED);
    }

    @Test
    void doesNotGoBelowBeginnerOnLevelDown() {
        when(sessionRepository.findRecentByUserAndStatuses(any(), any(), any()))
                .thenReturn(List.of(session(10.0), session(20.0), session(30.0)));

        assertThat(calculator.calculate(1L, Level.BEGINNER)).isEqualTo(Level.BEGINNER);
    }

    private LearningSession session(double rate) {
        return LearningSession.builder()
                .correctRate(BigDecimal.valueOf(rate))
                .build();
    }
}
