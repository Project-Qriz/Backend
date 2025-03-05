package com.qriz.sqld.service.exam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.qriz.sqld.domain.question.option.Option;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.daily.ResultDetailDto;
import com.qriz.sqld.dto.exam.ExamReqDto;
import com.qriz.sqld.dto.exam.ExamRespDto;
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

        /**
         * 특정 회차의 모의고사 문제들을 조회하고 DTO로 변환
         */
        @Transactional(readOnly = true)
        public ExamTestResult getExamQuestionsBySession(Long userId, String session) {
                // category 3은 모의고사를 의미
                List<Question> examQuestions = questionRepository.findByCategoryAndExamSessionOrderById(3, session);
                if (examQuestions.isEmpty()) {
                        throw new CustomApiException("해당 회차의 모의고사 문제를 찾을 수 없습니다.");
                }
                // ExamRespDto 생성자 내에서 Option 엔티티와 랜덤화 로직을 반영하도록 수정
                List<TestRespDto.ExamRespDto> questionDtos = examQuestions.stream()
                                .map(TestRespDto.ExamRespDto::new)
                                .collect(Collectors.toList());
                int totalTimeLimit = 5400; // 총 제한 시간 90분
                return new ExamTestResult(questionDtos, totalTimeLimit);
        }

        /**
         * 모의고사 제출 처리
         */
        @Transactional
        public List<TestRespDto.ExamSubmitRespDto> processExamSubmission(Long userId, String session,
                        ExamReqDto examSubmitReqDto) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));

                // 오늘 날짜 범위 내의 기존 세션 삭제
                LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime tomorrow = today.plusDays(1);
                List<UserExamSession> todaySessions = userExamSessionRepository
                                .findByUserIdAndSessionAndCompletionDateBetween(userId, session, today, tomorrow);
                for (UserExamSession previousSession : todaySessions) {
                        List<UserActivity> activities = userActivityRepository.findByExamSession(previousSession);
                        for (UserActivity activity : activities) {
                                clipRepository.deleteByUserActivity(activity);
                        }
                        userActivityRepository.deleteByExamSession(previousSession);
                        userExamSessionRepository.delete(previousSession);
                }

                // 새로운 세션 생성
                UserExamSession userExamSession = createNewExamSession(user, session);

                // 활동 기록 및 결과 생성
                List<TestRespDto.ExamSubmitRespDto> results = new ArrayList<>();
                for (ExamReqDto.ExamSubmitReqDto activity : examSubmitReqDto.getActivities()) {
                        Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                                        .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));
                        // 정답 비교 시 Option 엔티티를 통해 가져온 정답 사용 (랜덤화된 구조 반영)
                        UserActivity userActivity = createUserActivity(user, question, activity, userExamSession);
                        userActivityRepository.save(userActivity);
                        createClippedRecord(userActivity);
                        results.add(createResultDto(userActivity, user.getId(), question));
                }

                // 과목별 점수 계산 및 세션 업데이트
                Map<String, Double> subjectScores = calculateSubjectScores(examSubmitReqDto.getActivities());
                userExamSession.setSubject1Score(subjectScores.getOrDefault("1과목", 0.0));
                userExamSession.setSubject2Score(subjectScores.getOrDefault("2과목", 0.0));
                userExamSessionRepository.save(userExamSession);

                return results;
        }

        private UserExamSession createNewExamSession(User user, String session) {
                UserExamSession userExamSession = UserExamSession.builder()
                                .user(user)
                                .session(session)
                                .attemptCount(1)
                                .completionDate(LocalDateTime.now())
                                .build();
                return userExamSessionRepository.save(userExamSession);
        }

        /**
         * Option 엔티티 기반 정답 비교를 적용하여 UserActivity 생성
         */
        private UserActivity createUserActivity(User user, Question question,
                        ExamReqDto.ExamSubmitReqDto activity, UserExamSession userExamSession) {
                String correctAnswer = question.getSortedOptions().stream()
                                .filter(Option::isAnswer)
                                .map(Option::getContent)
                                .findFirst()
                                .orElse("");
                boolean isCorrect = correctAnswer.equals(activity.getChecked());

                UserActivity userActivity = new UserActivity();
                userActivity.setUser(user);
                userActivity.setQuestion(question);
                userActivity.setTestInfo(userExamSession.getSession());
                userActivity.setQuestionNum(activity.getQuestionNum());
                userActivity.setChecked(activity.getChecked());
                userActivity.setCorrection(isCorrect);
                userActivity.setDate(LocalDateTime.now());
                userActivity.setScore(isCorrect ? 2.0 : 0.0); // 맞으면 2점
                userActivity.setExamSession(userExamSession);
                return userActivity;
        }

        private void createClippedRecord(UserActivity userActivity) {
                Clipped clipped = new Clipped();
                clipped.setUserActivity(userActivity);
                clipped.setDate(LocalDateTime.now());
                clipRepository.save(clipped);
        }

        private TestRespDto.ExamSubmitRespDto createResultDto(UserActivity userActivity, Long userId,
                        Question question) {
                return new TestRespDto.ExamSubmitRespDto(
                                userActivity.getId(),
                                userId,
                                new TestRespDto.ExamSubmitRespDto.QuestionRespDto(
                                                question.getId(),
                                                getCategoryName(question.getCategory())),
                                userActivity.getQuestionNum(),
                                userActivity.getChecked(),
                                userActivity.isCorrection());
        }

        /**
         * 제출된 활동들을 바탕으로 과목별 점수 계산
         */
        private Map<String, Double> calculateSubjectScores(List<ExamReqDto.ExamSubmitReqDto> activities) {
                Map<String, Double> subjectScores = new HashMap<>();
                for (ExamReqDto.ExamSubmitReqDto activity : activities) {
                        Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                                        .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));
                        String correctAnswer = question.getSortedOptions().stream()
                                        .filter(Option::isAnswer)
                                        .map(Option::getContent)
                                        .findFirst()
                                        .orElse("");
                        boolean isCorrect = correctAnswer.equals(activity.getChecked());
                        double score = isCorrect ? 2.0 : 0.0;
                        String title = question.getSkill().getTitle(); // "1과목" 또는 "2과목"
                        subjectScores.merge(title, score, Double::sum);
                }
                return subjectScores;
        }

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
         * 모의고사 결과 상세보기
         * 변경: ResultDetailDto.from()을 사용하여 Option 엔티티 기반 정보를 반영
         */
        @Transactional(readOnly = true)
        public ResultDetailDto getExamResultDetail(Long userId, String session, Long questionId) {
                log.info("Getting exam result detail for userId: {}, session: {}, questionId: {}",
                                userId, session, questionId);
                String testInfo = session;
                UserActivity userActivity = userActivityRepository
                                .findByUserIdAndTestInfoAndQuestionId(userId, testInfo, questionId)
                                .orElseThrow(() -> new CustomApiException("해당 문제의 풀이 결과를 찾을 수 없습니다."));
                Question question = userActivity.getQuestion();
                ResultDetailDto result = ResultDetailDto.from(question, userActivity);
                return result;
        }

        /**
         * 모의고사 과목별 세부 항목 점수, 문제 풀이 결과 조회
         * (대부분 Exam의 경우 Daily와 유사하므로 기존 로직 유지, 필요시 Option 관련 DTO 매핑 수정)
         */
        @Transactional(readOnly = true)
        public ExamTestResult.Response getExamSubjectDetails(Long userId, String session) {
                List<UserExamSession> userExamSessions = userExamSessionRepository
                                .findByUserIdAndSessionOrderByCompletionDateDesc(userId, session);
                if (userExamSessions.isEmpty()) {
                        throw new CustomApiException("해당 회차의 모의고사 세션을 찾을 수 없습니다.");
                }
                UserExamSession latestSession = userExamSessions.get(0);
                List<ExamTestResult.SubjectDetails> userExamInfoList = new ArrayList<>();
                ExamTestResult.SubjectDetails subject1Details = new ExamTestResult.SubjectDetails("1과목");
                Map<String, Double> subject1TypeScores = new HashMap<>();
                ExamTestResult.SubjectDetails subject2Details = new ExamTestResult.SubjectDetails("2과목");
                Map<String, Double> subject2TypeScores = new HashMap<>();

                List<UserActivity> latestActivities = userActivityRepository.findByExamSession(latestSession);
                for (UserActivity activity : latestActivities) {
                        Question question = activity.getQuestion();
                        Skill skill = question.getSkill();
                        if (skill.getTitle().equals("1과목")) {
                                subject1TypeScores.merge(skill.getKeyConcepts(), activity.getScore(), Double::sum);
                        } else if (skill.getTitle().equals("2과목")) {
                                subject2TypeScores.merge(skill.getKeyConcepts(), activity.getScore(), Double::sum);
                        }
                }
                subject1TypeScores.forEach((type, score) -> subject1Details.addScore(type, score));
                subject2TypeScores.forEach((type, score) -> subject2Details.addScore(type, score));
                userExamInfoList.add(subject1Details);
                userExamInfoList.add(subject2Details);

                List<ExamTestResult.ResultDto> subjectResultsList = latestActivities.stream()
                                .map(activity -> new ExamTestResult.ResultDto(
                                                activity.getQuestionNum(),
                                                activity.getQuestion().getSkill().getKeyConcepts(),
                                                activity.getQuestion().getQuestion(),
                                                activity.isCorrection()))
                                .sorted(Comparator.comparingInt(ExamTestResult.ResultDto::getQuestionNum))
                                .collect(Collectors.toList());

                Map<LocalDate, UserExamSession> dateGroupedSessions = userExamSessions.stream()
                                .filter(s -> s.getCompletionDate() != null)
                                .collect(Collectors.groupingBy(
                                                s -> s.getCompletionDate().toLocalDate(),
                                                Collectors.collectingAndThen(
                                                                Collectors.minBy(Comparator.comparing(
                                                                                UserExamSession::getCompletionDate)
                                                                                .reversed()),
                                                                Optional::get)));

                List<ExamTestResult.HistoricalScore> historicalScores = new ArrayList<>();
                if (dateGroupedSessions.size() >= 2) {
                        historicalScores = dateGroupedSessions.values().stream()
                                        .sorted(Comparator.comparing(UserExamSession::getCompletionDate).reversed())
                                        .limit(5)
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
                List<String> allSessions = questionRepository.findDistinctExamSessionByCategory(3);
                List<UserExamSession> userSessions = userExamSessionRepository
                                .findByUserIdOrderByCompletionDateDesc(userId);
                Map<String, UserExamSession> completedSessionsMap = userSessions.stream()
                                .collect(Collectors.toMap(
                                                UserExamSession::getSession,
                                                session -> session,
                                                (existing, replacement) -> existing));

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
                                        return new ExamRespDto.SessionList(completed, session, totalScore);
                                });

                if ("completed".equals(status)) {
                        sessionStream = sessionStream.filter(ExamRespDto.SessionList::isCompleted);
                } else if ("incomplete".equals(status)) {
                        sessionStream = sessionStream.filter(session -> !session.isCompleted());
                }

                Comparator<ExamRespDto.SessionList> comparator = Comparator.comparing(
                                session -> Integer.parseInt(session.getSession().split("회차")[0]));
                if ("desc".equals(sort)) {
                        comparator = comparator.reversed();
                }

                return sessionStream.sorted(comparator).collect(Collectors.toList());
        }
}
