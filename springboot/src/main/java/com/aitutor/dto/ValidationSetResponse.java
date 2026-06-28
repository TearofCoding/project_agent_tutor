package com.aitutor.dto;

import com.aitutor.domain.Level;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ValidationSetResponse {
    private Long id;
    private String question;
    private String expectedAnswer;
    private Level difficulty;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
