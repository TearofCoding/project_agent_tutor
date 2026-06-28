package com.aitutor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class InitializeSkillRequest {

    @NotBlank(message = "스킬 내용은 필수입니다.")
    private String skillContent;
}
