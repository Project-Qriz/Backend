package com.qriz.sqld.domain.skill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    // 설문조사
    List<Skill> findByKeyConceptsIn(List<String> keyConcepts);

    List<Skill> findAllByOrderByFrequencyDesc();

    @Query("SELECT s.id FROM Skill s WHERE s.id NOT IN :skillIds")
    List<Long> findAllSkillIdsNotIn(@Param("skillIds") List<Long> skillIds);

    @Query("SELECT s.id FROM Skill s")
    List<Long> findAllIds();

    // 출제 빈도가 높은 상위
    List<Skill> findTop2ByOrderByFrequencyDesc();
}
