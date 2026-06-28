package com.aitutor.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StartSessionResponse {
    private Long sessionId;
    private ProblemDto firstProblem;
    private String skillVersion;
}
