package com.aitutor.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubmitAnswerResponse {
    private Long interactionId;
    private Boolean isCorrect;
    private String feedback;
    private ProblemDto nextProblem;
    private String skillVersion;
    private Integer responseTimeMs;
}
