package com.aitutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionStartPayload {

    @JsonProperty("session_id")
    private Long sessionId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("declared_level")
    private String declaredLevel;

    @JsonProperty("effective_level")
    private String effectiveLevel;

    @JsonProperty("selected_difficulty")
    private String selectedDifficulty;

    @JsonProperty("subject_id")
    private Long subjectId;

    @JsonProperty("subject_name")
    private String subjectName;

    @JsonProperty("weak_topics")
    private List<String> weakTopics;

    @JsonProperty("recent_correct_rate")
    private double recentCorrectRate;

    @JsonProperty("total_session_count")
    private int totalSessionCount;

    @JsonProperty("last_session_subject")
    private String lastSessionSubject;

    private String goal;
}
