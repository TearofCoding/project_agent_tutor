package com.aitutor.dto;

import com.aitutor.domain.Level;
import com.aitutor.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {

    private Long id;
    private String email;
    private String name;
    private Role role;
    private Level declaredLevel;
    private Level calculatedLevel;
    private String goal;
    private LocalDateTime createdAt;
}
