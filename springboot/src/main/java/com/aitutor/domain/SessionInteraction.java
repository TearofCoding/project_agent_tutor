package com.aitutor.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_interactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SessionInteraction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LearningSession session;

    @Column(name = "interaction_order")
    private Integer interactionOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "topic_tag", length = 100)
    private String topicTag;

    @Enumerated(EnumType.STRING)
    private Level difficulty;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
