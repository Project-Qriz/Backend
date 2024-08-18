package com.qriz.sqld.domain.daily;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDailyRepository extends JpaRepository<UserDaily, Long> {
    @Query("SELECT DISTINCT ud FROM UserDaily ud LEFT JOIN FETCH ud.plannedSkills WHERE ud.user.id = :userId ORDER BY ud.planDate ASC")
    List<UserDaily> findByUserIdWithPlannedSkillsOrderByPlanDateAsc(@Param("userId") Long userId);

    @Query("SELECT ud FROM UserDaily ud LEFT JOIN FETCH ud.plannedSkills WHERE ud.user.id = :userId AND ud.dayNumber = :dayNumber")
    Optional<UserDaily> findByUserIdAndDayNumberWithPlannedSkills(@Param("userId") Long userId, @Param("dayNumber") String dayNumber);

    Optional<UserDaily> findByUserIdAndPlanDate(Long userId, LocalDate planDate);

    Optional<UserDaily> findByUserIdAndDayNumber(Long userId, String dayNumber);

    List<UserDaily> findByUserIdAndDayNumberBetween(Long userId, String startDayNumber, String endDayNumber);

    List<UserDaily> findAllByUserId(Long userId);
}
