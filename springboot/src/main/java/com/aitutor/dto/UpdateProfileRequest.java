package com.aitutor.dto;

import com.aitutor.domain.Level;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdateProfileRequest {

    private Level declaredLevel;

    @Size(max = 500, message = "목표는 500자 이내여야 합니다.")
    private String goal;
}
