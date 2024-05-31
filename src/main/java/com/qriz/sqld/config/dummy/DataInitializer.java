package com.qriz.sqld.config.dummy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class DataInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final SkillRepository skillRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 스킬 데이터 나누어 삽입
        List<Skill> skillsPart1 = Arrays.asList(
            createSkill("1과목", "데이터 모델링의 이해", "데이터모델의 이해"),
            createSkill("1과목", "데이터 모델링의 이해", "엔터티"),
            createSkill("1과목", "데이터 모델링의 이해", "속성"),
            createSkill("1과목", "데이터 모델링의 이해", "관계"),
            createSkill("1과목", "데이터 모델링의 이해", "식별자"),
            createSkill("1과목", "데이터 모델과 SQL", "정규화"),
            createSkill("1과목", "데이터 모델과 SQL", "관계와 조인의 이해"),
            createSkill("1과목", "데이터 모델과 SQL", "모델이 표현하는 트랜잭션의 이해"),
            createSkill("1과목", "데이터 모델과 SQL", "Null 속성의 이해"),
            createSkill("1과목", "데이터 모델과 SQL", "본질 식별자 vs 인조 식별자")
        );

        List<Skill> skillsPart2 = Arrays.asList(
            createSkill("2과목", "SQL 기본", "관계형 데이터 베이스 개요"),
            createSkill("2과목", "SQL 기본", "SELECT 문"),
            createSkill("2과목", "SQL 기본", "함수"),
            createSkill("2과목", "SQL 기본", "WHERE 절"),
            createSkill("2과목", "SQL 기본", "GROUP BY, HAVING 절"),
            createSkill("2과목", "SQL 기본", "ORDER BY 절"),
            createSkill("2과목", "SQL 기본", "조인"),
            createSkill("2과목", "SQL 기본", "표준 조인"),
            createSkill("2과목", "SQL 활용", "서브 쿼리"),
            createSkill("2과목", "SQL 활용", "집합 연산자")
        );

        List<Skill> skillsPart3 = Arrays.asList(
            createSkill("2과목", "SQL 활용", "그룹 함수"),
            createSkill("2과목", "SQL 활용", "윈도우 함수"),
            createSkill("2과목", "SQL 활용", "Top N 쿼리"),
            createSkill("2과목", "SQL 활용", "계층형 질의와 셀프 조인"),
            createSkill("2과목", "SQL 활용", "PIVOT 절과 UNPIVOT 절"),
            createSkill("2과목", "SQL 활용", "정규 표현식"),
            createSkill("2과목", "관리구문", "DML"),
            createSkill("2과목", "관리구문", "TCL"),
            createSkill("2과목", "관리구문", "DDL"),
            createSkill("2과목", "관리구문", "DCL")
        );

        // 스킬 리스트 합치기
        List<Skill> skills = new ArrayList<>();
        skills.addAll(skillsPart1);
        skills.addAll(skillsPart2);
        skills.addAll(skillsPart3);

        // 스킬 데이터 삽입
        skillRepository.saveAll(skills);
    }

    private Skill createSkill(String title, String type, String keyConcepts) {
        Skill skill = new Skill();
        skill.setTitle(title);
        skill.setType(type);
        skill.setKeyConcepts(keyConcepts);
        return skill;
    }
}
