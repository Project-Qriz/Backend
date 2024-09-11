package com.qriz.sqld.domain.skillLevel;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.user.User;

@Repository
public interface SkillLevelRepository extends JpaRepository<SkillLevel, Long> {
    List<SkillLevel> findByUserId(Long userId);

    List<SkillLevel> findByUserIdAndSkillId(Long userId, Long skillId);

    SkillLevel findByUserIdAndSkillIdAndDifficulty(Long userId, Long skillId, Integer difficulty);

    Optional<SkillLevel> findByUserAndSkill(User user, Skill skill);

    Optional<SkillLevel> findByUserAndSkillAndDifficulty(User user, Skill skill, Integer difficulty);

    List<SkillLevel> findByUserIdAndLastUpdatedBetween(Long userId, LocalDateTime start, LocalDateTime end);

    List<SkillLevel> findTop3ByUserIdOrderByCurrentAccuracyAsc(Long userId);

    @Modifying
    @Query("DELETE FROM SkillLevel sl WHERE sl.user = :user")
    void deleteByUser(@Param("user") User user);
}
