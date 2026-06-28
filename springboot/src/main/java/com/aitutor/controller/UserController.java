package com.aitutor.controller;

import com.aitutor.common.ApiResponse;
import com.aitutor.dto.SessionDetailResponse;
import com.aitutor.dto.SessionSummaryResponse;
import com.aitutor.dto.UpdateProfileRequest;
import com.aitutor.dto.UserProfileResponse;
import com.aitutor.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(Authentication auth) {
        return ApiResponse.ok(userService.getMyProfile(userId(auth)));
    }

    @PutMapping("/me/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                          Authentication auth) {
        return ApiResponse.ok(userService.updateProfile(userId(auth), request), "프로파일이 수정되었습니다.");
    }

    @GetMapping("/me/history")
    public ApiResponse<Page<SessionSummaryResponse>> getHistory(
            @RequestParam(required = false) Long subjectId,
            @PageableDefault(size = 10, sort = "startedAt") Pageable pageable,
            Authentication auth) {
        return ApiResponse.ok(userService.getHistory(userId(auth), subjectId, pageable));
    }

    @GetMapping("/me/history/{sessionId}")
    public ApiResponse<SessionDetailResponse> getHistoryDetail(@PathVariable Long sessionId,
                                                               Authentication auth) {
        return ApiResponse.ok(userService.getHistoryDetail(userId(auth), sessionId));
    }

    private Long userId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
