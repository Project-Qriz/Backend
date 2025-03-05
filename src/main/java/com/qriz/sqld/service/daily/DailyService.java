package com.qriz.sqld.service.daily;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.question.option.Option;
import com.qriz.sqld.domain.question.option.OptionRepository;
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
import com.qriz.sqld.dto.daily.UserDailyDto.DailyDetailAndStatusDto;
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
import java.util.Objects;
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
    private final OptionRepository optionRepository;

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
                .map(q -> new TestRespDto.DailyRespDto(q, Objects.hash(q.getId(), userId, dayNumber)))
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

            // 제출된 optionId를 통해 Option 엔티티 조회
            Option submittedOption = optionRepository.findById(activity.getOptionId())
                    .orElseThrow(() -> new CustomApiException("선택한 옵션을 찾을 수 없습니다."));

            // 해당 옵션이 실제 이 문제에 속하는지 확인
            if (!submittedOption.getQuestion().getId().equals(question.getId())) {
                throw new CustomApiException("선택한 옵션이 해당 문제와 일치하지 않습니다.");
            }

            // 정답 여부 판별
            boolean isCorrect = submittedOption.isAnswer();

            UserActivity userActivity = new UserActivity();
            userActivity.setUser(user);
            userActivity.setQuestion(question);
            userActivity.setTestInfo(dayNumber);
            userActivity.setQuestionNum(activity.getQuestionNum());
            // 여기서는 제출된 optionId를 String 형태로 저장할 수 있습니다.
            userActivity.setChecked(String.valueOf(activity.getOptionId()));
            userActivity.setTimeSpent(activity.getTimeSpent());
            userActivity.setCorrection(isCorrect);
            userActivity.setDate(LocalDateTime.now());
            userActivity.setUserDaily(userDaily);

            userActivityRepository.save(userActivity);

            TestRespDto.TestSubmitRespDto result = new TestRespDto.TestSubmitRespDto(
                    userActivity.getId(),
                    userId,
                    new TestRespDto.TestSubmitRespDto.QuestionRespDto(
                            question.getId(),
                            getCategoryName(question.getCategory())),
                    activity.getQuestionNum(),
                    String.valueOf(activity.getOptionId()),
                    activity.getTimeSpent(),
                    isCorrect);
            results.add(result);
        }

        // 전체 점수 계산 및 플랜 상태 업데이트 (이하 기존 로직 유지)
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
                    Option option = optionRepository.findById(activity.getOptionId())
                            .orElseThrow(() -> new CustomApiException("선택한 옵션을 찾을 수 없습니다."));
                    return option.isAnswer() ? getPointsForDifficulty(question.getDifficulty()) : 0;
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
    public DailyDetailAndStatusDto getDailyDetailWithStatus(Long userId, String dayNumber) {
        // 해당 일자의 daily plan 조회
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("해당 일자의 데일리 플랜을 찾을 수 없습니다."));

        // skills 정보 구성
        List<DailyDetailAndStatusDto.SkillDetailDto> skills = userDaily.getPlannedSkills().stream()
                .map(skill -> DailyDetailAndStatusDto.SkillDetailDto.builder()
                        .id(skill.getId())
                        .keyConcepts(skill.getKeyConcepts())
                        .description(skill.getDescription())
                        .build())
                .collect(Collectors.toList());

        // UserActivity를 통한 총 점수 계산
        List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfo(userId, dayNumber);
        double totalScore = activities.stream()
                .mapToDouble(UserActivity::getScore)
                .sum();

        // 기존 testStatus 정보 구성
        int attemptCount = userDaily.getAttemptCount();
        boolean passed = userDaily.isPassed();
        boolean retestEligible = userDaily.isRetestEligible();

        // 추가: 진행 가능 여부 계산
        boolean available = false;
        if ("Day1".equals(dayNumber)) {
            available = true; // 첫 날은 항상 진행 가능
        } else {
            // 이전 Day 계산 (예: "Day2" → "Day1")
            int currentDayNum = Integer.parseInt(dayNumber.replace("Day", ""));
            String previousDay = "Day" + (currentDayNum - 1);
            UserDaily previousDaily = userDailyRepository.findByUserIdAndDayNumber(userId, previousDay)
                    .orElse(null);
            if (previousDaily != null && previousDaily.isPassed() && previousDaily.getCompletionDate() != null) {
                // 이전 Day가 완료되었고, 오늘의 계획일자가 이전 완료일 다음 날(또는 이후)이어야 진행 가능
                LocalDate allowedDate = previousDaily.getCompletionDate().plusDays(1);
                available = !userDaily.getPlanDate().isBefore(allowedDate);
            }
        }

        DailyDetailAndStatusDto.StatusDto status = DailyDetailAndStatusDto.StatusDto.builder()
                .attemptCount(attemptCount)
                .passed(passed)
                .retestEligible(retestEligible)
                .totalScore(totalScore)
                .available(available)
                .build();

        return DailyDetailAndStatusDto.builder()
                .dayNumber(userDaily.getDayNumber())
                .skills(skills)
                .status(status)
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