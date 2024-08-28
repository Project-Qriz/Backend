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
     * ContextRefreshedEvent를 사용하여 애플리케이션 컨텍스트가 초기화될 때 데이터베이스 상태를 확인하고, 필요한 경우에만
     * 데이터를 삽입
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
                createSkill("1과목", "데이터 모델링의 이해", "데이터모델의 이해", 74, "데이터 모델의 개념, 구성요소 및 데이터 모델링 절차에 대한 기본적인 이해"),
                createSkill("1과목", "데이터 모델링의 이해", "엔터티", 1, "업무에 필요하고 유용한 정보를 저장하고 관리하기 위한 집합적인 것(thing)으로 서로 구별되는 객체"),
                createSkill("1과목", "데이터 모델링의 이해", "속성", 57, "업무에서 필요로 하는 인스턴스로 관리하고자 하는 의미상 더 이상 분리되지 않는 최소의 데이터 단위"),
                createSkill("1과목", "데이터 모델과 SQL", "관계와 조인의 이해", 6, "엔터티 간의 관계 유형과 SQL에서의 조인 연산에 대한 이해"),
                createSkill("1과목", "데이터 모델링의 이해", "식별자", 28, "엔터티를 유일하게 구분할 수 있는 속성 또는 속성의 집합"),
                createSkill("1과목", "데이터 모델과 SQL", "정규화", 41, "데이터의 중복을 최소화하고 데이터 무결성을 보장하기 위한 데이터베이스 설계 기법"),
                createSkill("1과목", "데이터 모델링의 이해", "관계", 3, "엔터티 간의 업무적 연관성 및 데이터 연관성"),
                createSkill("1과목", "데이터 모델과 SQL", "모델이 표현하는 트랜잭션의 이해", 7, "데이터 모델에서 표현되는 업무 규칙과 트랜잭션의 관계 이해"),
                createSkill("1과목", "데이터 모델과 SQL", "Null 속성의 이해", 8, "데이터베이스에서 Null 값의 의미와 처리 방법에 대한 이해"),
                createSkill("1과목", "데이터 모델과 SQL", "본질 식별자 vs 인조 식별자", 9,
                        "자연적으로 발생하는 식별자와 인위적으로 만들어진 식별자의 차이점과 사용 시 고려사항"));

        List<Skill> skillsPart2 = Arrays.asList(
                createSkill("2과목", "SQL 기본", "관계형 데이터 베이스 개요", 10, "관계형 데이터베이스의 기본 개념, 특징 및 구조에 대한 이해"),
                createSkill("2과목", "SQL 기본", "SELECT 문", 104, "데이터베이스에서 데이터를 조회하는 기본적인 SQL 명령문"),
                createSkill("2과목", "SQL 기본", "함수", 72, "SQL에서 사용되는 다양한 내장 함수들의 종류와 사용법"),
                createSkill("2과목", "SQL 기본", "WHERE 절", 64, "SQL 쿼리에서 조건을 지정하여 원하는 데이터만 필터링하는 방법"),
                createSkill("2과목", "SQL 기본", "GROUP BY, HAVING 절", 14, "데이터를 그룹화하고 그룹별 조건을 지정하는 SQL 구문"),
                createSkill("2과목", "SQL 기본", "ORDER BY 절", 15, "쿼리 결과를 정렬하는 SQL 구문"),
                createSkill("2과목", "SQL 기본", "조인", 105, "여러 테이블의 데이터를 연결하여 조회하는 SQL 기법"),
                createSkill("2과목", "SQL 기본", "표준 조인", 16, "ANSI SQL 표준에 따른 조인 구문과 그 사용법"),
                createSkill("2과목", "SQL 활용", "서브 쿼리", 56, "쿼리 내부에 포함된 또 다른 쿼리로, 복잡한 데이터 검색에 활용"),
                createSkill("2과목", "SQL 활용", "집합 연산자", 24, "여러 쿼리 결과를 하나로 결합하는 UNION, INTERSECT, MINUS 등의 연산자"));

        List<Skill> skillsPart3 = Arrays.asList(
                createSkill("2과목", "SQL 활용", "그룹 함수", 20, "데이터 그룹에 대한 계산을 수행하는 SQL 함수들"),
                createSkill("2과목", "SQL 활용", "윈도우 함수", 24, "행과 행 간의 관계를 정의하여 결과를 출력하는 함수"),
                createSkill("2과목", "SQL 활용", "Top N 쿼리", 18, "조건에 맞는 상위 N개의 레코드를 추출하는 쿼리 기법"),
                createSkill("2과목", "SQL 활용", "계층형 질의와 셀프 조인", 31, "계층적 데이터 구조를 다루는 SQL 기법과 같은 테이블 내에서의 조인"),
                createSkill("2과목", "SQL 활용", "PIVOT 절과 UNPIVOT 절", 19, "행 데이터를 열 데이터로, 열 데이터를 행 데이터로 변환하는 SQL 기법"),
                createSkill("2과목", "SQL 활용", "정규 표현식", 21, "문자열 패턴을 정의하고 검색, 치환하는 기법"),
                createSkill("2과목", "관리구문", "DML", 36, "데이터 조작어: INSERT, UPDATE, DELETE 등 데이터를 조작하는 SQL 명령어"),
                createSkill("2과목", "관리구문", "TCL", 17, "트랜잭션 제어어: COMMIT, ROLLBACK 등 트랜잭션을 제어하는 명령어"),
                createSkill("2과목", "관리구문", "DDL", 29, "데이터 정의어: CREATE, ALTER, DROP 등 데이터베이스 객체를 정의하는 SQL 명령어"),
                createSkill("2과목", "관리구문", "DCL", 22, "데이터 제어어: GRANT, REVOKE 등 데이터베이스 접근 권한을 제어하는 명령어"));

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
        skill.setImportanceLevel(getImportanceLevel(frequency));
        return skill;
    }

    private String getImportanceLevel(Integer frequency) {
        if (frequency >= 50) {
            return "상";
        } else if (frequency >= 30) {
            return "중";
        } else {
            return "하";
        }
    }
}
