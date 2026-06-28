package com.aitutor.repository;

import com.aitutor.domain.LearningSession;
import com.aitutor.domain.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    @Query("SELECT s FROM LearningSession s WHERE s.user.id = :userId AND s.status IN :statuses ORDER BY s.startedAt DESC")
    List<LearningSession> findRecentByUserAndStatuses(
            @Param("userId") Long userId,
            @Param("statuses") List<SessionStatus> statuses,
            Pageable pageable);

    @Query("SELECT s FROM LearningSession s WHERE s.user.id = :userId AND (:subjectId IS NULL OR s.subject.id = :subjectId) ORDER BY s.startedAt DESC")
    Page<LearningSession> findByUserIdAndOptionalSubjectId(
            @Param("userId") Long userId,
            @Param("subjectId") Long subjectId,
            Pageable pageable);

    @Query("SELECT s FROM LearningSession s WHERE s.id = :id AND s.user.id = :userId")
    Optional<LearningSession> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<LearningSession> findByStatusAndLastActivityAtBefore(SessionStatus status, LocalDateTime cutoff);

    @Query("SELECT s FROM LearningSession s WHERE s.user.id = :userId ORDER BY s.startedAt DESC")
    List<LearningSession> findTopByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM LearningSession s WHERE s.user.id = :userId AND s.subject.id = :subjectId")
    long countByUserIdAndSubjectId(@Param("userId") Long userId, @Param("subjectId") Long subjectId);

    long countByStatusIn(java.util.Collection<SessionStatus> statuses);
}
