package com.aitutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiSessionResponse(
        @JsonProperty("session_id")    String sessionId,
        @JsonProperty("interaction_id") String interactionId,
        @JsonProperty("is_correct")    Boolean isCorrect,
        String feedback,
        @JsonProperty("next_problem")  ProblemInfo nextProblem,
        String recommendation,
        @JsonProperty("skill_version") String skillVersion,
        @JsonProperty("response_time_ms") Integer responseTimeMs
) {
    public record ProblemInfo(
            String question,
            @JsonProperty("topic_tag") String topicTag
    ) {}
}
