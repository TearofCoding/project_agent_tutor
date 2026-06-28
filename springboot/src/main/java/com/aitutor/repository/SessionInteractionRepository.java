package com.aitutor.repository;

import com.aitutor.domain.SessionInteraction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionInteractionRepository extends JpaRepository<SessionInteraction, Long> {

    List<SessionInteraction> findBySessionIdOrderByInteractionOrderAsc(Long sessionId);

    Optional<SessionInteraction> findFirstBySession_IdAndUserAnswerIsNullOrderByInteractionOrderDesc(Long sessionId);

    List<SessionInteraction> findBySession_IdAndUserAnswerIsNotNull(Long sessionId);

    @Query("SELECT DISTINCT si.topicTag FROM SessionInteraction si " +
           "WHERE si.session.user.id = :userId AND si.isCorrect = false AND si.topicTag IS NOT NULL " +
           "ORDER BY si.topicTag")
    List<String> findIncorrectTopicsByUserId(@Param("userId") Long userId, Pageable pageable);
}
