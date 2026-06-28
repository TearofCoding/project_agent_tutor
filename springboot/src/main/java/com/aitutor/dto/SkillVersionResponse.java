package com.aitutor.dto;

import com.aitutor.domain.SkillStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SkillVersionResponse {
    private Long id;
    private String version;
    private String skillContent;
    private SkillStatus status;
    private BigDecimal correctRateBefore;
    private BigDecimal correctRateAfter;
    private String reflectSummary;
    private Integer tokensUsed;
    private Boolean budgetExceeded;
    private String rollbackReason;
    private LocalDateTime createdAt;
}
