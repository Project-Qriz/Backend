package com.qriz.sqld.service.exam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.UserActivity.UserActivity;
import com.qriz.sqld.domain.UserActivity.UserActivityRepository;
import com.qriz.sqld.domain.clip.ClipRepository;
import com.qriz.sqld.domain.clip.Clipped;
import com.qriz.sqld.domain.exam.UserExamSession;
import com.qriz.sqld.domain.exam.UserExamSessionRepository;
import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.daily.ResultDetailDto;
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

    private final Logger log = LoggerFactory.getLogger(ExamService.class);

    @Transactional(readOnly = true)
    public ExamTestResult getExamQuestionsBySession(Long userId, String session) {
        // 1. 해당 회차의 세션이 있는지 확인하고 없으면 생성
        UserExamSession userExamSession = userExamSessionRepository
                .findByUserIdAndSession(userId, session)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));
                    UserExamSession newSession = new UserExamSession();
                    newSession.setUser(user);
                    newSession.setSession(session);
                    newSession.setCompleted(false);
                    newSession.setCompletedCount(0);
                    return userExamSessionRepository.save(newSession);
                });

        // 2. 해당 회차의 문제들을 불러옴 (category=3은 모의고사를 의미)
        List<Question> examQuestions = questionRepository.findByCategoryAndExamSessionOrderById(3, session);

        if (examQuestions.isEmpty()) {
            throw new CustomApiException("해당 회차의 모의고사 문제를 찾을 수 없습니다.");
        }

        // 3. 문제들을 DTO로 변환
        List<TestRespDto.ExamRespDto> questionDtos = examQuestions.stream()
                .map(question -> new TestRespDto.ExamRespDto(
                        question.getId(),
                        question.getSkill().getId(),
                        question.getCategory(),
                        question.getQuestion(),
                        question.getOption1(),
                        question.getOption2(),
                        question.getOption3(),
                        question.getOption4(),
                        question.getTimeLimit()))
                .collect(Collectors.toList());

        // 4. 총 제한 시간 계산
        int totalTimeLimit = examQuestions.stream()
                .mapToInt(Question::getTimeLimit)
                .sum();

        return new ExamTestResult(questionDtos, totalTimeLimit);
    }

    /**
     * 모의고사 제출 처리
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

    /**
     * 오늘의 공부 결과 - 문제 상세보기
     * 
     * @param userId
     * @param session
     * @param questionId
     * @return
     */
    @Transactional(readOnly = true)
    public ResultDetailDto getExamResultDetail(Long userId, String session, Long questionId) {
        log.info("Getting daily result detail for userId: {}, session: {}, questionId: {}", userId, session,
                questionId);

        String testInfo = session;
        log.info("Constructed testInfo: {}", testInfo);

        UserActivity userActivity = userActivityRepository
                .findByUserIdAndTestInfoAndQuestionId(userId, testInfo, questionId)
                .orElseThrow(() -> {
                    log.error("UserActivity not found for userId: {}, testInfo: {}, questionId: {}", userId, testInfo,
                            questionId);
                    return new CustomApiException("해당 문제의 풀이 결과를 찾을 수 없습니다.");
                });

        log.info("UserActivity found: {}", userActivity);

        Question question = userActivity.getQuestion();
        Skill skill = question.getSkill();

        ResultDetailDto result = ResultDetailDto.builder()
                .skillName(skill.getKeyConcepts())
                .question(question.getQuestion())
                .option1(question.getOption1())
                .option2(question.getOption2())
                .option3(question.getOption3())
                .option4(question.getOption4())
                .answer(question.getAnswer())
                .solution(question.getSolution())
                .checked(userActivity.getChecked())
                .correction(userActivity.isCorrection())
                .build();

        log.info("ExamResultDetailDto created: {}", result);

        return result;
    }

    /**
     * 특정 회차의 테스트 결과 점수
     * 
     * @param userId
     * @param session
     * @return
     */
    @Transactional(readOnly = true)
    public ExamTestResult.Response getExamSubjectDetails(Long userId, String session) {
        log.info("Starting getDaySubjectDetails for userId: {} and session: {}", userId, session);

        UserExamSession userExamSession = userExamSessionRepository.findByUserIdAndSession(userId, session)
                .orElseThrow(() -> new CustomApiException("해당 회차의 모의고사 세션을 찾을 수 없습니다."));

        if (!userExamSession.isCompleted()) {
            throw new CustomApiException("해당 회차의 모의고사가 완료되지 않았습니다.");
        }

        List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfoOrderByQuestionNumAsc(userId,
                session);

        if (activities.isEmpty()) {
            throw new CustomApiException("해당 회차에 대한 활동 기록이 없습니다.");
        }

        Map<String, ExamTestResult.SubjectDetails> subjectDetailsMap = new HashMap<>();
        List<ExamTestResult.ResultDto> subjectResultsList = new ArrayList<>();

        for (UserActivity activity : activities) {
            Question question = activity.getQuestion();
            Skill skill = question.getSkill();

            String subjectTitle = skill.getTitle(); // 직접 Skill의 title 사용
            ExamTestResult.SubjectDetails subjectDetails = subjectDetailsMap.computeIfAbsent(subjectTitle,
                    k -> new ExamTestResult.SubjectDetails(subjectTitle));

            subjectDetails.addScore(skill.getKeyConcepts(), activity.getScore());

            subjectResultsList.add(new ExamTestResult.ResultDto(
                    skill.getKeyConcepts(),
                    question.getQuestion(),
                    activity.isCorrection()));
        }

        subjectDetailsMap.values().forEach(ExamTestResult.SubjectDetails::adjustTotalScore);

        List<ExamTestResult.SubjectDetails> userExamInfoList = new ArrayList<>(subjectDetailsMap.values());

        log.info("Completed processing for getDaySubjectDetails");
        return new ExamTestResult.Response(session, userExamInfoList, subjectResultsList);
    }
}
