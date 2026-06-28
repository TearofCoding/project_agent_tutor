package com.aitutor.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EndSessionResponse {
    private Long sessionId;
    private BigDecimal score;
    private BigDecimal correctRate;
    private Integer durationSec;
    private String recommendation;
}
