package com.qriz.sqld.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.preview.UserPreviewTest;
import com.qriz.sqld.domain.preview.UserPreviewTestRepository;
import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.survey.Survey;
import com.qriz.sqld.domain.survey.SurveyRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.domain.userActivity.UserActivity;
import com.qriz.sqld.domain.userActivity.UserActivityRepository;
import com.qriz.sqld.dto.recommend.WeeklyRecommendationDto;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserPreviewTestRepository userPreviewTestRepository;
    private final UserActivityRepository userActivityRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public WeeklyRecommendationDto getWeeklyRecommendation(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Survey> knowsNothingSurvey = surveyRepository.findByUserAndKnowsNothingTrue(user);

        if (knowsNothingSurvey.isPresent()) {
            return getRecommendationsWithoutPreview();
        } else {
            Optional<UserPreviewTest> completedPreviewTest = userPreviewTestRepository
                    .findFirstByUserAndCompletedOrderByCompletionDateDesc(user, true);

            if (completedPreviewTest.isPresent()) {
                return getRecommendationsWithPreview(userId);
            } else {
                return WeeklyRecommendationDto.builder()
                        .recommendationType("프리뷰 테스트 미완료")
                        .recommendations(Collections.emptyList())
                        .build();
            }
        }
    }

    private WeeklyRecommendationDto getRecommendationsWithPreview(Long userId) {
        List<UserActivity> previewActivities = userActivityRepository.findByUserIdAndTestInfo(userId, "Preview Test");

        Map<Long, SkillStats> skillStatsMap = new HashMap<>();

        for (UserActivity activity : previewActivities) {
            Question question = questionRepository.findById(activity.getQuestion().getId())
                    .orElseThrow(() -> new RuntimeException("Question not found"));
            Skill skill = question.getSkill();

            SkillStats stats = skillStatsMap.computeIfAbsent(skill.getId(), k -> new SkillStats(skill));
            stats.totalQuestions++;
            if (!activity.isCorrection()) {
                stats.incorrectAnswers++;
            }
        }

        List<SkillStats> sortedStats = new ArrayList<>(skillStatsMap.values());
        sortedStats.sort((a, b) -> {
            if (a.getIncorrectRate() != b.getIncorrectRate()) {
                return Double.compare(b.getIncorrectRate(), a.getIncorrectRate());
            }
            return Integer.compare(b.getSkill().getFrequency(), a.getSkill().getFrequency());
        });

        List<WeeklyRecommendationDto.ConceptRecommendation> recommendations = sortedStats.stream()
                .limit(2)
                .map(stats -> WeeklyRecommendationDto.ConceptRecommendation.builder()
                        .skillId(stats.getSkill().getId())
                        .keyConcepts(stats.getSkill().getKeyConcepts())
                        .description(stats.getSkill().getDescription())
                        .frequency(stats.getSkill().getFrequency())
                        .incorrectRate(stats.getIncorrectRate())
                        .build())
                .collect(Collectors.toList());

        return WeeklyRecommendationDto.builder()
                .recommendationType("주간 맞춤 개념")
                .recommendations(recommendations)
                .build();
    }

    private WeeklyRecommendationDto getRecommendationsWithoutPreview() {
        List<Skill> topFrequencySkills = skillRepository.findTop2ByOrderByFrequencyDesc();

        List<WeeklyRecommendationDto.ConceptRecommendation> recommendations = topFrequencySkills.stream()
                .map(skill -> WeeklyRecommendationDto.ConceptRecommendation.builder()
                        .skillId(skill.getId())
                        .keyConcepts(skill.getKeyConcepts())
                        .description(skill.getDescription())
                        .frequency(skill.getFrequency())
                        .build())
                .collect(Collectors.toList());

        return WeeklyRecommendationDto.builder()
                .recommendationType("주간 추천 개념")
                .recommendations(recommendations)
                .build();
    }

    private static class SkillStats {
        private final Skill skill;
        private int totalQuestions;
        private int incorrectAnswers;

        public SkillStats(Skill skill) {
            this.skill = skill;
            this.totalQuestions = 0;
            this.incorrectAnswers = 0;
        }

        public double getIncorrectRate() {
            return totalQuestions > 0 ? (double) incorrectAnswers / totalQuestions : 0;
        }

        public Skill getSkill() {
            return skill;
        }
    }
}
