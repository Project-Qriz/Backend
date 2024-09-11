package com.qriz.sqld.domain.survey;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.qriz.sqld.domain.user.User;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    List<Survey> findByUserIdAndCheckedFalse(Long userId);

    List<Survey> findByUserIdAndCheckedTrue(Long userId);

    List<Survey> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);

    List<Survey> findByUserAndCheckedTrue(User user);

    Optional<Survey> findByUserAndKnowsNothingTrue(User user);

    @Modifying
    @Query("DELETE FROM Survey s WHERE s.user = :user")
    void deleteByUser(@Param("user") User user);
}