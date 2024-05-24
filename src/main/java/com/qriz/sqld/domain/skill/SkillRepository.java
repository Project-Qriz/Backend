package com.qriz.sqld.domain.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    
    // 설문조사
    List<Skill> findByKeyConceptsIn(List<String> keyConcepts);
}
