package com.qriz.sqld.service.daily;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.question.option.Option;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.domain.UserActivity.UserActivity;
import com.qriz.sqld.domain.UserActivity.UserActivityRepository;
import com.qriz.sqld.domain.clip.ClipRepository;
import com.qriz.sqld.domain.clip.Clipped;
import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.dto.daily.ResultDetailDto;
import com.qriz.sqld.dto.daily.DailyScoreDto;
import com.qriz.sqld.dto.daily.DaySubjectDetailsDto;
import com.qriz.sqld.dto.daily.UserDailyDto;
import com.qriz.sqld.dto.daily.WeeklyTestResultDto;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.util.WeekendPlanUtil;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DailyService {

    private final QuestionRepository questionRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;
    private final UserDailyRepository userDailyRepository;
    private final ClipRepository clipRepository;
    private final DailyPlanService dailyPlanService;
    private final DKTService dktService;

    private final Logger log = LoggerFactory.getLogger(DailyService.class);

    @Lazy
    @Autowired
    private final WeekendPlanUtil weekendPlanUtil;

    /**
     * 오늘의 데일리 테스트 문제를 가져오기
     */
    @Transactional
    public List<TestRespDto.DailyRespDto> getDailyTestQuestionsByDay(Long userId, String dayNumber) {
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("해당 일자의 데일리 플랜을 찾을 수 없습니다."));

        if (!dailyPlanService.canAccessDay(userId, userDaily.getDayNumber())) {
            throw new CustomApiException("이 테스트에 아직 접근할 수 없습니다.");
        }

        if (userDaily.isPassed() || (userDaily.getAttemptCount() > 0 && !userDaily.isRetestEligible())) {
            throw new CustomApiException("이미 완료된 테스트이거나 재시험 자격이 없습니다.");
        }

        // 첫 시도와 재시험 모두 동일한 로직으로 처리
        List<Question> questions;
        if (userDaily.getPlannedSkills() == null) {
            questions = getWeekFourQuestions(userId, userDaily);
        } else if (userDaily.isReviewDay()) {
            questions = weekendPlanUtil.getWeekendQuestions(userId, userDaily);
        } else {
            questions = getRegularDayQuestions(userDaily);
        }

        // 모든 경우에 랜덤화된 선택지만 반환하도록 통일
        return questions.stream()
                .map(TestRespDto.DailyRespDto::new)
                .collect(Collectors.toList());
    }

    private List<Question> getWeekFourQuestions(Long userId, UserDaily todayPlan) {
        LocalDateTime startDateTime = todayPlan.getPlanDate().minusWeeks(3).atStartOfDay();
        LocalDateTime endDateTime = todayPlan.getPlanDate().atTime(23, 59, 59);
        List<UserActivity> activities = userActivityRepository.findByUserIdAndDateBetween(
                userId, startDateTime, endDateTime);
        List<Double> predictions = dktService.getPredictions(userId, activities);
        return getQuestionsBasedOnPredictions(predictions);
    }

    private List<Question> getRegularDayQuestions(UserDaily todayPlan) {
        return questionRepository.findRandomQuestionsBySkillsAndCategory(
                todayPlan.getPlannedSkills(),
                2, // 데일리 카테고리 값
                20 // 문제 수
        );
    }

    private List<Question> getQuestionsBasedOnPredictions(List<Double> predictions) {
        List<Long> sortedSkillIds = IntStream.range(0, predictions.size())
                .boxed()
                .sorted(Comparator.comparingDouble(predictions::get))
                .map(Long::valueOf)
                .limit(5)
                .collect(Collectors.toList());
        return questionRepository.findRandomQuestionsBySkillIdsAndCategory(sortedSkillIds, 2, 10);
    }

    /**
     * 데일리 테스트 제출 처리
     */
    @Transactional
    public List<TestRespDto.TestSubmitRespDto> processDailyTestSubmission(Long userId, String dayNumber,
            TestReqDto testSubmitReqDto) {
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("해당 일자의 데일리 플랜을 찾을 수 없습니다."));
        if (userDaily.isPassed() || (userDaily.getAttemptCount() > 0 && !userDaily.isRetestEligible())) {
            throw new CustomApiException("이미 완료된 테스트이거나 재시험 자격이 없습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));
        List<TestRespDto.TestSubmitRespDto> results = new ArrayList<>();
        for (TestReqDto.TestSubmitReqDto activity : testSubmitReqDto.getActivities()) {
            Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                    .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));
            UserActivity userActivity = new UserActivity();
            userActivity.setUser(user);
            userActivity.setQuestion(question);
            userActivity.setTestInfo(dayNumber);
            userActivity.setQuestionNum(activity.getQuestionNum());
            userActivity.setChecked(activity.getChecked());
            userActivity.setTimeSpent(activity.getTimeSpent());
            // 수정: Option 엔티티 기반 정답 비교
            String correctAnswer = question.getSortedOptions().stream()
                    .filter(Option::isAnswer)
                    .map(Option::getContent)
                    .findFirst()
                    .orElse("");
            userActivity.setCorrection(correctAnswer.equals(activity.getChecked()));
            userActivity.setDate(LocalDateTime.now());
            userActivity.setUserDaily(userDaily);
            double score = calculateScore(activity, question);
            userActivity.setScore(score);
            userActivityRepository.save(userActivity);
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
        double totalPossibleScore = testSubmitReqDto.getActivities().stream()
                .mapToDouble(activity -> {
                    Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                            .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));
                    return getPointsForDifficulty(question.getDifficulty());
                }).sum();
        double userScore = testSubmitReqDto.getActivities().stream()
                .mapToDouble(activity -> {
                    Question question = questionRepository.findById(activity.getQuestion().getQuestionId())
                            .orElseThrow(() -> new CustomApiException("문제를 찾을 수 없습니다."));
                    String correctAnswer = question.getSortedOptions().stream()
                            .filter(Option::isAnswer)
                            .map(Option::getContent)
                            .findFirst()
                            .orElse("");
                    return correctAnswer.equals(activity.getChecked())
                            ? getPointsForDifficulty(question.getDifficulty())
                            : 0;
                }).sum();
        boolean isPassed = userScore >= totalPossibleScore * 0.7;
        userDaily.updateTestStatus(isPassed);
        if (isPassed) {
            userDaily.setPassed(true);
            userDaily.setRetestEligible(false);
        } else if (userDaily.getAttemptCount() >= 2) {
            userDaily.setRetestEligible(false);
        }
        userDailyRepository.save(userDaily);
        if (isPassed || userDaily.getAttemptCount() >= 2) {
            for (TestRespDto.TestSubmitRespDto result : results) {
                UserActivity ua = userActivityRepository.findById(result.getActivityId())
                        .orElseThrow(() -> new CustomApiException("UserActivity를 찾을 수 없습니다."));
                Clipped clipped = new Clipped();
                clipped.setUserActivity(ua);
                clipped.setDate(LocalDateTime.now());
                clipRepository.save(clipped);
            }
        }
        int day = Integer.parseInt(dayNumber.replace("Day", ""));
        if (day % 7 == 5 && day <= 19) {
            dailyPlanService.updateWeekendPlan(userId, day);
        }
        return results;
    }

    private int getPointsForDifficulty(Integer difficulty) {
        if (difficulty == null)
            return 5;
        switch (difficulty) {
            case 1:
                return 5;
            case 2:
                return 7;
            case 3:
                return 10;
            default:
                return 5;
        }
    }

    /**
     * 수정된 채점 메서드: Option 엔티티 기반 정답 비교
     */
    private double calculateScore(TestReqDto.TestSubmitReqDto activity, Question question) {
        int points = getPointsForDifficulty(question.getDifficulty());
        String correctAnswer = question.getSortedOptions().stream()
                .filter(Option::isAnswer)
                .map(Option::getContent)
                .findFirst()
                .orElse("");
        return correctAnswer.equals(activity.getChecked()) ? points : 0;
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
     * 오늘의 공부 결과 - 문제 상세보기
     * 수정: ResultDetailDto.from() 메서드를 사용하여 Option 엔티티 기반 정보를 반영
     */
    @Transactional(readOnly = true)
    public ResultDetailDto getDailyResultDetail(Long userId, String dayNumber, Long questionId) {
        log.info("Getting daily result detail for userId: {}, dayNumber: {}, questionId: {}",
                userId, dayNumber, questionId);
        String testInfo = dayNumber;
        UserActivity userActivity = userActivityRepository
                .findByUserIdAndTestInfoAndQuestionId(userId, testInfo, questionId)
                .orElseThrow(() -> new CustomApiException("해당 문제의 풀이 결과를 찾을 수 없습니다."));
        Question question = userActivity.getQuestion();
        // ResultDetailDto.from() 내부에서 getSortedOptions()를 사용하여 변경된 구조를 반영
        ResultDetailDto result = ResultDetailDto.from(question, userActivity);
        return result;
    }

    /**
     * 특정 Day 가 포함된 주의 과목별 테스트 결과 점수
     * 
     * @param userId
     * @param dayNumber
     * @return
     */
    public WeeklyTestResultDto getDetailedWeeklyTestResult(Long userId, String dayNumber) {
        log.info("Starting getDetailedWeeklyTestResult for userId: {} and dayNumber: {}", userId, dayNumber);

        UserDaily currentDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("Daily plan not found"));

        LocalDate startDate = currentDaily.getPlanDate().with(DayOfWeek.MONDAY);
        LocalDate endDate = startDate.plusDays(6);

        log.info("Fetching activities between {} and {}", startDate, endDate);
        List<UserActivity> activities = userActivityRepository.findByUserIdAndDateBetween(
                userId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));

        Map<String, DailyScoreDto> dailyScores = new HashMap<>();

        for (UserActivity activity : activities) {
            log.debug("Processing activity: {}", activity.getId());

            UserDaily daily = userDailyRepository.findByUserIdAndPlanDate(userId, activity.getDate().toLocalDate())
                    .orElseThrow(() -> new CustomApiException(
                            "Daily plan not found for date: " + activity.getDate().toLocalDate()));

            String dayNum = daily.getDayNumber();

            Optional.ofNullable(activity.getQuestion())
                    .map(Question::getSkill)
                    .ifPresentOrElse(
                            skill -> {
                                log.debug("Adding score for skill: {}", skill.getTitle());
                                dailyScores.computeIfAbsent(dayNum, k -> new DailyScoreDto())
                                        .addScore(skill.getTitle(), activity.getScore());
                            },
                            () -> log.warn("Question or Skill is null for activity: {}", activity.getId()));
        }

        log.info("Completed processing for getDetailedWeeklyTestResult");
        return new WeeklyTestResultDto(dailyScores);
    }

    @Transactional(readOnly = true)
    public DaySubjectDetailsDto.Response getDaySubjectDetails(Long userId, String dayNumber) {
        List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfo(userId, dayNumber);

        Map<String, DaySubjectDetailsDto.SubjectDetails> subjectDetailsMap = new HashMap<>();
        List<DaySubjectDetailsDto.DailyResultDto> dailyResults = new ArrayList<>();

        for (UserActivity activity : activities) {
            Question question = activity.getQuestion();
            Skill skill = question.getSkill();
            String title = skill.getTitle();
            String keyConcepts = skill.getKeyConcepts();

            double score = activity.isCorrection() ? 10.0 : 0.0;
            subjectDetailsMap.computeIfAbsent(title, k -> new DaySubjectDetailsDto.SubjectDetails(title))
                    .addScore(keyConcepts, score);

            DaySubjectDetailsDto.DailyResultDto resultDto = new DaySubjectDetailsDto.DailyResultDto(
                    skill.getKeyConcepts(),
                    question.getQuestion(),
                    activity.isCorrection());

            dailyResults.add(resultDto);
        }

        List<DaySubjectDetailsDto.SubjectDetails> subjectDetailsList = new ArrayList<>(subjectDetailsMap.values());

        for (DaySubjectDetailsDto.SubjectDetails subject : subjectDetailsList) {
            subject.adjustTotalScore();
        }

        return new DaySubjectDetailsDto.Response(dayNumber, subjectDetailsList, dailyResults);
    }

    @Transactional(readOnly = true)
    public UserDailyDto.DailyDetailsDto getDailyDetails(Long userId, String dayNumber) {
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("해당 일자의 데일리 플랜을 찾을 수 없습니다."));

        List<UserDailyDto.DailyDetailsDto.SkillDetailDto> skillDetails = userDaily.getPlannedSkills().stream()
                .map(skill -> UserDailyDto.DailyDetailsDto.SkillDetailDto.builder()
                        .id(skill.getId())
                        .keyConcepts(skill.getKeyConcepts())
                        .description(skill.getDescription())
                        .build())
                .collect(Collectors.toList());

        // 해당 데일리의 UserActivity들을 가져와서 총 점수 계산
        List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfo(userId, dayNumber);
        double totalScore = activities.stream()
                .mapToDouble(UserActivity::getScore)
                .sum();

        return UserDailyDto.DailyDetailsDto.builder()
                .dayNumber(userDaily.getDayNumber())
                .passed(userDaily.isPassed())
                .skills(skillDetails)
                .totalScore(totalScore)
                .build();
    }

    public UserDailyDto.TestStatusDto getDailyTestStatus(Long userId, String dayNumber) {
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("해당 일자의 데일리 플랜을 찾을 수 없습니다."));

        return UserDailyDto.TestStatusDto.builder()
                .dayNumber(userDaily.getDayNumber())
                .attemptCount(userDaily.getAttemptCount())
                .passed(userDaily.isPassed())
                .retestEligible(userDaily.isRetestEligible())
                .build();
    }

    // 테스트용
    @Transactional
    public void completeDailyTest(Long userId, String dayNumber) {
        log.info("Completing daily test for user {} and day {}", userId, dayNumber);
        dailyPlanService.completeDailyPlan(userId, dayNumber);

        if (dayNumber.equals("Day21")) {
            log.info("Day21 completed. Updating week four plan.");
            dailyPlanService.updateWeekFourPlan(userId);
        }
        log.info("Completed daily test for user {} and day {}", userId, dayNumber);
    }
}