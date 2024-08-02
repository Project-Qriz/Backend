package com.qriz.sqld.config.dummy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class DataInitializer {

    private final SkillRepository skillRepository;

    /**
     * spring.jpa.hibernate.ddl-auto=create 일 때만 데이터를 삽입
     * ContextRefreshedEvent를 사용하여 애플리케이션 컨텍스트가 초기화될 때 데이터베이스 상태를 확인하고, 필요한 경우에만 데이터를 삽입
     * 
     * @param event
     */
    @EventListener
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (isDatabaseEmpty()) {
            initializeData();
        }
    }

    private boolean isDatabaseEmpty() {
        return skillRepository.count() == 0;
    }

    private void initializeData() {
        // 스킬 데이터 나누어 삽입
        List<Skill> skillsPart1 = Arrays.asList(
                createSkill("1과목", "데이터 모델링의 이해", "데이터모델의 이해", 74, "모델링의 이해 설명"),
                createSkill("1과목", "데이터 모델링의 이해", "엔터티", 1, "엔터티 설명"),
                createSkill("1과목", "데이터 모델링의 이해", "속성", 57, "속성 설명"),
                createSkill("1과목", "데이터 모델링의 이해", "관계", 3, "관계 설명"),
                createSkill("1과목", "데이터 모델링의 이해", "식별자", 28, "식별자 설명"),
                createSkill("1과목", "데이터 모델과 SQL", "정규화", 41, "정규화 설명"),
                createSkill("1과목", "데이터 모델과 SQL", "관계와 조인의 이해", 6, "관계와 조인의 이해 설명"),
                createSkill("1과목", "데이터 모델과 SQL", "모델이 표현하는 트랜잭션의 이해", 7, "모델이 표현하는 트랜잭션의 이해 설명"),
                createSkill("1과목", "데이터 모델과 SQL", "Null 속성의 이해", 8, "Null 속성의 이해 설명"),
                createSkill("1과목", "데이터 모델과 SQL", "본질 식별자 vs 인조 식별자", 9, "본질 식별자 vs 인조 식별자 설명"));

        List<Skill> skillsPart2 = Arrays.asList(
                createSkill("2과목", "SQL 기본", "관계형 데이터 베이스 개요", 10, "관계형 데이터 베이스 개요 설명"),
                createSkill("2과목", "SQL 기본", "SELECT 문", 104, "SELECT 문 설명"),
                createSkill("2과목", "SQL 기본", "함수", 72, "함수 설명"),
                createSkill("2과목", "SQL 기본", "WHERE 절", 64, "WHERE 절 설명"),
                createSkill("2과목", "SQL 기본", "GROUP BY, HAVING 절", 14, "GROUP BY, HAVING 절 설명"),
                createSkill("2과목", "SQL 기본", "ORDER BY 절", 15, "ORDER BY 절 설명"),
                createSkill("2과목", "SQL 기본", "조인", 105, "조인 설명"),
                createSkill("2과목", "SQL 기본", "표준 조인", 16, "표준 조인 설명"),
                createSkill("2과목", "SQL 활용", "서브 쿼리", 56, "서브 쿼리 설명"),
                createSkill("2과목", "SQL 활용", "집합 연산자", 24, "집합 연산자 설명"));

        List<Skill> skillsPart3 = Arrays.asList(
                createSkill("2과목", "SQL 활용", "그룹 함수", 20, "그룹 함수 설명"),
                createSkill("2과목", "SQL 활용", "윈도우 함수", 24, "윈도우 함수 설명"),
                createSkill("2과목", "SQL 활용", "Top N 쿼리", 18, "Top N 쿼리 설명"),
                createSkill("2과목", "SQL 활용", "계층형 질의와 셀프 조인", 31, "계층형 질의와 셀프 조인 설명"),
                createSkill("2과목", "SQL 활용", "PIVOT 절과 UNPIVOT 절", 19, "PIVOT 절과 UNPIVOT 절 설명"),
                createSkill("2과목", "SQL 활용", "정규 표현식", 21, "정규 표현식 설명"),
                createSkill("2과목", "관리구문", "DML", 36, "DML 설명"),
                createSkill("2과목", "관리구문", "TCL", 17, "TCL 설명"),
                createSkill("2과목", "관리구문", "DDL", 29, "DDL 설명"),
                createSkill("2과목", "관리구문", "DCL", 22, "DCL 설명"));

        // 스킬 리스트 합치기
        List<Skill> skills = new ArrayList<>();
        skills.addAll(skillsPart1);
        skills.addAll(skillsPart2);
        skills.addAll(skillsPart3);

        // 스킬 데이터 삽입
        skillRepository.saveAll(skills);
    }

    private Skill createSkill(String title, String type, String keyConcepts, Integer frequency, String description) {
        Skill skill = new Skill();
        skill.setTitle(title);
        skill.setType(type);
        skill.setKeyConcepts(keyConcepts);
        skill.setFrequency(frequency);
        skill.setDescription(description);
        return skill;
    }
}
