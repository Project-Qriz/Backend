package com.qriz.sqld.domain.daily;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qriz.sqld.domain.user.User;

@Repository
public interface UserDailyRepository extends JpaRepository<UserDaily, Long> {
    @Query("SELECT DISTINCT ud FROM UserDaily ud LEFT JOIN FETCH ud.plannedSkills WHERE ud.user.id = :userId ORDER BY ud.planDate ASC")
    List<UserDaily> findByUserIdWithPlannedSkillsOrderByPlanDateAsc(@Param("userId") Long userId);

    @Query("SELECT ud FROM UserDaily ud LEFT JOIN FETCH ud.plannedSkills WHERE ud.user.id = :userId AND ud.dayNumber = :dayNumber")
    Optional<UserDaily> findByUserIdAndDayNumberWithPlannedSkills(@Param("userId") Long userId,
            @Param("dayNumber") String dayNumber);

    Optional<UserDaily> findByUserIdAndPlanDate(Long userId, LocalDate planDate);

    Optional<UserDaily> findByUserIdAndDayNumber(Long userId, String dayNumber);

    List<UserDaily> findByUserIdAndDayNumberBetween(Long userId, String startDayNumber, String endDayNumber);

    List<UserDaily> findAllByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserDaily ud where ud.user = :user")
    void deleteByUser(@Param("user") User user);

    // 특정 유저의 최대 플랜 버전 조회
    @Query("SELECT MAX(ud.planVersion) FROM UserDaily ud WHERE ud.user.id = :userId")
    Optional<Integer> findMaxPlanVersionByUserId(@Param("userId") Long userId);

    // 특정 버전 수를 초과하는 오래된 버전의 플랜들 조회
    @Query("SELECT DISTINCT ud FROM UserDaily ud LEFT JOIN FETCH ud.plannedSkills WHERE ud.user.id = :userId AND ud.planVersion <= (SELECT MAX(ud2.planVersion) - :keepVersions FROM UserDaily ud2 WHERE ud2.user.id = :userId) ORDER BY ud.planDate ASC")
    List<UserDaily> findOldVersionsByUserId(
            @Param("userId") Long userId,
            @Param("keepVersions") int keepVersions);

    @Query("SELECT ud FROM UserDaily ud LEFT JOIN FETCH ud.plannedSkills WHERE ud.user.id = :userId AND ud.dayNumber LIKE 'Day%' AND ud.planVersion = :planVersion")
    List<UserDaily> findByUserIdAndPlanVersion(@Param("userId") Long userId, @Param("planVersion") int planVersion);

    // 현재 활성화된(아카이브되지 않은) 플랜 조회 - plannedSkills도 함께 조회하도록 수정
    @Query("SELECT DISTINCT ud FROM UserDaily ud LEFT JOIN FETCH ud.plannedSkills WHERE ud.user.id = :userId AND ud.isArchived = false ORDER BY ud.planDate ASC")
    List<UserDaily> findByUserIdAndIsArchivedFalse(@Param("userId") Long userId);

    // 특정 유저의 완료된 Day 수 카운트 (현재 활성화된 플랜에서만)
    @Query("SELECT COUNT(ud) FROM UserDaily ud WHERE ud.user.id = :userId AND ud.completed = true AND ud.isArchived = false")
    int countByUserIdAndCompletedTrue(@Param("userId") Long userId);
}
