package com.aitutor.dto;

import com.aitutor.domain.Level;
import com.aitutor.domain.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SessionDetailResponse {

    private Long id;
    private String subjectName;
    private Level selectedDifficulty;
    private SessionStatus status;
    private BigDecimal score;
    private BigDecimal correctRate;
    private String recommendation;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<InteractionResponse> interactions;
}
