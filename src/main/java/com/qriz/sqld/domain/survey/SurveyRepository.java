package com.qriz.sqld.domain.survey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    // 진단고사 추천용 설문조사 체크 여부
    List<Survey> findByUserIdAndCheckedFalse(Long userId);
    List<Survey> findByUserIdAndCheckedTrue(Long userId);
    boolean existsByUserId(Long userId);
}
