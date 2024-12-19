package com.qriz.sqld.domain.clip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qriz.sqld.domain.UserActivity.UserActivity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClipRepository extends JpaRepository<Clipped, Long> {
        List<Clipped> findByUserActivity_User_Id(Long userId);

        Optional<Clipped> findByUserActivity_Id(Long activityId);

        boolean existsByUserActivity_Id(Long activityId);

        // testInfo로 조회 (데일리 테스트 "Day1", 모의고사 "1회차" 모두 처리)
        @Query("SELECT c FROM Clipped c " +
                        "WHERE c.userActivity.user.id = :userId " +
                        "AND c.userActivity.testInfo = :testInfo " +
                        "ORDER BY c.userActivity.questionNum")
        List<Clipped> findByUserIdAndTestInfoOrderByQuestionNum(
                        @Param("userId") Long userId,
                        @Param("testInfo") String testInfo);

        List<Clipped> findByUserActivity_User_IdOrderByDateDesc(Long userId);

        @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.question.skill.keyConcepts IN :keyConcepts ORDER BY c.date DESC")
        List<Clipped> findByUserIdAndKeyConcepts(@Param("userId") Long userId,
                        @Param("keyConcepts") List<String> keyConcepts);

        @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.correction = false ORDER BY c.date DESC")
        List<Clipped> findIncorrectByUserId(@Param("userId") Long userId);

        @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.correction = false AND c.userActivity.question.skill.keyConcepts IN :keyConcepts ORDER BY c.date DESC")
        List<Clipped> findIncorrectByUserIdAndKeyConcepts(@Param("userId") Long userId,
                        @Param("keyConcepts") List<String> keyConcepts);

        @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.question.category = :category ORDER BY c.date DESC")
        List<Clipped> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") Integer category);

        @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.correction = false AND c.userActivity.question.category = :category ORDER BY c.date DESC")
        List<Clipped> findIncorrectByUserIdAndCategory(@Param("userId") Long userId,
                        @Param("category") Integer category);

        @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.question.skill.keyConcepts IN :keyConcepts AND c.userActivity.question.category = :category ORDER BY c.date DESC")
        List<Clipped> findByUserIdAndKeyConceptsAndCategory(@Param("userId") Long userId,
                        @Param("keyConcepts") List<String> keyConcepts, @Param("category") Integer category);

        @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.correction = false AND c.userActivity.question.skill.keyConcepts IN :keyConcepts AND c.userActivity.question.category = :category ORDER BY c.date DESC")
        List<Clipped> findIncorrectByUserIdAndKeyConceptsAndCategory(@Param("userId") Long userId,
                        @Param("keyConcepts") List<String> keyConcepts, @Param("category") Integer category);

        @Query("SELECT MAX(CAST(SUBSTRING(c.userActivity.testInfo, 4) AS int)) FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.testInfo LIKE 'Day%'")
        Integer findLatestDayNumberByUserId(@Param("userId") Long userId);

        @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.testInfo = :dayNumber ORDER BY c.userActivity.questionNum")
        List<Clipped> findByUserIdAndDayNumberOrderByQuestionNum(@Param("userId") Long userId,
                        @Param("dayNumber") String dayNumber);

        @Query("SELECT DISTINCT ud.dayNumber FROM UserDaily ud WHERE ud.user.id = :userId AND ud.completed = true ORDER BY ud.dayNumber DESC")
        List<String> findCompletedDayNumbersByUserId(@Param("userId") Long userId);

        List<Clipped> findByUserActivity_User_IdAndUserActivity_TestInfoOrderByDateDesc(Long userId, String testInfo);

        @Query("SELECT c FROM Clipped c " +
                        "JOIN c.userActivity ua " +
                        "JOIN ua.question q " +
                        "WHERE ua.user.id = :userId " +
                        "AND (:testInfo IS NULL OR ua.testInfo = :testInfo) " +
                        "AND (:category IS NULL OR q.category = :category) " +
                        "ORDER BY c.date DESC")
        List<Clipped> findByUserIdAndFilters(
                        @Param("userId") Long userId,
                        @Param("testInfo") String testInfo,
                        @Param("category") Integer category);

        @Query("SELECT c FROM Clipped c " +
                        "JOIN c.userActivity ua " +
                        "JOIN ua.question q " +
                        "JOIN q.skill s " +
                        "WHERE ua.user.id = :userId " +
                        "AND (:testInfo IS NULL OR ua.testInfo = :testInfo) " +
                        "AND (:category IS NULL OR q.category = :category) " +
                        "AND s.keyConcepts IN :keyConcepts " +
                        "ORDER BY c.date DESC")
        List<Clipped> findByUserIdAndFiltersWithKeyConcepts(
                        @Param("userId") Long userId,
                        @Param("testInfo") String testInfo,
                        @Param("category") Integer category,
                        @Param("keyConcepts") List<String> keyConcepts);

        @Query("SELECT DISTINCT ua.testInfo FROM Clipped c " +
                        "JOIN c.userActivity ua " +
                        "WHERE ua.user.id = :userId " +
                        "ORDER BY ua.testInfo")
        List<String> findDistinctTestInfosByUserId(@Param("userId") Long userId);

        void deleteByUserActivity(UserActivity userActivity);

        @Query("SELECT c FROM Clipped c " +
                        "WHERE c.userActivity.user.id = :userId " +
                        "AND c.userActivity.testInfo = :testInfo")
        List<Clipped> findByUserActivity_UserIdAndUserActivity_TestInfo(
                        @Param("userId") Long userId,
                        @Param("testInfo") String testInfo);
}
