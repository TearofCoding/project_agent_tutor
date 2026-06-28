package com.aitutor.repository;

import com.aitutor.domain.SkillStatus;
import com.aitutor.domain.SkillVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillVersionRepository extends JpaRepository<SkillVersion, Long> {
    Optional<SkillVersion> findFirstByStatusOrderByCreatedAtDesc(SkillStatus status);
}
