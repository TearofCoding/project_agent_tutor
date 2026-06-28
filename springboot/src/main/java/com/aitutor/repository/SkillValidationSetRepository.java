package com.aitutor.repository;

import com.aitutor.domain.SkillValidationSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillValidationSetRepository extends JpaRepository<SkillValidationSet, Long> {
    List<SkillValidationSet> findByIsActiveTrue();
    long countByIsActiveTrue();
}
