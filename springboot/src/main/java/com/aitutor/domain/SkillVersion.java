package com.aitutor.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "skill_versions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SkillVersion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "skill_content", nullable = false, columnDefinition = "TEXT")
    private String skillContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SkillStatus status = SkillStatus.IN_PROGRESS;

    @Column(name = "trigger_session_id")
    private Long triggerSessionId;

    @Column(name = "correct_rate_before", precision = 5, scale = 2)
    private BigDecimal correctRateBefore;

    @Column(name = "correct_rate_after", precision = 5, scale = 2)
    private BigDecimal correctRateAfter;

    @Column(name = "reflect_summary", columnDefinition = "TEXT")
    private String reflectSummary;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "budget_exceeded", nullable = false)
    @Builder.Default
    private Boolean budgetExceeded = false;

    @Column(name = "rollback_reason", columnDefinition = "TEXT")
    private String rollbackReason;

    @Column(name = "rolled_back_by")
    private Long rolledBackBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
