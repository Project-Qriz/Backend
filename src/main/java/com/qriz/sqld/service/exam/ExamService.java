package com.qriz.sqld.service.exam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.qriz.sqld.domain.question.option.OptionRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.daily.ResultDetailDto;
import com.qriz.sqld.dto.exam.ExamReqDto;
import com.qriz.sqld.dto.exam.ExamRespDto;
import com.qriz.sqld.dto.exam.ExamTestResult;
import com.qriz.sqld.dto.exam.ExamTestResult.SimpleMajorItem;
import com.qriz.sqld.dto.exam.ExamTestResult.SimpleSubjectDetails;
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
        private final OptionRepository optionRepository;

        private final Logger log = LoggerFactory.getLogger(ExamService.class);

        /**
         * 특정 회차의 모의고사 문제들을 조회하고 DTO로 변환
         */
        @Transactional(readOnly = true)
        public ExamTestResult getExamQuestionsBySession(Long userId, Long examId) {
                // examId를 "회차" 형식의 문자열로 변환
                String session = examId + "회차";

                List<Question> examQuestions = questionRepository.findByCategoryAndExamSessionOrderById(3, session);
                if (examQuestions.isEmpty()) {
                        throw new CustomApiException("해당 회차의 모의고사 문제를 찾을 수 없습니다.");
                }
                List<TestRespDto.ExamRespDto> questionDtos = examQuestions.stream()
                                .map(q -> new TestRespDto.ExamRespDto(q, Objects.hash(q.getId(), userId, session)))
                                .collect(Collectors.toList());
                int totalTimeLimit = 5400; // 총 제한 시간 90분
                return new ExamTestResult(questionDtos, totalTimeLimit);
        }

        /**
         * 모의고사 제출 처리
         */
        @Transactional
        public List<TestRespDto.ExamSubmitRespDto> processExamSubmission(Long userId, Long examId,
                        ExamReqDto examSubmitReqDto) {

                String session = examId + "회차";

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));

                // 기존 세션 삭제 로직 등은 그대로 유지

                // 새로운 세션 생성
                UserExamSession userExamSession = createNewExamSession(user, session);

                List<TestRespDto.ExamSubmitRespDto> results = new ArrayList<>();
                for (ExamReqDto.ExamSubmitReqDto activity : examSubmitReqDto.getActivities()) {
                        Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                                        .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));

                        // 제출된 optionId를 통해 Option 엔티티 조회
                        Option submittedOption = optionRepository.findById((long) activity.getOptionId())
                                        .orElseThrow(() -> new CustomApiException("선택한 옵션을 찾을 수 없습니다."));

                        // 해당 옵션이 이 문제에 속하는지 확인
                        if (!submittedOption.getQuestion().getId().equals(question.getId())) {
                                throw new CustomApiException("선택한 옵션이 해당 문제와 일치하지 않습니다.");
                        }

                        // 정답 여부 판별: Option 엔티티의 isAnswer 사용
                        boolean isCorrect = submittedOption.isAnswer();

                        // UserActivity 기록 생성
                        UserActivity userActivity = new UserActivity();
                        userActivity.setUser(user);
                        userActivity.setQuestion(question);
                        userActivity.setTestInfo(userExamSession.getSession());
                        userActivity.setQuestionNum(activity.getQuestionNum());
                        // 여기서 checked 필드에 선택한 optionId를 문자열로 저장 (또는 별도 필드로 저장)
                        userActivity.setChecked(String.valueOf(activity.getOptionId()));
                        userActivity.setTimeSpent(0); // 모의고사의 경우 시간 정보가 없으면 0 처리
                        userActivity.setCorrection(isCorrect);
                        userActivity.setDate(LocalDateTime.now());
                        userActivity.setExamSession(userExamSession);
                        userActivity.setScore(isCorrect ? 2.0 : 0.0); // 예: 맞으면 2점

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

                        // Option PK로 Option 엔티티 조회
                        Option submittedOption = optionRepository.findById((long) activity.getOptionId())
                                        .orElseThrow(() -> new CustomApiException("선택한 옵션을 찾을 수 없습니다."));

                        boolean isCorrect = submittedOption.isAnswer();
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
         */
        @Transactional(readOnly = true)
        public ResultDetailDto getExamResultDetail(Long userId, Long examId, Long questionId) {
                String session = examId + "회차";

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

        @Transactional(readOnly = true)
        public SimpleSubjectDetails getExamScoreBySubject(Long userId, Long examId, String subject) {
                // examId를 "회차" 형식의 문자열로 변환
                String session = examId + "회차";

                // 영어 subject를 한글 과목명으로 매핑 (예: "subject1" → "1과목", "subject2" → "2과목")
                String mappedSubject = mapSubject(subject);

                // 해당 세션의 ExamSession 조회
                List<UserExamSession> userExamSessions = userExamSessionRepository
                                .findByUserIdAndSessionOrderByCompletionDateDesc(userId, session);
                if (userExamSessions.isEmpty()) {
                        throw new CustomApiException("No exam session found for the given session.");
                }
                UserExamSession latestSession = userExamSessions.get(0);

                // 과목별 주요 항목 정의
                List<String> majorItems;
                if ("1과목".equals(mappedSubject)) {
                        majorItems = List.of("데이터 모델링의 이해", "데이터 모델과 SQL");
                } else if ("2과목".equals(mappedSubject)) {
                        majorItems = List.of("SQL 기본", "SQL 활용", "관리 구문");
                } else {
                        throw new CustomApiException("Unsupported subject: " + mappedSubject);
                }

                // 각 주요 항목의 점수를 저장할 맵 초기화
                Map<String, Double> majorScoreMap = new HashMap<>();
                for (String major : majorItems) {
                        majorScoreMap.put(major, 0.0);
                }
                double totalScore = 0.0;

                // 최신 세션의 활동(Activity) 조회
                List<UserActivity> latestActivities = userActivityRepository.findByExamSession(latestSession);
                for (UserActivity activity : latestActivities) {
                        // 해당 활동이 지정된 과목에 속하는지 확인
                        if (mappedSubject.equals(activity.getQuestion().getSkill().getTitle())) {
                                // 여기서 주요 항목은 skill.getType()로 가정
                                String majorItem = activity.getQuestion().getSkill().getType();
                                if (majorScoreMap.containsKey(majorItem)) {
                                        double updatedScore = majorScoreMap.get(majorItem) + activity.getScore();
                                        majorScoreMap.put(majorItem, updatedScore);
                                        totalScore += activity.getScore();
                                }
                        }
                }

                // 총점이 100을 초과하면 비율 조정 (옵션)
                if (totalScore > 100) {
                        double factor = 100.0 / totalScore;
                        totalScore = 100.0;
                        for (String key : majorScoreMap.keySet()) {
                                majorScoreMap.put(key, majorScoreMap.get(key) * factor);
                        }
                }

                // majorScoreMap을 SimpleMajorItem 리스트로 변환
                List<SimpleMajorItem> simpleMajorItems = majorScoreMap.entrySet().stream()
                                .map(e -> new SimpleMajorItem(e.getKey(), e.getValue()))
                                .collect(Collectors.toList());

                return new SimpleSubjectDetails(mappedSubject, totalScore, simpleMajorItems);
        }

        /**
         * 영어 subject 값을 한글 과목명("1과목", "2과목")으로 매핑합니다.
         */
        private String mapSubject(String subject) {
                if ("subject1".equalsIgnoreCase(subject)) {
                        return "1과목";
                } else if ("subject2".equalsIgnoreCase(subject)) {
                        return "2과목";
                } else {
                        throw new CustomApiException("Unsupported subject: " + subject);
                }
        }

        @Transactional(readOnly = true)
        public ExamTestResult.ExamResultsDto getExamResults(Long userId, Long examId) {
                String session = examId + "회차";

                List<UserExamSession> userExamSessions = userExamSessionRepository
                                .findByUserIdAndSessionOrderByCompletionDateDesc(userId, session);
                if (userExamSessions.isEmpty()) {
                        throw new CustomApiException("해당 회차의 모의고사 세션을 찾을 수 없습니다.");
                }
                UserExamSession latestSession = userExamSessions.get(0);

                // 문제별 채점 결과 생성
                List<ExamTestResult.ResultDto> resultDtos = userActivityRepository.findByExamSession(latestSession)
                                .stream()
                                .map(activity -> new ExamTestResult.ResultDto(
                                                activity.getQuestionNum(),
                                                activity.getQuestion().getSkill().getKeyConcepts(),
                                                activity.getQuestion().getQuestion(),
                                                activity.isCorrection()))
                                .sorted(Comparator.comparingInt(ExamTestResult.ResultDto::getQuestionNum))
                                .collect(Collectors.toList());

                // 과거 점수 이력 계산
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
                                        .map(sessionItem -> {
                                                List<ExamTestResult.ItemScore> sessionScores = new ArrayList<>();
                                                if (sessionItem.getSubject1Score() != null) {
                                                        sessionScores.add(new ExamTestResult.ItemScore("1과목",
                                                                        sessionItem.getSubject1Score()));
                                                }
                                                if (sessionItem.getSubject2Score() != null) {
                                                        sessionScores.add(new ExamTestResult.ItemScore("2과목",
                                                                        sessionItem.getSubject2Score()));
                                                }
                                                return new ExamTestResult.HistoricalScore(
                                                                sessionItem.getCompletionDate(), sessionScores,
                                                                dateGroupedSessions.size());
                                        })
                                        .collect(Collectors.toList());
                }

                return new ExamTestResult.ExamResultsDto(resultDtos, historicalScores);
        }

        @Transactional(readOnly = true)
        public ExamTestResult.SubjectDetails getSubjectScoreDetails(Long userId, Long examId, String subject) {
                String session = examId + "회차";

                // 영어 subject를 한글 과목명("1과목", "2과목")으로 매핑
                String mappedSubject = mapSubject(subject);

                List<UserExamSession> userExamSessions = userExamSessionRepository
                                .findByUserIdAndSessionOrderByCompletionDateDesc(userId, session);
                if (userExamSessions.isEmpty()) {
                        throw new CustomApiException("No exam session found for the given session.");
                }
                UserExamSession latestSession = userExamSessions.get(0);

                // 과목별 주요 항목 및 세부 항목 목록 미리 정의
                Map<String, List<String>> majorAndSubItems = new HashMap<>();
                if ("1과목".equals(mappedSubject)) {
                        majorAndSubItems.put("데이터 모델링의 이해",
                                        List.of("데이터모델의 이해", "엔터티", "속성", "관계", "식별자"));
                        majorAndSubItems.put("데이터 모델과 SQL",
                                        List.of("정규화", "관계와 조인의 이해", "모델이 표현하는 트랜잭션의 이해", "NULL 속성의 이해",
                                                        "본질식별자 vs 인조 식별자"));
                } else if ("2과목".equals(mappedSubject)) {
                        majorAndSubItems.put("SQL 기본",
                                        List.of("관계형 데이터베이스 개요", "SELECT 문", "함수", "WHERE 절", "GROUP BY, HAVING 절",
                                                        "ORDER BY 절", "조인", "표준 조인"));
                        majorAndSubItems.put("SQL 활용",
                                        List.of("SQL 활용")); // 필요 시 세부 항목 추가
                        majorAndSubItems.put("관리 구문",
                                        List.of("DML", "TCL", "DDL", "DCL"));
                } else {
                        throw new CustomApiException("Unsupported subject: " + mappedSubject);
                }

                // SubjectDetails 초기화 (빈 MajorItemDetail 목록으로)
                ExamTestResult.SubjectDetails subjectDetails = new ExamTestResult.SubjectDetails(mappedSubject);
                // 각 주요 항목을 초기 0점과 해당 세부 항목 목록으로 생성
                for (Map.Entry<String, List<String>> entry : majorAndSubItems.entrySet()) {
                        String majorItem = entry.getKey();
                        List<ExamTestResult.SubItemScore> subItemScoreList = new ArrayList<>();
                        for (String subItem : entry.getValue()) {
                                subItemScoreList.add(new ExamTestResult.SubItemScore(subItem, 0.0));
                        }
                        subjectDetails.getMajorItems()
                                        .add(new ExamTestResult.MajorItemDetail(majorItem, 0.0, subItemScoreList));
                }

                // 최신 세션의 활동(Activity) 조회 후, 점수 누적
                List<UserActivity> latestActivities = userActivityRepository.findByExamSession(latestSession);
                for (UserActivity activity : latestActivities) {
                        // 과목이 일치하는 활동만 처리
                        if (mappedSubject.equals(activity.getQuestion().getSkill().getTitle())) {
                                // 활동에서 주요 항목은 skill.getType(), 세부 항목은 skill.getSubType()로 가정
                                String majorItem = activity.getQuestion().getSkill().getType();
                                String subItem = activity.getQuestion().getSkill().getKeyConcepts();
                                if (majorAndSubItems.containsKey(majorItem)) {
                                        // 주요 항목 점수 누적
                                        subjectDetails.addMajorItemScore(majorItem, activity.getScore());
                                        // 해당 주요 항목의 세부 항목 점수 누적
                                        for (ExamTestResult.MajorItemDetail mid : subjectDetails.getMajorItems()) {
                                                if (mid.getMajorItem().equals(majorItem)) {
                                                        mid.addSubItemScore(subItem, activity.getScore());
                                                        break;
                                                }
                                        }
                                }
                        }
                }

                subjectDetails.adjustTotalScore();
                return subjectDetails;
        }

        @Transactional(readOnly = true)
        public List<ExamTestResult.SubjectDetails> getSubjectScoreDetailsForAllSubjects(Long userId, Long examId) {
                // 두 과목에 대한 세부 정보를 개별적으로 구한 후 리스트로 반환
                ExamTestResult.SubjectDetails details1 = getSubjectScoreDetails(userId, examId, "subject1");
                ExamTestResult.SubjectDetails details2 = getSubjectScoreDetails(userId, examId, "subject2");

                return List.of(details1, details2);
        }

        @Transactional
        public List<ExamTestResult.SimpleMajorItem> getMajorResults(Long userId, Long examId) {
                // examId를 "회차" 형식의 문자열로 변환 (예: 1 -> "1회차")
                String session = examId + "회차";

                // 해당 세션의 ExamSession 조회
                List<UserExamSession> userExamSessions = userExamSessionRepository
                                .findByUserIdAndSessionOrderByCompletionDateDesc(userId, session);
                if (userExamSessions.isEmpty()) {
                        throw new CustomApiException("해당 회차의 모의고사 세션을 찾을 수 없습니다.");
                }
                // 최신 세션 사용
                UserExamSession latestSession = userExamSessions.get(0);

                // 해당 세션의 활동(Activity) 조회
                List<UserActivity> activities = userActivityRepository.findByExamSession(latestSession);

                // 각 주요 항목별 점수를 누적 (주요 항목은 skill.getType()로 가정)
                Map<String, Double> majorScoreMap = new HashMap<>();
                for (UserActivity activity : activities) {
                        String majorItem = activity.getQuestion().getSkill().getType();
                        // 누적 점수 계산
                        double currentScore = majorScoreMap.getOrDefault(majorItem, 0.0);
                        majorScoreMap.put(majorItem, currentScore + activity.getScore());
                }

                // Map을 List<SimpleMajorItem>로 변환하여 반환
                List<ExamTestResult.SimpleMajorItem> result = majorScoreMap.entrySet().stream()
                                .map(e -> new ExamTestResult.SimpleMajorItem(e.getKey(), e.getValue()))
                                .collect(Collectors.toList());

                return result;
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
