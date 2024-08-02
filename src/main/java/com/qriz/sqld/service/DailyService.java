package com.qriz.sqld.service;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.QuestionRepository;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.skillLevel.SkillLevel;
import com.qriz.sqld.domain.skillLevel.SkillLevelRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.domain.userActivity.UserActivity;
import com.qriz.sqld.domain.userActivity.UserActivityRepository;
import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.domain.preview.UserPreviewTest;
import com.qriz.sqld.domain.preview.UserPreviewTestRepository;
import com.qriz.sqld.dto.daily.DailyResultDetailDto;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class DailyService {

    private final QuestionRepository questionRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;
    private final UserDailyRepository userDailyRepository;
    private final DailyPlanService dailyPlanService;
    private final DKTService dktService;
    private final SurveyService surveyService;
    private final SkillLevelRepository skillLevelRepository;
    private final SkillRepository skillRepository;

    /**
     * 오늘의 데일리 테스트 문제를 가져오기
     * 
     * @param user 현재 사용자
     * @return 데일리 테스트 문제 목록
     */
    public List<TestRespDto.DailyRespDto> getDailyTestQuestionsByDay(Long userId, String dayNumber) {
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumberWithPlannedSkills(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("해당 일자의 데일리 플랜을 찾을 수 없습니다."));

        // 이전 Day 완료 여부 및 당일 접근 가능 여부 확인
        if (!dailyPlanService.canAccessDay(userId, userDaily.getDayNumber())) {
            throw new CustomApiException("이 테스트에 아직 접근할 수 없습니다.");
        }

        List<Question> questions;
        if (userDaily.getPlannedSkills() == null) {
            // 4주차: DKT 기반 문제 추천
            questions = getWeekFourQuestions(userId, userDaily);
        } else if (userDaily.isReviewDay()) {
            // 주말: 복습 문제
            questions = getWeekendQuestions(userId, userDaily);
        } else {
            // 평일: 일반 문제
            questions = getRegularDayQuestions(userDaily);
        }

        return questions.stream()
                .map(TestRespDto.DailyRespDto::new)
                .collect(Collectors.toList());
    }

    private List<Question> getWeekendQuestions(Long userId, UserDaily todayPlan) {
        List<Question> weakConceptQuestions = getWeakConceptQuestionsForWeek(userId, todayPlan);

        boolean isKnowsNothingUser = surveyService.isKnowsNothingUser(userId);
        if (!isKnowsNothingUser) {
            // 프리뷰 테스트를 진행한 사용자의 경우
            List<Question> previewTestWeakQuestions = getPreviewTestWeakQuestions(userId);
            weakConceptQuestions.addAll(previewTestWeakQuestions);
        }

        // 중복 제거 및 10개로 제한
        List<Question> finalQuestions = weakConceptQuestions.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());

        // 문제가 10개 미만이면 약점 개념에서 추가 문제 선택
        if (finalQuestions.size() < 10) {
            List<Question> additionalQuestions = getAdditionalWeakConceptQuestions(userId, todayPlan,
                    10 - finalQuestions.size());
            finalQuestions.addAll(additionalQuestions);
        }

        return finalQuestions;
    }

    private List<Question> getAdditionalWeakConceptQuestions(Long userId, UserDaily todayPlan, int count) {
        int currentDay = Integer.parseInt(todayPlan.getDayNumber().replace("Day", ""));
        int startDay = Math.max(1, currentDay - 5); // Day1 미만으로 내려가지 않도록

        List<UserDaily> previousDays = userDailyRepository.findByUserIdAndDayNumberBetween(
                userId, "Day" + startDay, "Day" + (currentDay - 1));

        Map<Long, SkillAccuracy> skillAccuracyMap = new HashMap<>();

        for (UserDaily daily : previousDays) {
            if (daily.isCompleted()) {
                List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfo(userId,
                        "데일리 테스트 - " + daily.getDayNumber());
                for (UserActivity activity : activities) {
                    Long skillId = activity.getQuestion().getSkill().getId();
                    skillAccuracyMap.computeIfAbsent(skillId, k -> new SkillAccuracy())
                            .addResult(activity.isCorrection());
                }
            }
        }

        List<Long> weakSkillIds = skillAccuracyMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> e.getValue().getAccuracy()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 약점 스킬이 충분하지 않은 경우, 모든 스킬을 포함
        if (weakSkillIds.size() < count) {
            List<Long> allSkillIds = skillRepository.findAllIds();
            for (Long skillId : allSkillIds) {
                if (!weakSkillIds.contains(skillId)) {
                    weakSkillIds.add(skillId);
                }
            }
        }

        // 요청된 수만큼의 문제를 가져오기
        List<Question> additionalQuestions = new ArrayList<>();
        int questionsPerSkill = Math.max(1, count / weakSkillIds.size());
        int remainingQuestions = count;

        for (Long skillId : weakSkillIds) {
            if (remainingQuestions <= 0)
                break;

            int questionsToFetch = Math.min(questionsPerSkill, remainingQuestions);
            List<Question> questions = questionRepository.findRandomQuestionsBySkillIdsAndCategory(
                    Collections.singletonList(skillId), 2, questionsToFetch);

            additionalQuestions.addAll(questions);
            remainingQuestions -= questions.size();
        }

        // 아직 부족한 경우 랜덤으로 추가
        if (remainingQuestions > 0) {
            additionalQuestions.addAll(getRandomQuestions(remainingQuestions));
        }

        return additionalQuestions;
    }

    private List<Question> getRandomQuestions(int count) {
        List<Long> allSkillIds = skillRepository.findAllIds();
        return questionRepository.findRandomQuestionsBySkillIdsAndCategory(allSkillIds, 2, count);
    }

    private List<Question> getPreviewTestWeakQuestions(Long userId) {
        List<SkillLevel> weakSkillLevels = skillLevelRepository.findTop3ByUserIdOrderByCurrentAccuracyAsc(userId);
        List<Long> weakSkillIds = weakSkillLevels.stream()
                .map(skillLevel -> skillLevel.getSkill().getId())
                .collect(Collectors.toList());

        return questionRepository.findRandomQuestionsBySkillIdsAndCategory(weakSkillIds, 2, 5); // 데일리 카테고리, 5문제
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
                10 // 데일리 테스트 문제 수
        );
    }

    private List<Question> getWeakConceptQuestionsForWeek(Long userId, UserDaily todayPlan) {
        int currentDay = Integer.parseInt(todayPlan.getDayNumber().replace("Day", ""));
        int startDay = Math.max(1, currentDay - 5); // Day1 미만으로 내려가지 않도록

        List<UserActivity> activities = userActivityRepository.findByUserIdAndTestInfoBetween(
                userId,
                "데일리 테스트 - Day" + startDay,
                "데일리 테스트 - Day" + (currentDay - 1));

        Map<Long, SkillAccuracy> skillAccuracyMap = new HashMap<>();

        for (UserActivity activity : activities) {
            Long skillId = activity.getQuestion().getSkill().getId();
            skillAccuracyMap.computeIfAbsent(skillId, k -> new SkillAccuracy())
                    .addResult(activity.isCorrection());
        }

        List<Long> weakSkillIds = skillAccuracyMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> e.getValue().getAccuracy()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (weakSkillIds.isEmpty()) {
            // 만약 이번 주에 푼 문제가 없다면, 모든 스킬에서 선택
            weakSkillIds = skillRepository.findAllIds();
        }

        return questionRepository.findRandomQuestionsBySkillIdsAndCategory(weakSkillIds, 2, 5); // 데일리 카테고리, 5문제
    }

    private static class SkillAccuracy {
        private int correct = 0;
        private int total = 0;

        public void addResult(boolean isCorrect) {
            if (isCorrect)
                correct++;
            total++;
        }

        public double getAccuracy() {
            return total == 0 ? 0 : (double) correct / total;
        }
    }

    private List<Question> getQuestionsBasedOnPredictions(List<Double> predictions) {
        List<Long> sortedSkillIds = IntStream.range(0, predictions.size())
                .boxed()
                .sorted(Comparator.comparingDouble(predictions::get))
                .map(Long::valueOf)
                .limit(5)
                .collect(Collectors.toList());

        return questionRepository.findRandomQuestionsBySkillIdsAndCategory(sortedSkillIds, 2, 10); // 데일리 카테고리, 10문제
    }

    /**
     * 데일리 테스트 제출 처리
     * 
     * @param user             현재 사용자
     * @param testSubmitReqDto 테스트 제출 데이터
     * @return 테스트 제출 결과 목록
     */
    @Transactional
    public List<TestRespDto.TestSubmitRespDto> processDailyTestSubmission(Long userId, String dayNumber,
            TestReqDto testSubmitReqDto) {
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("해당 일자의 데일리 플랜을 찾을 수 없습니다."));

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
            userActivity.setCorrection(question.getAnswer().equals(activity.getChecked()));
            userActivity.setDate(LocalDateTime.now());

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

        userDaily.setCompleted(true);
        userDaily.setCompletionDate(LocalDate.now());
        userDailyRepository.save(userDaily);

        return results;
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
     * 오늘의 데일리 테스트 결과 - 문제 상세보기
     * 
     * @param userId     로그인 사용자 아이디
     * @param dayNumber  데일리 정보
     * @param questionId 문제 아이디
     * @return
     */
    public DailyResultDetailDto getDailyResultDetail(Long userId, String dayNumber, Long questionId) {
        UserActivity userActivity = userActivityRepository
                .findByUserIdAndTestInfoAndQuestionId(userId, "데일리 테스트 - " + dayNumber, questionId)
                .orElseThrow(() -> new CustomApiException("해당 문제의 풀이 결과를 찾을 수 없습니다."));

        Question question = userActivity.getQuestion();
        Skill skill = question.getSkill();

        return DailyResultDetailDto.builder()
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
    }
}