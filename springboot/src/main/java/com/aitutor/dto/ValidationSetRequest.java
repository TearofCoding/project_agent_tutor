package com.aitutor.dto;

import com.aitutor.domain.Level;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ValidationSetRequest {

    @NotBlank(message = "문제를 입력해주세요.")
    private String question;

    @NotBlank(message = "기대 정답을 입력해주세요.")
    private String expectedAnswer;

    private Level difficulty;
}
