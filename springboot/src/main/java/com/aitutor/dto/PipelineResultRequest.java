package com.aitutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PipelineResultRequest(
        String version,
        @JsonProperty("skill_content")      String skillContent,
        String status,
        @JsonProperty("correct_rate_before") double correctRateBefore,
        @JsonProperty("correct_rate_after")  double correctRateAfter,
        @JsonProperty("reflect_summary")     String reflectSummary,
        @JsonProperty("tokens_used")         int tokensUsed,
        @JsonProperty("budget_exceeded")     boolean budgetExceeded
) {}
