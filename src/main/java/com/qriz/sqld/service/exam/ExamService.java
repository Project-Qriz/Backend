package com.qriz.sqld.service.exam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.clip.ClipRepository;
import com.qriz.sqld.domain.clip.Clipped;
import com.qriz.sqld.domain.exam.UserExamSession;
import com.qriz.sqld.domain.exam.UserExamSessionRepository;
import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.domain.userActivity.UserActivity;
import com.qriz.sqld.domain.userActivity.UserActivityRepository;
import com.qriz.sqld.dto.exam.ExamTestResult;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final UserExamSessionRepository userExamSessionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final ClipRepository clipRepository;

    @Transactional(readOnly = true)
    public ExamTestResult getExamQuestionsBySession(Long userId, String session) {
        UserExamSession userExamSession = userExamSessionRepository.findByUserIdAndSession(userId, session)
                .orElseThrow(() -> new CustomApiException("해당 회차의 모의고사 세션을 찾을 수 없습니다."));

        if (userExamSession.isCompleted()) {
            throw new CustomApiException("이미 완료된 모의고사입니다.");
        }

        List<Question> examQuestions = questionRepository.findByCategoryAndExamSessionOrderById(3, session);

        if (examQuestions.isEmpty()) {
            throw new CustomApiException("해당 회차의 모의고사 문제를 찾을 수 없습니다.");
        }

        List<TestRespDto.ExamRespDto> questionDtos = examQuestions.stream()
                .map(this::convertToExamRespDto)
                .collect(Collectors.toList());

        int totalTimeLimit = examQuestions.stream()
                .mapToInt(Question::getTimeLimit)
                .sum();

        return new ExamTestResult(questionDtos, totalTimeLimit);
    }

    private TestRespDto.ExamRespDto convertToExamRespDto(Question question) {
        return new TestRespDto.ExamRespDto(
                question.getId(),
                question.getSkill().getId(),
                question.getCategory(),
                question.getQuestion(),
                question.getOption1(),
                question.getOption2(),
                question.getOption3(),
                question.getOption4(),
                question.getTimeLimit());
    }

    /**
     * 데일리 테스트 제출 처리
     * 
     * @param user             현재 사용자
     * @param testSubmitReqDto 테스트 제출 데이터
     * @return 테스트 제출 결과 목록
     */
    @Transactional
    public List<TestRespDto.TestSubmitRespDto> processExamSubmission(Long userId, String session,
            TestReqDto testSubmitReqDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));

        UserExamSession userExamSession = userExamSessionRepository.findByUserIdAndSession(userId, session)
                .orElseThrow(() -> new CustomApiException("해당 회차의 모의고사 세션을 찾을 수 없습니다."));

        if (userExamSession.isCompleted()) {
            throw new CustomApiException("이미 완료된 모의고사입니다.");
        }

        List<TestRespDto.TestSubmitRespDto> results = new ArrayList<>();
        int correctCount = 0;
        double totalScore = 0;

        for (TestReqDto.TestSubmitReqDto activity : testSubmitReqDto.getActivities()) {
            Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                    .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));

            UserActivity userActivity = new UserActivity();
            userActivity.setUser(user);
            userActivity.setQuestion(question);
            userActivity.setTestInfo(session);
            userActivity.setQuestionNum(activity.getQuestionNum());
            userActivity.setChecked(activity.getChecked());
            userActivity.setTimeSpent(activity.getTimeSpent());
            userActivity.setCorrection(question.getAnswer().equals(activity.getChecked()));
            userActivity.setDate(LocalDateTime.now());
            double score = calculateScore(activity, question);
            userActivity.setScore(score);
            totalScore += score;

            userActivityRepository.save(userActivity);

            if (userActivity.isCorrection()) {
                correctCount++;
            }

            // Clipped 엔티티 생성 및 저장
            Clipped clipped = new Clipped();
            clipped.setUserActivity(userActivity);
            clipped.setDate(LocalDateTime.now());
            clipRepository.save(clipped);

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

        // UserExamSession 업데이트
        userExamSession.setCompleted(true);
        userExamSession.setCompletedCount(userExamSession.getCompletedCount() + 1);
        userExamSession.setCompletionDate(LocalDateTime.now().toLocalDate());
        userExamSessionRepository.save(userExamSession);

        return results;
    }

    private double calculateScore(TestReqDto.TestSubmitReqDto activity, Question question) {
        return question.getAnswer().equals(activity.getChecked()) ? 2.5 : 0.0; // 40문제 기준, 맞으면 2.5점
    }

    /**
     * 카테고리 번호에 해당하는 카테고리 이름 반환
     * 
     * @param category 카테고리 번호
     * @return 카테고리 이름
     */
    private String getCategoryName(int category) {
        switch (category) {
            case 1:
                return "진단";
            case 2:
                return "데일리";
            case 3:
                return "모의고사";
            default:
                return "알 수 없음";
        }
    }
}
