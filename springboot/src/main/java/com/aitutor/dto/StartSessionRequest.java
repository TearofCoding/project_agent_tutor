package com.aitutor.dto;

import com.aitutor.domain.Level;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StartSessionRequest {

    @NotNull(message = "과목을 선택해주세요.")
    private Long subjectId;

    @NotNull(message = "난이도를 선택해주세요.")
    private Level selectedDifficulty;
}
