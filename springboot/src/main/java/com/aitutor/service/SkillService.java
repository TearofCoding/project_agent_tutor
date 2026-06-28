package com.aitutor.service;

import com.aitutor.domain.SkillStatus;
import com.aitutor.domain.SkillValidationSet;
import com.aitutor.domain.SkillVersion;
import com.aitutor.dto.*;
import com.aitutor.exception.SkillException;
import com.aitutor.repository.SkillValidationSetRepository;
import com.aitutor.repository.SkillVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private static final List<String> REQUIRED_SECTIONS = List.of(
            "# 교수 원칙",
            "# 난이도별 전략",
            "# 약점 토픽 대응 전략",
            "# 피드백 규칙"
    );

    private final SkillVersionRepository skillVersionRepository;
    private final SkillValidationSetRepository validationSetRepository;

    // ── Active skill ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SkillVersion getActiveSkill() {
        return skillVersionRepository
                .findFirstByStatusOrderByCreatedAtDesc(SkillStatus.SUCCESS)
                .orElseThrow(() -> SkillException.notFound("활성화된 스킬이 없습니다."));
    }

    @Transactional
    public SkillVersion initialize(InitializeSkillRequest request) {
        validateContent(request.getSkillContent());
        long count = skillVersionRepository.count();
        return skillVersionRepository.save(SkillVersion.builder()
                .version("v" + (count + 1) + ".0")
                .skillContent(request.getSkillContent())
                .status(SkillStatus.SUCCESS)
                .build());
    }

    // ── Version history ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SkillVersionResponse> getVersions() {
        return skillVersionRepository.findAll().stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillVersionResponse getVersion(Long id) {
        return toVersionResponse(skillVersionRepository.findById(id)
                .orElseThrow(() -> SkillException.notFound("버전을 찾을 수 없습니다.")));
    }

    @Transactional
    public SkillVersionResponse rollback(Long versionId, Long adminUserId) {
        SkillVersion target = skillVersionRepository.findById(versionId)
                .orElseThrow(() -> SkillException.notFound("버전을 찾을 수 없습니다."));
        long count = skillVersionRepository.count();
        return toVersionResponse(skillVersionRepository.save(SkillVersion.builder()
                .version("v" + (count + 1) + ".0")
                .skillContent(target.getSkillContent())
                .status(SkillStatus.SUCCESS)
                .rollbackReason("Rollback to " + target.getVersion())
                .rolledBackBy(adminUserId)
                .build()));
    }

    // ── Validation set ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ValidationSetResponse> getValidationSet() {
        return validationSetRepository.findAll().stream()
                .map(this::toValidationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ValidationSetResponse> getActiveValidationSet() {
        return validationSetRepository.findByIsActiveTrue().stream()
                .map(this::toValidationResponse)
                .toList();
    }

    @Transactional
    public ValidationSetResponse addValidationQuestion(ValidationSetRequest request) {
        return toValidationResponse(validationSetRepository.save(SkillValidationSet.builder()
                .question(request.getQuestion())
                .expectedAnswer(request.getExpectedAnswer())
                .difficulty(request.getDifficulty())
                .build()));
    }

    @Transactional
    public void deleteValidationQuestion(Long id) {
        SkillValidationSet q = validationSetRepository.findById(id)
                .orElseThrow(() -> SkillException.notFound("검증 문제를 찾을 수 없습니다."));
        q.setIsActive(false);
        validationSetRepository.save(q);
    }

    // ── Pipeline result (INTERNAL) ────────────────────────────────────────────

    @Transactional
    public SkillVersionResponse savePipelineResult(PipelineResultRequest request) {
        return toVersionResponse(skillVersionRepository.save(SkillVersion.builder()
                .version(request.version())
                .skillContent(request.skillContent())
                .status(SkillStatus.valueOf(request.status()))
                .correctRateBefore(bd(request.correctRateBefore()))
                .correctRateAfter(bd(request.correctRateAfter()))
                .reflectSummary(request.reflectSummary())
                .tokensUsed(request.tokensUsed())
                .budgetExceeded(request.budgetExceeded())
                .build()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateContent(String content) {
        if (content.length() < 200) {
            throw SkillException.badRequest("스킬 내용은 200자 이상이어야 합니다.");
        }
        if (REQUIRED_SECTIONS.stream().anyMatch(s -> !content.contains(s))) {
            throw SkillException.badRequest(
                    "필수 섹션이 누락되었습니다: 교수 원칙, 난이도별 전략, 약점 토픽 대응 전략, 피드백 규칙");
        }
    }

    private SkillVersionResponse toVersionResponse(SkillVersion v) {
        return SkillVersionResponse.builder()
                .id(v.getId())
                .version(v.getVersion())
                .skillContent(v.getSkillContent())
                .status(v.getStatus())
                .correctRateBefore(v.getCorrectRateBefore())
                .correctRateAfter(v.getCorrectRateAfter())
                .reflectSummary(v.getReflectSummary())
                .tokensUsed(v.getTokensUsed())
                .budgetExceeded(v.getBudgetExceeded())
                .rollbackReason(v.getRollbackReason())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private ValidationSetResponse toValidationResponse(SkillValidationSet q) {
        return ValidationSetResponse.builder()
                .id(q.getId())
                .question(q.getQuestion())
                .expectedAnswer(q.getExpectedAnswer())
                .difficulty(q.getDifficulty())
                .isActive(q.getIsActive())
                .createdAt(q.getCreatedAt())
                .build();
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
