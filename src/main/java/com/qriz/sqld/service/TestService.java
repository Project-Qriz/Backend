package com.qriz.sqld.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.skillLevel.SkillLevel;
import com.qriz.sqld.domain.skillLevel.SkillLevelRepository;
import com.qriz.sqld.domain.survey.Survey;
import com.qriz.sqld.domain.survey.SurveyRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.domain.userActivity.UserActivityRepository;
import com.qriz.sqld.domain.userActivity.UserActivity;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TestService {

    private final SkillLevelRepository skillLevelRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final SurveyRepository surveyRepository;
    private final Logger log = LoggerFactory.getLogger(TestService.class);

    /**
     * 데일리 문제 추천
     * 
     * @param userId
     * @param numRecommendations
     */
    public List<TestRespDto.DailyRespDto> recommendDaily(Long userId, int numRecommendations) {
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

        double easyAccuracy = getAccuracy(skillLevels, currentSkillId, 1);
        double mediumAccuracy = getAccuracy(skillLevels, currentSkillId, 2);

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

        if (easyAccuracy > 0.7 && mediumAccuracy > 0.7) {
            recommendations = getRandomQuestionIds(hardProblems, numRecommendations);
        } else if (easyAccuracy > 0.7) {
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

        return recommendations.stream().map(TestRespDto.DailyRespDto::new).collect(Collectors.toList());
    }

    private double getAccuracy(List<SkillLevel> skillLevels, Long skillId, int difficulty) {
        return skillLevels.stream()
                .filter(skill -> skill.getSkill().getId().equals(skillId) && skill.getDifficulty() == difficulty)
                .findFirst()
                .map(SkillLevel::getCurrentAccuracy)
                .orElse(0.0f);
    }

    private List<Question> getRandomQuestionIds(List<Question> questions, int count) {
        Collections.shuffle(questions);
        return questions.stream()
                .distinct()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * 데일리 문제 제출
     * 
     * @param userId
     * @param testSubmitReqDto
     */
    public List<TestRespDto.TestSubmitRespDto> processActivity(Long userId, TestReqDto testSubmitReqDto) {
        return testSubmitReqDto.getActivities().stream()
                .map(activity -> {
                    TestReqDto.TestSubmitReqDto.QuestionReqDto questionReqDto = activity.getQuestion();
                    Long questionId = questionReqDto.getQuestionId();
                    Question question = questionRepository.findById(questionId)
                            .orElseThrow(() -> new CustomApiException("해당 문제를 찾을 수 없습니다."));

                    boolean isCorrect = checkAnswer(question, activity.getChecked());

                    TestRespDto.TestSubmitRespDto.QuestionRespDto questionRespDto = new TestRespDto.TestSubmitRespDto.QuestionRespDto(
                            question.getId(), getCategoryName(questionReqDto.getCategory()));

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomApiException("해당 사용자를 찾을 수 없습니다."));

                    // UserActivity 엔티티 저장
                    UserActivity userActivity = new UserActivity();
                    userActivity.setUser(user);
                    userActivity.setQuestion(question);
                    userActivity.setChecked(activity.getChecked());
                    userActivity.setTimeSpent(activity.getTimeSpent());
                    userActivity.setCorrection(isCorrect);
                    userActivity.setDate(LocalDateTime.now());

                    // Set testInfo based on the current day
                    userActivity.setTestInfo(getNextDayInfo(userId));

                    userActivityRepository.save(userActivity);

                    return new TestRespDto.TestSubmitRespDto(activity.getActivityId(), activity.getUserId(),
                            questionRespDto,
                            activity.getQuestionNum(), activity.getChecked(), activity.getTimeSpent(), isCorrect);
                })
                .collect(Collectors.toList());
    }

    /**
     * 정답 여부 확인
     * 
     * @param question
     * @param checked
     */
    private boolean checkAnswer(Question question, String checked) {
        log.info("Checked: " + checked);
        log.info("Answer: " + question.getAnswer());
        boolean isCorrect = checked != null && checked.equals(question.getAnswer());
        log.info("Is correct: " + isCorrect);
        return isCorrect;
    }

    /**
     * 카테고리 값에 따른 분류
     * 
     * @param category
     */
    private String getCategoryName(int category) {
        switch (category) {
            case 1:
                return "진단고사";
            case 2:
                return "데일리";
            case 3:
                return "모의고사";
            default:
                return "Unknown";
        }
    }

    /**
     * 데일리 정보 불러오기
     * 
     * @param userId
     */
    private String getNextDayInfo(Long userId) {
        List<UserActivity> userActivities = userActivityRepository.findByUserId(userId);
        int maxDay = userActivities.stream()
                .filter(activity -> activity.getTestInfo().startsWith("Day "))
                .mapToInt(activity -> Integer.parseInt(activity.getTestInfo().substring(4)))
                .max()
                .orElse(0);
        return "Day " + (maxDay + 1);
    }

    /**
     * 데일리 결과 조회
     * 
     * @param userId
     * @param dayNumber
     * @return TestResultRespDto
     */
    public List<TestRespDto.TestResultRespDto> getDailyResults(Long userId, String dayNumber) {
        List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfo(userId, dayNumber);

        if (activities.isEmpty()) {
            throw new CustomApiException("해당 데일리 결과를 찾을 수 없습니다.");
        }

        return activities.stream()
                .map(activity -> new TestRespDto.TestResultRespDto(
                        activity.getId(),
                        activity.getUser().getId(),
                        new TestRespDto.TestResultRespDto.QuestionRespDto(
                                activity.getQuestion().getId(),
                                getCategoryName(activity.getQuestion().getCategory())),
                        activity.getQuestionNum(),
                        activity.isCorrection()))
                .collect(Collectors.toList());
    }

    /**
     * 데일리 결과 상세보기
     * @param userId
     * @param activityId
     * @return TestResultDetailRespDto
     */
    public TestRespDto.TestResultDetailRespDto getDailyResultDetail(Long userId, Long activityId) {
        UserActivity activity = userActivityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new CustomApiException("해당 활동을 찾을 수 없습니다."));

        Question question = activity.getQuestion();

        TestRespDto.TestResultDetailRespDto.QuestionDto questionDto = new TestRespDto.TestResultDetailRespDto.QuestionDto(
                question.getId(),
                question.getSkill().getId(),
                getCategoryName(question.getCategory()),
                question.getAnswer(),
                question.getSolution()
        );

        TestRespDto.TestResultDetailRespDto.ActivityDto activityDto = new TestRespDto.TestResultDetailRespDto.ActivityDto(
                activity.getId(),
                activity.getTestInfo()
        );

        return new TestRespDto.TestResultDetailRespDto(
                activityDto,
                activity.getUser().getId(),
                questionDto,
                activity.getQuestionNum(),
                activity.getChecked(),
                activity.isCorrection()
        );
    }

    /**
     * 진단 고사 문제 추천
     * 
     * @param userId
     * @param numRecommendations
     */
    public List<TestRespDto.DailyRespDto> recommendPreview(Long userId, int numRecommendations) {
        List<Survey> surveys = surveyRepository.findByUserIdAndCheckedFalse(userId);

        if (surveys.isEmpty()) {
            throw new CustomApiException("진단고사에 해당하는 체크하지 않은 개념이 없습니다.");
        }

        List<Long> uncheckedSkillIds = surveys.stream()
                .map(survey -> survey.getSkill().getId())
                .collect(Collectors.toList());

        List<Question> diagnosisProblems = questionRepository.findBySkillIdInAndCategory(uncheckedSkillIds, 1);

        // 최근 2년 시험 출제 경향에 따라 선별
        diagnosisProblems = prioritizeByRecentTrends(diagnosisProblems);

        return diagnosisProblems.stream()
                .limit(numRecommendations)
                .map(TestRespDto.DailyRespDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 최근 2년 시험 출제 경향에 따른 우선순위 지정
     * 
     * @param questions
     */
    private List<Question> prioritizeByRecentTrends(List<Question> questions) {
        return questions.stream()
                .sorted(Comparator.comparingInt((Question q) -> q.getSkill().getFrequency()).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 진단 고사 결과 처리
     * 
     * @param userId
     * @param testResults
     */
    public List<TestRespDto.TestSubmitRespDto> processPreviewResults(Long userId,
            List<TestReqDto.TestSubmitReqDto> testResults) {
        return testResults.stream()
                .map(result -> {
                    TestReqDto.TestSubmitReqDto.QuestionReqDto questionReqDto = result.getQuestion();
                    Long questionId = questionReqDto.getQuestionId();
                    Question question = questionRepository.findById(questionId)
                            .orElseThrow(() -> new CustomApiException("해당 문제를 찾을 수 없습니다."));

                    boolean isCorrect = checkAnswer(question, result.getChecked());

                    TestRespDto.TestSubmitRespDto.QuestionRespDto questionRespDto = new TestRespDto.TestSubmitRespDto.QuestionRespDto(
                            question.getId(),
                            getCategoryName(questionReqDto.getCategory()));

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomApiException("해당 사용자를 찾을 수 없습니다."));

                    // UserActivity 엔티티 저장
                    UserActivity userActivity = new UserActivity();
                    userActivity.setUser(user);
                    userActivity.setQuestion(question);
                    userActivity.setChecked(result.getChecked());
                    userActivity.setTimeSpent(result.getTimeSpent());
                    userActivity.setCorrection(isCorrect);
                    userActivity.setDate(LocalDateTime.now());
                    userActivityRepository.save(userActivity);

                    return new TestRespDto.TestSubmitRespDto(
                            result.getActivityId(),
                            userId,
                            questionRespDto,
                            result.getQuestionNum(),
                            result.getChecked(),
                            result.getTimeSpent(),
                            isCorrect);
                })
                .collect(Collectors.toList());
    }
}
