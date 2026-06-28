package com.aitutor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SubmitAnswerRequest {

    @NotBlank(message = "답변을 입력해주세요.")
    private String userAnswer;
}
