package com.qriz.sqld.service.exam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.qriz.sqld.dto.exam.ExamReqDto;
import com.qriz.sqld.dto.exam.ExamRespDto;
import com.qriz.sqld.dto.exam.ExamRespDto.SessionList;
import com.qriz.sqld.dto.exam.ExamTestResult;
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
                // 해당 회차의 문제들을 불러옴 (category=3은 모의고사를 의미)
                List<Question> examQuestions = questionRepository.findByCategoryAndExamSessionOrderById(3, session);

                if (examQuestions.isEmpty()) {
                        throw new CustomApiException("해당 회차의 모의고사 문제를 찾을 수 없습니다.");
                }

                // 문제들을 DTO로 변환
                List<TestRespDto.ExamRespDto> questionDtos = examQuestions.stream()
                                .map(TestRespDto.ExamRespDto::new)
                                .collect(Collectors.toList());

                // 총 제한 시간 계산
                int totalTimeLimit = 5400;

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
        public List<TestRespDto.ExamSubmitRespDto> processExamSubmission(Long userId, String session,
                        ExamReqDto examSubmitReqDto) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));

                // 오늘 날짜의 세션만 삭제
                LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime tomorrow = today.plusDays(1);

                List<UserExamSession> todaySessions = userExamSessionRepository
                                .findByUserIdAndSessionAndCompletionDateBetween(userId, session, today, tomorrow);

                for (UserExamSession previousSession : todaySessions) {
                        // 관련된 UserActivity와 Clipped 엔티티도 함께 삭제
                        List<UserActivity> activities = userActivityRepository.findByExamSession(previousSession);
                        for (UserActivity activity : activities) {
                                clipRepository.deleteByUserActivity(activity);
                        }
                        userActivityRepository.deleteByExamSession(previousSession);
                        userExamSessionRepository.delete(previousSession);
                }

                // 새로운 세션 생성
                UserExamSession userExamSession = UserExamSession.builder()
                                .user(user)
                                .session(session)
                                .attemptCount(1)
                                .completionDate(LocalDateTime.now())
                                .build();

                userExamSessionRepository.save(userExamSession);

                List<TestRespDto.ExamSubmitRespDto> results = new ArrayList<>();
                int correctCount = 0;
                double totalScore = 0;

                for (ExamReqDto.ExamSubmitReqDto activity : examSubmitReqDto.getActivities()) {
                        Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                                        .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));

                        UserActivity userActivity = new UserActivity();
                        userActivity.setUser(user);
                        userActivity.setQuestion(question);
                        userActivity.setTestInfo(session);
                        userActivity.setQuestionNum(activity.getQuestionNum());
                        userActivity.setChecked(activity.getChecked());
                        userActivity.setCorrection(question.getAnswer().equals(activity.getChecked()));
                        userActivity.setDate(LocalDateTime.now());
                        double score = calculateScore(activity, question);
                        userActivity.setScore(score);
                        userActivity.setExamSession(userExamSession);
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

                        TestRespDto.ExamSubmitRespDto result = new TestRespDto.ExamSubmitRespDto(
                                        userActivity.getId(),
                                        userId,
                                        new TestRespDto.ExamSubmitRespDto.QuestionRespDto(
                                                        question.getId(),
                                                        getCategoryName(question.getCategory())),
                                        activity.getQuestionNum(),
                                        activity.getChecked(),
                                        userActivity.isCorrection());

                        results.add(result);
                }

                // 과목별 점수 계산 후 저장
                Map<String, Double> subjectScores = calculateSubjectScores(examSubmitReqDto.getActivities());
                userExamSession.setSubject1Score(subjectScores.getOrDefault("1과목", 0.0));
                userExamSession.setSubject2Score(subjectScores.getOrDefault("2과목", 0.0));

                userExamSessionRepository.save(userExamSession);

                return results;
        }

        private Map<String, Double> calculateSubjectScores(List<ExamReqDto.ExamSubmitReqDto> activities) {
                Map<String, Double> subjectScores = new HashMap<>();

                for (ExamReqDto.ExamSubmitReqDto activity : activities) {
                        Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                                        .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));

                        double score = calculateScore(activity, question);
                        String title = question.getSkill().getTitle(); // "1과목" 또는 "2과목"
                        subjectScores.merge(title, score, Double::sum);
                }

                return subjectScores;
        }

        private double calculateScore(ExamReqDto.ExamSubmitReqDto activity, Question question) {
                return question.getAnswer().equals(activity.getChecked()) ? 2 : 0.0; // 50문제 기준, 맞으면 2점
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
                                        log.error("UserActivity not found for userId: {}, testInfo: {}, questionId: {}",
                                                        userId, testInfo,
                                                        questionId);
                                        return new CustomApiException("해당 문제의 풀이 결과를 찾을 수 없습니다.");
                                });

                log.info("UserActivity found: {}", userActivity);

                Question question = userActivity.getQuestion();
                Skill skill = question.getSkill();

                ResultDetailDto result = ResultDetailDto.builder()
                                .skillName(skill.getKeyConcepts())
                                .question(question.getQuestion())
                                .question(question.getDescription())
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
                // 해당 회차의 모든 세션을 시간순으로 조회 (내림차순)
                List<UserExamSession> userExamSessions = userExamSessionRepository
                                .findByUserIdAndSessionOrderByCompletionDateDesc(userId, session);

                if (userExamSessions.isEmpty()) {
                        throw new CustomApiException("해당 회차의 모의고사 세션을 찾을 수 없습니다.");
                }

                // 가장 최근 세션
                UserExamSession latestSession = userExamSessions.get(0);

                // 최신 세션의 과목별 점수로 userExamInfoList 생성
                List<ExamTestResult.SubjectDetails> userExamInfoList = new ArrayList<>();

                // 1과목 details 생성
                ExamTestResult.SubjectDetails subject1Details = new ExamTestResult.SubjectDetails("1과목");
                Map<String, Double> subject1TypeScores = new HashMap<>();

                // 2과목 details 생성
                ExamTestResult.SubjectDetails subject2Details = new ExamTestResult.SubjectDetails("2과목");
                Map<String, Double> subject2TypeScores = new HashMap<>();

                // 최신 세션의 활동 조회
                List<UserActivity> latestActivities = userActivityRepository.findByExamSession(latestSession);

                // 각 활동의 점수를 해당 과목의 type별로 집계
                for (UserActivity activity : latestActivities) {
                        Question question = activity.getQuestion();
                        Skill skill = question.getSkill();

                        if (skill.getTitle().equals("1과목")) {
                                subject1TypeScores.merge(skill.getKeyConcepts(), activity.getScore(), Double::sum);
                        } else if (skill.getTitle().equals("2과목")) {
                                subject2TypeScores.merge(skill.getKeyConcepts(), activity.getScore(), Double::sum);
                        }
                }

                // 집계된 점수를 SubjectDetails에 추가
                subject1TypeScores.forEach((type, score) -> subject1Details.addScore(type, score));
                subject2TypeScores.forEach((type, score) -> subject2Details.addScore(type, score));

                userExamInfoList.add(subject1Details);
                userExamInfoList.add(subject2Details);

                // 문제 풀이 결과 목록 생성
                List<ExamTestResult.ResultDto> subjectResultsList = latestActivities.stream()
                                .map(activity -> new ExamTestResult.ResultDto(
                                                activity.getQuestionNum(),
                                                activity.getQuestion().getSkill().getKeyConcepts(),
                                                activity.getQuestion().getQuestion(),
                                                activity.isCorrection()))
                                .sorted(Comparator.comparingInt(ExamTestResult.ResultDto::getQuestionNum))
                                .collect(Collectors.toList());

                // 날짜별로 그룹화하여 각 날짜의 최신 기록만 유지
                Map<LocalDate, UserExamSession> dateGroupedSessions = userExamSessions.stream()
                                .filter(s -> s.getCompletionDate() != null)
                                .collect(Collectors.groupingBy(
                                                s -> s.getCompletionDate().toLocalDate(),
                                                Collectors.collectingAndThen(
                                                                Collectors.minBy(Comparator.comparing(
                                                                                UserExamSession::getCompletionDate)
                                                                                .reversed()),
                                                                Optional::get)));

                // 날짜별로 그룹화하여 각 날짜의 최신 기록만 유지하고, 최신 5개만 선택
                List<ExamTestResult.HistoricalScore> historicalScores = new ArrayList<>();
                if (dateGroupedSessions.size() >= 2) {
                        historicalScores = dateGroupedSessions.values().stream()
                                        .sorted(Comparator.comparing(UserExamSession::getCompletionDate).reversed())
                                        .limit(5) // 최신 5개만 선택
                                        .map(examSession -> {
                                                List<ExamTestResult.ItemScore> sessionScores = new ArrayList<>();
                                                if (examSession.getSubject1Score() != null) {
                                                        sessionScores.add(new ExamTestResult.ItemScore("1과목",
                                                                        examSession.getSubject1Score()));
                                                }
                                                if (examSession.getSubject2Score() != null) {
                                                        sessionScores.add(new ExamTestResult.ItemScore("2과목",
                                                                        examSession.getSubject2Score()));
                                                }

                                                return new ExamTestResult.HistoricalScore(
                                                                examSession.getCompletionDate(),
                                                                sessionScores,
                                                                dateGroupedSessions.size());
                                        })
                                        .collect(Collectors.toList());
                }

                return new ExamTestResult.Response(session, userExamInfoList, subjectResultsList, historicalScores);
        }

        @Transactional
        public List<ExamRespDto.SessionList> getSessionList(Long userId, String status, String sort) {
                // 1. 모든 모의고사 회차 정보 조회
                List<String> allSessions = questionRepository.findDistinctExamSessionByCategory(3);

                // 2. 사용자가 완료한 시험 세션 정보 조회 및 Map으로 변환
                List<UserExamSession> userSessions = userExamSessionRepository
                                .findByUserIdOrderByCompletionDateDesc(userId);
                Map<String, UserExamSession> completedSessionsMap = userSessions.stream()
                                .collect(Collectors.toMap(
                                                UserExamSession::getSession,
                                                session -> session,
                                                (existing, replacement) -> existing));

                // 3. 각 회차별 정보 생성 및 필터링
                Stream<ExamRespDto.SessionList> sessionStream = allSessions.stream()
                                .map(session -> {
                                        UserExamSession userSession = completedSessionsMap.get(session);
                                        boolean completed = userSession != null;
                                        String totalScore = null;
                                        if (completed) {
                                                double score = userSession.getSubject1Score()
                                                                + userSession.getSubject2Score();
                                                totalScore = String.format("%.1f", score);
                                        }

                                        return new ExamRespDto.SessionList(
                                                        completed,
                                                        session,
                                                        totalScore);
                                });

                // 4. 학습 상태에 따른 필터링
                if ("completed".equals(status)) {
                        sessionStream = sessionStream.filter(ExamRespDto.SessionList::isCompleted);
                } else if ("incomplete".equals(status)) {
                        sessionStream = sessionStream.filter(session -> !session.isCompleted());
                }

                // 5. 정렬
                Comparator<ExamRespDto.SessionList> comparator = Comparator.comparing(
                                session -> Integer.parseInt(session.getSession().split("회차")[0]));

                if ("desc".equals(sort)) {
                        comparator = comparator.reversed();
                }

                return sessionStream
                                .sorted(comparator)
                                .collect(Collectors.toList());
        }
}
