package com.qriz.sqld.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.skillLevel.SkillLevel;
import com.qriz.sqld.domain.skillLevel.SkillLevelRepository;
import com.qriz.sqld.domain.userActivity.UserActivity;
import com.qriz.sqld.domain.userActivity.UserActivityRepository;
import com.qriz.sqld.service.SurveyService;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class WeekendPlanUtil {

    private final QuestionRepository questionRepository;
    private final UserActivityRepository userActivityRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final UserDailyRepository userDailyRepository;
    private final SkillRepository skillRepository;

    @Lazy
    @Autowired
    private SurveyService surveyService;

    public List<Skill> getWeekendPlannedSkills(Long userId, UserDaily weekendDay) {
        List<Question> weekendQuestions = getWeekendQuestions(userId, weekendDay);
        return weekendQuestions.stream()
                .map(Question::getSkill)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Question> getWeekendQuestions(Long userId, UserDaily todayPlan) {
        List<Question> weakConceptQuestions = getWeakConceptQuestionsForWeek(userId, todayPlan);

        boolean isKnowsNothingUser = surveyService.isKnowsNothingUser(userId);
        if (!isKnowsNothingUser) {
            List<Question> previewTestWeakQuestions = getPreviewTestWeakQuestions(userId);
            weakConceptQuestions.addAll(previewTestWeakQuestions);
        }

        List<Question> finalQuestions = weakConceptQuestions.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());

        if (finalQuestions.size() < 10) {
            List<Question> additionalQuestions = getAdditionalWeakConceptQuestions(userId, todayPlan,
                    10 - finalQuestions.size());
            finalQuestions.addAll(additionalQuestions);
        }

        return finalQuestions;
    }

    private List<Question> getWeakConceptQuestionsForWeek(Long userId, UserDaily todayPlan) {
        int currentDay = Integer.parseInt(todayPlan.getDayNumber().replace("Day", ""));
        int startDay = Math.max(1, currentDay - 5); // Day1 미만으로 내려가지 않도록

        List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfoBetween(
                userId,
                "데일리 테스트 - Day" + startDay,
                "데일리 테스트 - Day" + (currentDay - 1));

        Map<Long, SkillAccuracy> skillAccuracyMap = new HashMap<>();

        for (UserActivity activity : activities) {
            Long skillId = activity.getQuestion().getSkill().getId();
            skillAccuracyMap.computeIfAbsent(skillId, k -> new SkillAccuracy())
                    .addResult(activity.isCorrection());
        }

        List<Long> weakSkillIds = skillAccuracyMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> e.getValue().getAccuracy()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (weakSkillIds.isEmpty()) {
            // 만약 이번 주에 푼 문제가 없다면, 모든 스킬에서 선택
            weakSkillIds = skillRepository.findAllIds();
        }

        return questionRepository.findRandomQuestionsBySkillIdsAndCategory(weakSkillIds, 2, 5); // 데일리 카테고리, 5문제
    }

    private static class SkillAccuracy {
        private int correct = 0;
        private int total = 0;

        public void addResult(boolean isCorrect) {
            if (isCorrect)
                correct++;
            total++;
        }

        public double getAccuracy() {
            return total == 0 ? 0 : (double) correct / total;
        }
    }

    private List<Question> getPreviewTestWeakQuestions(Long userId) {
        List<SkillLevel> weakSkillLevels = skillLevelRepository.findTop3ByUserIdOrderByCurrentAccuracyAsc(userId);
        List<Long> weakSkillIds = weakSkillLevels.stream()
                .map(skillLevel -> skillLevel.getSkill().getId())
                .collect(Collectors.toList());

        return questionRepository.findRandomQuestionsBySkillIdsAndCategory(weakSkillIds, 2, 5); // 데일리 카테고리, 5문제
    }

    private List<Question> getAdditionalWeakConceptQuestions(Long userId, UserDaily todayPlan, int count) {
        int currentDay = Integer.parseInt(todayPlan.getDayNumber().replace("Day", ""));
        int startDay = Math.max(1, currentDay - 5); // Day1 미만으로 내려가지 않도록

        List<UserDaily> previousDays = userDailyRepository.findByUserIdAndDayNumberBetween(
                userId, "Day" + startDay, "Day" + (currentDay - 1));

        Map<Long, SkillAccuracy> skillAccuracyMap = new HashMap<>();

        for (UserDaily daily : previousDays) {
            if (daily.isCompleted()) {
                List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfo(userId,
                        "데일리 테스트 - " + daily.getDayNumber());
                for (UserActivity activity : activities) {
                    Long skillId = activity.getQuestion().getSkill().getId();
                    skillAccuracyMap.computeIfAbsent(skillId, k -> new SkillAccuracy())
                            .addResult(activity.isCorrection());
                }
            }
        }

        List<Long> weakSkillIds = skillAccuracyMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> e.getValue().getAccuracy()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 약점 스킬이 충분하지 않은 경우, 모든 스킬을 포함
        if (weakSkillIds.size() < count) {
            List<Long> allSkillIds = skillRepository.findAllIds();
            for (Long skillId : allSkillIds) {
                if (!weakSkillIds.contains(skillId)) {
                    weakSkillIds.add(skillId);
                }
            }
        }

        // 요청된 수만큼의 문제를 가져오기
        List<Question> additionalQuestions = new ArrayList<>();
        int questionsPerSkill = Math.max(1, count / weakSkillIds.size());
        int remainingQuestions = count;

        for (Long skillId : weakSkillIds) {
            if (remainingQuestions <= 0)
                break;

            int questionsToFetch = Math.min(questionsPerSkill, remainingQuestions);
            List<Question> questions = questionRepository.findRandomQuestionsBySkillIdsAndCategory(
                    Collections.singletonList(skillId), 2, questionsToFetch);

            additionalQuestions.addAll(questions);
            remainingQuestions -= questions.size();
        }

        // 아직 부족한 경우 랜덤으로 추가
        if (remainingQuestions > 0) {
            additionalQuestions.addAll(getRandomQuestions(remainingQuestions));
        }

        return additionalQuestions;
    }

    private List<Question> getRandomQuestions(int count) {
        List<Long> allSkillIds = skillRepository.findAllIds();
        return questionRepository.findRandomQuestionsBySkillIdsAndCategory(allSkillIds, 2, count);
    }

    // getWeakConceptQuestionsForWeek, getPreviewTestWeakQuestions, getAdditionalWeakConceptQuestions 메소드들도 여기에 구현
    // ...
}