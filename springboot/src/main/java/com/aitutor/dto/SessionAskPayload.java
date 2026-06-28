package com.aitutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionAskPayload {

    @JsonProperty("user_answer")
    private String userAnswer;

    @JsonProperty("current_question")
    private String currentQuestion;

    @JsonProperty("current_topic_tag")
    private String currentTopicTag;

    @JsonProperty("subject_name")
    private String subjectName;

    @JsonProperty("selected_difficulty")
    private String selectedDifficulty;

    @JsonProperty("effective_level")
    private String effectiveLevel;

    @JsonProperty("weak_topics")
    private List<String> weakTopics;

    private String goal;

    @JsonProperty("interaction_count")
    private int interactionCount;
}
