package com.qriz.sqld.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.skillLevel.SkillLevel;
import com.qriz.sqld.domain.skillLevel.SkillLevelRepository;
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
    private final Logger log = LoggerFactory.getLogger(TestService.class);

    // 데일리 문제 추천
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

        return recommendations.stream().map(TestRespDto.DailyRespDto::new).collect(Collectors.toList());
    }

    private List<Question> getRandomQuestionIds(List<Question> questions, int count) {
        Collections.shuffle(questions);
        return questions.stream()
                .distinct()
                .limit(count)
                .collect(Collectors.toList());
    }

    // 데일리 문제 제출
    public List<TestRespDto.TestSubmitRespDto> processActivity(Long userId, TestReqDto testSubmitReqDto) {
        return testSubmitReqDto.getActivities().stream()
                .map(activity -> {
                    TestReqDto.TestSubmitReqDto.QuestionReqDto questionReqDto = activity.getQuestion();
                    Long questionId = questionReqDto.getQuestionId();
                    Question question = questionRepository.findById(questionId)
                            .orElseThrow(() -> new CustomApiException("해당 문제를 찾을 수 없습니다."));

                    boolean isCorrect = checkAnswer(question, activity.getChecked());

                    TestRespDto.TestSubmitRespDto.QuestionRespDto questionRespDto = new TestRespDto.TestSubmitRespDto.QuestionRespDto(
                            question.getId(),
                            getCategoryName(questionReqDto.getCategory()));

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
                    userActivityRepository.save(userActivity);


                    return new TestRespDto.TestSubmitRespDto(
                            activity.getActivityId(),
                            activity.getUserId(),
                            questionRespDto,
                            activity.getQuestionNum(),
                            activity.getChecked(),
                            activity.getTimeSpent(),
                            isCorrect);
                })
                .collect(Collectors.toList());
    }

    private boolean checkAnswer(Question question, String checked) {
        log.info("Checked: " + checked);
        log.info("Answer: " + question.getAnswer());
        boolean isCorrect = checked != null && checked.equals(question.getAnswer());
        log.info("Is correct: " + isCorrect);
        return isCorrect;
    }

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
}
