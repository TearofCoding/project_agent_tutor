package com.aitutor.dto;

import com.aitutor.domain.Level;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InteractionResponse {

    private Long id;
    private Integer interactionOrder;
    private String question;
    private String topicTag;
    private Level difficulty;
    private String userAnswer;
    private Boolean isCorrect;
    private String feedback;
}
