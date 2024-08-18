package com.qriz.sqld.domain.survey;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qriz.sqld.domain.user.User;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    List<Survey> findByUserIdAndCheckedFalse(Long userId);

    List<Survey> findByUserIdAndCheckedTrue(Long userId);

    List<Survey> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);

    List<Survey> findByUserAndCheckedTrue(User user);

    Optional<Survey> findByUserAndKnowsNothingTrue(User user);
}