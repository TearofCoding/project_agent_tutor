package com.aitutor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshRequest {

    @NotBlank(message = "refresh token은 필수입니다.")
    private String refreshToken;
}
