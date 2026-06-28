package com.aitutor.controller;

import com.aitutor.common.ApiResponse;
import com.aitutor.dto.EndSessionResponse;
import com.aitutor.dto.StartSessionRequest;
import com.aitutor.dto.StartSessionResponse;
import com.aitutor.dto.SubmitAnswerRequest;
import com.aitutor.dto.SubmitAnswerResponse;
import com.aitutor.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StartSessionResponse> startSession(@Valid @RequestBody StartSessionRequest request,
                                                          Authentication auth) {
        return ApiResponse.ok(sessionService.startSession(userId(auth), request), "세션이 시작되었습니다.");
    }

    @PostMapping("/{sessionId}/interactions")
    public ApiResponse<SubmitAnswerResponse> submitAnswer(@PathVariable Long sessionId,
                                                          @Valid @RequestBody SubmitAnswerRequest request,
                                                          Authentication auth) {
        return ApiResponse.ok(sessionService.submitAnswer(userId(auth), sessionId, request));
    }

    @PostMapping("/{sessionId}/end")
    public ApiResponse<EndSessionResponse> endSession(@PathVariable Long sessionId, Authentication auth) {
        return ApiResponse.ok(sessionService.endSession(userId(auth), sessionId), "세션이 종료되었습니다.");
    }

    private Long userId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
