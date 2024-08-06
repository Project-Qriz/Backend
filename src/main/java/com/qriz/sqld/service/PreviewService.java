package com.qriz.sqld.service;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.preview.UserPreviewTest;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.skillLevel.SkillLevel;
import com.qriz.sqld.domain.skillLevel.SkillLevelRepository;
import com.qriz.sqld.domain.survey.Survey;
import com.qriz.sqld.domain.survey.SurveyRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.domain.userActivity.UserActivity;
import com.qriz.sqld.domain.userActivity.UserActivityRepository;
import com.qriz.sqld.dto.preview.QuestionDto;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.domain.preview.UserPreviewTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class PreviewService {

    private final QuestionRepository questionRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserPreviewTestRepository userPreviewTestRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final DailyPlanService dailyPlanService;
    private final SkillLevelRepository skillLevelRepository;
    private final SurveyRepository surveyRepository;

    public List<QuestionDto> getPreviewTestQuestions(User user) {
        // 이미 프리뷰 테스트를 완료했는지 확인
        if (userPreviewTestRepository.existsByUserAndCompleted(user, true)) {
            throw new RuntimeException("User has already completed the preview test");
        }

        List<Skill> selectedSkills = surveyRepository.findByUserAndCheckedTrue(user).stream()
                .map(Survey::getSkill)
                .collect(Collectors.toList());

        List<Question> questions;
        if (selectedSkills.isEmpty()) {
            // 아무것도 모른다 선택 (기존 로직)
            questions = getRandomPreviewQuestions(21);
        } else {
            questions = getPreviewQuestionsBasedOnSelection(selectedSkills);
        }

        // Question을 QuestionDto로 변환
        return questions.stream()
                .map(QuestionDto::from)
                .collect(Collectors.toList());
    }

    private List<Question> getPreviewQuestionsBasedOnSelection(List<Skill> selectedSkills) {
        List<Long> selectedSkillIds = selectedSkills.stream().map(Skill::getId).collect(Collectors.toList());
        List<Long> unselectedSkillIds = skillRepository.findAllSkillIdsNotIn(selectedSkillIds);

        int totalQuestions = 21;
        int selectedQuestionsCount = Math.min(10, totalQuestions * selectedSkills.size() / 30);
        int unselectedQuestionsCount = totalQuestions - selectedQuestionsCount;

        List<Question> selectedQuestions = new ArrayList<>();
        for (Long skillId : selectedSkillIds) {
            int questionsPerSkill = Math.max(1, selectedQuestionsCount / selectedSkillIds.size());
            selectedQuestions.addAll(questionRepository.findRandomQuestionsBySkillIdsAndCategoryOrderByFrequency(
                    Collections.singletonList(skillId), 1, questionsPerSkill));
        }

        List<Question> unselectedQuestions = new ArrayList<>();
        while (unselectedQuestions.size() < unselectedQuestionsCount) {
            for (Long skillId : unselectedSkillIds) {
                if (unselectedQuestions.size() >= unselectedQuestionsCount)
                    break;
                unselectedQuestions.addAll(questionRepository.findRandomQuestionsBySkillIdsAndCategoryOrderByFrequency(
                        Collections.singletonList(skillId), 1, 1));
            }
        }

        List<Question> allQuestions = new ArrayList<>(selectedQuestions);
        allQuestions.addAll(unselectedQuestions);

        while (allQuestions.size() > totalQuestions) {
            allQuestions.remove(allQuestions.size() - 1);
        }

        Collections.shuffle(allQuestions);

        return allQuestions;
    }

    private List<Question> getRandomPreviewQuestions(int totalQuestions) {
        List<Long> allSkillIds = skillRepository.findAll().stream().map(Skill::getId).collect(Collectors.toList());
        return questionRepository.findRandomQuestionsBySkillIdsAndCategoryOrderByFrequency(allSkillIds, 1,
                totalQuestions);
    }

    @Transactional
    public List<TestRespDto.TestSubmitRespDto> processPreviewResults(Long userId,
            List<TestReqDto.TestSubmitReqDto> activities) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        List<TestRespDto.TestSubmitRespDto> results = new ArrayList<>();

        Map<Long, List<TestReqDto.TestSubmitReqDto>> activityBySkill = activities.stream()
                .collect(Collectors
                        .groupingBy(activity -> questionRepository.findById(activity.getQuestion().getQuestionId())
                                .orElseThrow(() -> new RuntimeException("Question not found"))
                                .getSkill().getId()));

        for (Map.Entry<Long, List<TestReqDto.TestSubmitReqDto>> entry : activityBySkill.entrySet()) {
            Long skillId = entry.getKey();
            List<TestReqDto.TestSubmitReqDto> skillActivities = entry.getValue();

            Skill skill = skillRepository.findById(skillId).orElseThrow(() -> new RuntimeException("Skill not found"));

            UserPreviewTest userPreviewTest = new UserPreviewTest();
            userPreviewTest.setUser(user);
            userPreviewTest.setSkill(skill);
            userPreviewTest.setCompleted(true);
            userPreviewTest.setCompletionDate(LocalDate.now());
            userPreviewTestRepository.save(userPreviewTest);

            Map<Integer, Integer> difficultyTotalMap = new HashMap<>();
            Map<Integer, Integer> difficultyCorrectMap = new HashMap<>();

            for (TestReqDto.TestSubmitReqDto activity : skillActivities) {
                Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                        .orElseThrow(() -> new RuntimeException("Question not found"));

                UserActivity userActivity = new UserActivity();
                userActivity.setUser(user);
                userActivity.setQuestion(question);
                userActivity.setTestInfo("Preview Test");
                userActivity.setQuestionNum(activity.getQuestionNum());
                userActivity.setChecked(activity.getChecked());
                userActivity.setTimeSpent(activity.getTimeSpent());
                userActivity.setCorrection(question.getAnswer().equals(activity.getChecked()));
                userActivity.setDate(LocalDateTime.now());

                userActivity = userActivityRepository.save(userActivity);

                int difficulty = question.getDifficulty();
                difficultyTotalMap.merge(difficulty, 1, Integer::sum);
                if (userActivity.isCorrection()) {
                    difficultyCorrectMap.merge(difficulty, 1, Integer::sum);
                }

                TestRespDto.TestSubmitRespDto result = new TestRespDto.TestSubmitRespDto(
                        userActivity.getId(),
                        userId,
                        new TestRespDto.TestSubmitRespDto.QuestionRespDto(
                                question.getId(),
                                getCategoryName(question.getCategory())),
                        activity.getQuestionNum(),
                        activity.getChecked(),
                        activity.getTimeSpent(),
                        userActivity.isCorrection());

                results.add(result);
            }

            // 각 난이도별 SkillLevel 업데이트 또는 생성
            for (int difficulty : difficultyTotalMap.keySet()) {
                int total = difficultyTotalMap.get(difficulty);
                int correct = difficultyCorrectMap.getOrDefault(difficulty, 0);
                float accuracy = (float) correct / total;

                SkillLevel skillLevel = skillLevelRepository.findByUserAndSkillAndDifficulty(user, skill, difficulty)
                        .orElse(new SkillLevel(user, skill, difficulty));

                skillLevel.setCurrentAccuracy(accuracy);
                skillLevel.setLastUpdated(LocalDateTime.now());
                skillLevelRepository.save(skillLevel);
            }
        }

        // 30일 플랜 생성
        dailyPlanService.generateDailyPlan(userId);

        return results;
    }

    private String getCategoryName(int category) {
        switch (category) {
            case 1:
                return "PREVIEW";
            case 2:
                return "DAILY";
            case 3:
                return "EXAM";
            default:
                return "UNKNOWN";
        }
    }
}