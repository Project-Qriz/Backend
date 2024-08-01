package com.qriz.sqld.domain.userActivity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    List<UserActivity> findByUserId(Long userId);
    List<UserActivity> findByUserIdAndTestInfo(Long userId, String testInfo);
    Optional<UserActivity> findByIdAndUserId(Long activityId, Long userId);
    boolean existsByUserIdAndQuestionCategory(Long userId, int category);
    List<UserActivity> findByUserIdAndQuestionCategory(Long userId, int category);
    Optional<UserActivity> findLatestDailyByUserId(Long userId);
    boolean existsByUserIdAndTestInfo(Long userId, String testInfo);
    List<UserActivity> findByUserIdAndDateBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    Optional<UserActivity> findByUserIdAndTestInfoAndQuestionId(Long userId, String testInfo, Long questionId);
}
