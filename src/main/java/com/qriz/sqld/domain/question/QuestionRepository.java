package com.qriz.sqld.domain.question;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    /**
     * @param skillId
     * @return
     * skillId 로 문제 조회
     */ 
    List<Question> findBySkillId(Long skillId);

    /**
     *  특정 skillId 목록과 Category 에 해당하는 질문들을 찾는 메서드
     */
    List<Question> findBySkillIdInAndCategory(List<Long> skillIds, int category);

    /**
     *  특정 skillId 와 Category 에 해당하는 질문들을 찾은 메서드
     */
    List<Question> findBySkillIdAndCategory(Long skillId, int category);
}
