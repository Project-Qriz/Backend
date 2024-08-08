package com.qriz.sqld.domain.clip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClipRepository extends JpaRepository<Clipped, Long> {
    List<Clipped> findByUserActivity_User_Id(Long userId);

    Optional<Clipped> findByUserActivity_Id(Long activityId);

    boolean existsByUserActivity_Id(Long activityId);

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
    List<Clipped> findIncorrectByUserIdAndCategory(@Param("userId") Long userId, @Param("category") Integer category);

    @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.question.skill.keyConcepts IN :keyConcepts AND c.userActivity.question.category = :category ORDER BY c.date DESC")
    List<Clipped> findByUserIdAndKeyConceptsAndCategory(@Param("userId") Long userId,
            @Param("keyConcepts") List<String> keyConcepts, @Param("category") Integer category);

    @Query("SELECT c FROM Clipped c WHERE c.userActivity.user.id = :userId AND c.userActivity.correction = false AND c.userActivity.question.skill.keyConcepts IN :keyConcepts AND c.userActivity.question.category = :category ORDER BY c.date DESC")
    List<Clipped> findIncorrectByUserIdAndKeyConceptsAndCategory(@Param("userId") Long userId,
            @Param("keyConcepts") List<String> keyConcepts, @Param("category") Integer category);
}
