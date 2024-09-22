package com.qriz.sqld.domain.question;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qriz.sqld.domain.skill.Skill;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

        /**
         * 모든 고유한 skillId를 조회
         * 
         * @return 고유한 skillId 목록
         */
        @Query("SELECT DISTINCT q.skill.id FROM Question q")
        List<Long> findAllSkillIds();

        /**
         * 특정 skillId 목록과 Category에 해당하는 질문들을 조회
         * 
         * @param skillIds 조회할 skillId 목록
         * @param category 조회할 카테고리
         * @return 조건에 맞는 Question 목록
         */
        List<Question> findBySkillIdInAndCategory(List<Long> skillIds, int category);

        /**
         * 특정 skillId와 Category에 해당하는 질문들을 조회
         * 
         * @param skillId  조회할 skillId
         * @param category 조회할 카테고리
         * @return 조건에 맞는 Question 목록
         */
        List<Question> findBySkillIdAndCategory(Long skillId, int category);

        /**
         * 특정 skillId, Category, 난이도에 해당하는 질문들을 조회
         * 
         * @param skillId    조회할 skillId
         * @param category   조회할 카테고리
         * @param difficulty 조회할 난이도
         * @return 조건에 맞는 Question 목록
         */
        List<Question> findBySkillIdAndCategoryAndDifficulty(Long skillId, int category, int difficulty);

        /**
         * 특정 skillId 목록과 Category에 해당하는 질문들 중 무작위로 지정된 개수만큼 조회
         * 
         * @param skillIds 조회할 skillId 목록
         * @param category 조회할 카테고리
         * @param limit    조회할 질문의 개수
         * @return 무작위로 선택된 Question 목록
         */
        @Query(value = "SELECT * FROM question q " +
                        "WHERE q.skill_id IN :skillIds " +
                        "AND q.category = :category " +
                        "ORDER BY RAND() " +
                        "LIMIT :limit", nativeQuery = true)
        List<Question> findRandomQuestionsBySkillIdsAndCategory(@Param("skillIds") List<Long> skillIds,
                        @Param("category") int category,
                        @Param("limit") int limit);

        /**
         * Skill 객체 목록과 Category를 받아 무작위로 지정된 개수만큼의 질문을 조회
         * 
         * @param skills   조회할 Skill 목록
         * @param category 조회할 카테고리
         * @param limit    조회할 질문의 개수
         * @return 무작위로 선택된 Question 목록
         */
        default List<Question> findRandomQuestionsBySkillsAndCategory(List<Skill> skills, int category, int limit) {
                List<Long> skillIds = skills.stream().map(Skill::getId).collect(Collectors.toList());
                return findRandomQuestionsBySkillIdsAndCategory(skillIds, category, limit);
        }

        @Query(value = "SELECT * FROM question WHERE skill_id = :skillId AND category = :category ORDER BY RAND() LIMIT :limit", nativeQuery = true)
        List<Question> findRandomQuestionsBySkillIdAndCategory(@Param("skillId") Long skillId,
                        @Param("category") int category, @Param("limit") int limit);

        @Query(value = "SELECT q.* FROM question q " +
                        "JOIN skill s ON q.skill_id = s.skill_id " +
                        "WHERE q.skill_id IN :skillIds AND q.category = :category " +
                        "ORDER BY s.frequency DESC, RAND() " +
                        "LIMIT :limit", nativeQuery = true)
        List<Question> findRandomQuestionsBySkillIdsAndCategoryOrderByFrequency(
                        @Param("skillIds") List<Long> skillIds,
                        @Param("category") int category,
                        @Param("limit") int limit);

        /**
         * 카테고리와 세션으로 문제들을 조회하는 메소드
         *
         * @param category 문제 카테고리 (3: exam)
         * @param session  세션 (예: "1회차", "2회차")
         * @return 해당하는 Question 리스트
         */
        List<Question> findByCategoryAndExamSessionOrderById(int category, String examSession);
}