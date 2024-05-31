package com.qriz.sqld.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.skillLevel.SkillLevel;
import com.qriz.sqld.domain.skillLevel.SkillLevelRepository;
import com.qriz.sqld.dto.recommend.RecommendRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RecommendationService {

    private final SkillLevelRepository skillLevelRepository;

    private final QuestionRepository questionRepository;

    public List<RecommendRespDto.DailyRespDto> recommendProblem(Long userId, int numRecommendations) {
        List<SkillLevel> skillLevels = skillLevelRepository.findByUserId(userId);

        if (skillLevels.isEmpty()) {
            throw new CustomApiException("해당 skill 데이터를 찾을 수 없습니다.");
        }

        // 최근 사용자가 풀이한 항목에 focus
        SkillLevel currentSkill = skillLevels.stream()
                .filter(skill -> skill.getPredictAccuracy() < 0.8)
                .findFirst()
                .orElse(null);

        if (currentSkill == null) {
            throw new CustomApiException("해당 사용자에게 향상이 필요한 skill 이 없습니다." + userId);
        }

        Long currentSkillId = currentSkill.getSkill().getId();

        // current skill 과 연관된 문제 불러오기
        List<Question> questions = questionRepository.findBySkillId(currentSkillId);

        // current accuracy 에 기반한 difficulty 값에 따른 question 분류
        List<Question> easyProblems = questions.stream()
                .filter(q -> q.getDifficulty() == 1)
                .collect(Collectors.toList());
        List<Question> mediumProblems = questions.stream()
                .filter(q -> q.getDifficulty() == 2)
                .collect(Collectors.toList());
        List<Question> hardProblems = questions.stream()
                .filter(q -> q.getDifficulty() == 3)
                .collect(Collectors.toList());

        List<Question> recommendations = new ArrayList<>();
        double currentAccuracy = currentSkill.getCurrentAccuracy();

        if (currentAccuracy > 0.7) {
            recommendations = getRandomQuestionIds(hardProblems, numRecommendations);
        } else if (currentAccuracy > 0.4) {
            recommendations = getRandomQuestionIds(mediumProblems, numRecommendations);
        } else {
            recommendations = getRandomQuestionIds(easyProblems, numRecommendations);
        }

        // 추천이 부족한 경우
        if (recommendations.size() < numRecommendations) {
            int remainingRecommendations = numRecommendations - recommendations.size();
            List<Question> allProblems = new ArrayList<>();
            allProblems.addAll(easyProblems);
            allProblems.addAll(mediumProblems);
            allProblems.addAll(hardProblems);
            recommendations.addAll(getRandomQuestionIds(allProblems, remainingRecommendations));
        }

        return recommendations.stream().map(RecommendRespDto.DailyRespDto::new).collect(Collectors.toList());
    }

    private List<Question> getRandomQuestionIds(List<Question> questions, int count) {
        Collections.shuffle(questions);
        return questions.stream()
                .distinct()
                .limit(count)
                .collect(Collectors.toList());
    }
}
