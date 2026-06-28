package com.aitutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionEndPayload {

    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("subject_name")
    private String subjectName;

    @JsonProperty("correct_rate")
    private double correctRate;

    @JsonProperty("total_interactions")
    private int totalInteractions;

    @JsonProperty("weak_topics")
    private List<String> weakTopics;

    private String goal;

    @JsonProperty("total_completed_sessions")
    private long totalCompletedSessions;
}
