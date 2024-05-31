package com.qriz.sqld.domain.skillLevel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillLevelRepository extends JpaRepository<SkillLevel, Long> {
    List<SkillLevel> findByUserId(Long userId);
    List<SkillLevel> findByUserIdAndSkillId(Long userId, Long skillId);
}
