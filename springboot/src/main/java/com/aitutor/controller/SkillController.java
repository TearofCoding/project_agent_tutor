package com.aitutor.controller;

import com.aitutor.client.AIAgentClient;
import com.aitutor.common.ApiResponse;
import com.aitutor.domain.SkillVersion;
import com.aitutor.dto.*;
import com.aitutor.exception.SkillException;
import com.aitutor.exception.UserException;
import com.aitutor.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;
    private final AIAgentClient aiAgentClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    // ── INTERNAL endpoints (X-Internal-Api-Key) ───────────────────────────────

    @GetMapping("/active")
    public ApiResponse<SkillActiveResponse> getActive(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        requireInternalKey(apiKey);
        SkillVersion sv = skillService.getActiveSkill();
        return ApiResponse.ok(new SkillActiveResponse(sv.getVersion(), sv.getSkillContent()));
    }

    @GetMapping("/validation-set/active")
    public ApiResponse<List<ValidationSetResponse>> getActiveValidationSet(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        requireInternalKey(apiKey);
        return ApiResponse.ok(skillService.getActiveValidationSet());
    }

    @PostMapping("/pipeline/result")
    public ApiResponse<SkillVersionResponse> savePipelineResult(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestBody PipelineResultRequest request) {
        requireInternalKey(apiKey);
        return ApiResponse.ok(skillService.savePipelineResult(request), "파이프라인 결과가 저장되었습니다.");
    }

    // ── ADMIN endpoints (JWT ROLE_ADMIN) ──────────────────────────────────────

    @PostMapping("/initialize")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> initialize(@Valid @RequestBody InitializeSkillRequest request) {
        skillService.initialize(request);
        return ApiResponse.ok(null, "스킬이 초기화되었습니다.");
    }

    @GetMapping("/versions")
    public ApiResponse<List<SkillVersionResponse>> getVersions() {
        return ApiResponse.ok(skillService.getVersions());
    }

    @GetMapping("/versions/{id}")
    public ApiResponse<SkillVersionResponse> getVersion(@PathVariable Long id) {
        return ApiResponse.ok(skillService.getVersion(id));
    }

    @PostMapping("/rollback/{versionId}")
    public ApiResponse<SkillVersionResponse> rollback(@PathVariable Long versionId, Authentication auth) {
        Long adminUserId = Long.parseLong(auth.getName());
        return ApiResponse.ok(skillService.rollback(versionId, adminUserId), "롤백이 완료되었습니다.");
    }

    @GetMapping("/validation-set")
    public ApiResponse<List<ValidationSetResponse>> getValidationSet() {
        return ApiResponse.ok(skillService.getValidationSet());
    }

    @PostMapping("/validation-set")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ValidationSetResponse> addValidationQuestion(
            @Valid @RequestBody ValidationSetRequest request) {
        return ApiResponse.ok(skillService.addValidationQuestion(request), "검증 문제가 등록되었습니다.");
    }

    @DeleteMapping("/validation-set/{id}")
    public ApiResponse<Void> deleteValidationQuestion(@PathVariable Long id) {
        skillService.deleteValidationQuestion(id);
        return ApiResponse.ok(null, "검증 문제가 삭제되었습니다.");
    }

    @PostMapping("/pipeline/trigger")
    public ApiResponse<Void> forceTriggerPipeline() {
        try {
            aiAgentClient.triggerPipeline();
        } catch (Exception e) {
            throw SkillException.badGateway("FastAPI 파이프라인 트리거 실패: " + e.getMessage());
        }
        return ApiResponse.ok(null, "파이프라인이 트리거되었습니다.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireInternalKey(String apiKey) {
        if (!internalApiKey.equals(apiKey)) {
            throw UserException.forbidden("Forbidden");
        }
    }
}
