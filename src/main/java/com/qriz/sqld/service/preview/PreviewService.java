package com.qriz.sqld.service.preview;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.UserActivity.UserActivity;
import com.qriz.sqld.domain.UserActivity.UserActivityRepository;
import com.qriz.sqld.domain.preview.UserPreviewTest;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.skillLevel.SkillLevel;
import com.qriz.sqld.domain.skillLevel.SkillLevelRepository;
import com.qriz.sqld.domain.survey.Survey;
import com.qriz.sqld.domain.survey.SurveyRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.preview.PreviewTestResult;
import com.qriz.sqld.dto.preview.QuestionDto;
import com.qriz.sqld.dto.preview.ResultDto;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.service.daily.DailyPlanService;
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
import java.util.Comparator;

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

    public PreviewTestResult getPreviewTestQuestions(User user) {
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
        List<QuestionDto> questionDtos = questions.stream()
                .map(QuestionDto::from)
                .collect(Collectors.toList());

        int totalTimeLimit = questions.stream()
                .mapToInt(Question::getTimeLimit)
                .sum();

        return new PreviewTestResult(questionDtos, totalTimeLimit);
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
    public void processPreviewResults(Long userId, List<TestReqDto.TestSubmitReqDto> activities) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

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

                // 체크되지 않은 문제도 처리
                boolean isCorrect = activity.getChecked() != null && question.getAnswer().equals(activity.getChecked());
                userActivity.setCorrection(isCorrect);

                // 정답일 경우 약 4.76점 부여 (100/21 ≈ 4.76), 체크되지 않은 경우 0점
                userActivity.setScore(isCorrect ? 100.0 / 21 : 0.0);

                userActivity.setDate(LocalDateTime.now());

                userActivityRepository.save(userActivity);

                int difficulty = question.getDifficulty();
                difficultyTotalMap.merge(difficulty, 1, Integer::sum);
                if (isCorrect) {
                    difficultyCorrectMap.merge(difficulty, 1, Integer::sum);
                }
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
    }

    public ResultDto.Response analyzePreviewTestResult(Long userId, String testInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfo(userId, testInfo);

        // 실전 예상 점수 계산
        double estimatedScore = calculateEstimatedScore(activities);

        // 점수 분석
        ResultDto.ScoreBreakdown scoreBreakdown = analyzeScore(activities);

        // 취약 영역 분석
        ResultDto.WeakAreaAnalysis weakAreaAnalysis = analyzeWeakAreas(activities);

        // 보충해야 할 개념 Top 2
        List<String> topConceptsToImprove = getTopConceptsToImprove(weakAreaAnalysis);

        return ResultDto.Response.builder()
                .scoreBreakdown(scoreBreakdown)
                .weakAreaAnalysis(weakAreaAnalysis)
                .topConceptsToImprove(topConceptsToImprove)
                .estimatedScore(estimatedScore)
                .build();
    }

    private int calculateEstimatedScore(List<UserActivity> activities) {
        Map<String, Double> weights = getWeights();
        Map<String, Integer> correctAnswers = new HashMap<>();
        Map<String, Integer> totalQuestions = new HashMap<>();

        for (UserActivity activity : activities) {
            String topic = activity.getQuestion().getSkill().getKeyConcepts();
            totalQuestions.put(topic, totalQuestions.getOrDefault(topic, 0) + 1);
            if (activity.isCorrection()) {
                correctAnswers.put(topic, correctAnswers.getOrDefault(topic, 0) + 1);
            }
        }

        double weightedScore = 0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String topic = entry.getKey();
            double weight = entry.getValue();
            int correct = correctAnswers.getOrDefault(topic, 0);
            int total = totalQuestions.getOrDefault(topic, 0);

            if (total > 0) {
                weightedScore += (correct / (double) total) * weight;
            }
        }

        // 100점 만점으로 환산하고 반올림하여 정수로 변환
        return (int) Math.round(weightedScore * 100);
    }

    private Map<String, Double> getWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("SELECT 문", 0.13);
        weights.put("조인", 0.13);
        weights.put("데이터모델의 이해", 0.09);
        weights.put("함수", 0.09);
        weights.put("WHERE 절", 0.08);
        weights.put("속성", 0.07);
        weights.put("서브 쿼리", 0.07);
        weights.put("정규화", 0.05);
        weights.put("DML", 0.04);
        weights.put("계층형 질의와 셀프 조인", 0.04);
        weights.put("DDL", 0.03);
        weights.put("식별자", 0.03);
        weights.put("집합 연산자", 0.03);
        weights.put("윈도우 함수", 0.03);
        weights.put("DCL", 0.02);
        weights.put("그룹 함수", 0.02);
        weights.put("ORDER BY 절", 0.02);
        weights.put("PIVOT 절과 UNPIVOT 절", 0.02);
        weights.put("Top N 쿼리", 0.02);
        weights.put("정규 표현식", 0.02);
        weights.put("TCL", 0.02);
        weights.put("GROUP BY, HAVING 절", 0.02);
        weights.put("표준 조인", 0.02);
        weights.put("관계형 데이터 베이스 개요", 0.01);
        weights.put("엔터티", 0.01);
        weights.put("관계", 0.01);
        weights.put("모델이 표현하는 트랜잭션의 이해", 0.01);
        weights.put("Null 속성의 이해", 0.01);
        weights.put("본질 식별자 vs 인조 식별자", 0.01);

        return weights;
    }

    private ResultDto.ScoreBreakdown analyzeScore(List<UserActivity> activities) {
        double part1Score = 0;
        double part2Score = 0;

        for (UserActivity activity : activities) {
            Double score = activity.getScore();
            if (score != null) {
                if (activity.getQuestion().getSkill().getTitle().equals("1과목")) {
                    part1Score += score;
                } else {
                    part2Score += score;
                }
            }
        }

        // part1과 part2의 점수를 정수로 반올림
        int part1ScoreAdjusted = (int) Math.round(part1Score);
        int part2ScoreAdjusted = (int) Math.round(part2Score);

        // totalScore는 part1과 part2의 합, 최대 100점
        int totalScoreAdjusted = Math.min(100, part1ScoreAdjusted + part2ScoreAdjusted);

        return ResultDto.ScoreBreakdown.builder()
                .totalScore(totalScoreAdjusted)
                .part1Score(part1ScoreAdjusted)
                .part2Score(part2ScoreAdjusted)
                .build();
    }

    private ResultDto.WeakAreaAnalysis analyzeWeakAreas(List<UserActivity> activities) {
        Map<String, Integer> incorrectCounts = new HashMap<>();

        for (UserActivity activity : activities) {
            if (!activity.isCorrection()) {
                String topic = activity.getQuestion().getSkill().getKeyConcepts();
                incorrectCounts.put(topic, incorrectCounts.getOrDefault(topic, 0) + 1);
            }
        }

        List<ResultDto.WeakArea> weakAreas = incorrectCounts.entrySet().stream()
                .map(entry -> ResultDto.WeakArea.builder()
                        .topic(entry.getKey())
                        .incorrectCount(entry.getValue())
                        .build())
                .sorted(Comparator.comparingInt(ResultDto.WeakArea::getIncorrectCount).reversed())
                .collect(Collectors.toList());

        return ResultDto.WeakAreaAnalysis.builder()
                .totalQuestions(activities.size())
                .weakAreas(weakAreas)
                .build();
    }

    private List<String> getTopConceptsToImprove(ResultDto.WeakAreaAnalysis weakAreaAnalysis) {
        List<String> topConcepts = new ArrayList<>();

        // 틀린 문제에서 추출한 개념들
        List<String> weakTopics = weakAreaAnalysis.getWeakAreas().stream()
                .map(ResultDto.WeakArea::getTopic)
                .collect(Collectors.toList());

        // 틀린 개념 먼저 추가
        topConcepts.addAll(weakTopics);

        // 만약 틀린 개념이 2개 미만이라면, 가중치가 높은 순으로 추가
        if (topConcepts.size() < 2) {
            List<String> highWeightTopics = getWeights().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .filter(topic -> !topConcepts.contains(topic))
                    .collect(Collectors.toList());

            // 필요한 만큼만 추가
            int needed = 2 - topConcepts.size();
            topConcepts.addAll(highWeightTopics.subList(0, needed));
        }

        return topConcepts;
    }
}