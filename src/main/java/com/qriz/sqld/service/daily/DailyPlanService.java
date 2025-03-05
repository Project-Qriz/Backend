package com.qriz.sqld.service.daily;

import com.qriz.sqld.domain.UserActivity.UserActivity;
import com.qriz.sqld.domain.UserActivity.UserActivityRepository;
import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.domain.preview.PreviewTestStatus;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.daily.UserDailyDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.util.WeekendPlanUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.AbstractMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DailyPlanService {

    private final UserDailyRepository userDailyRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final WeekendPlanUtil weekendPlanUtil;
    private final UserActivityRepository userActivityRepository;
    private final DKTService dktService;
    private final PlanVersionService planVersionService;

    private final Logger log = LoggerFactory.getLogger(DailyPlanService.class);

    @Transactional
    public void generateDailyPlan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException("User not found"));
        LocalDate startDate = LocalDate.now();
        List<UserDaily> dailyPlans = buildDailyPlansForUser(user, startDate, 0, false);
        userDailyRepository.saveAll(dailyPlans);
    }

    /**
     * 공통 플랜 생성 로직: 신규 플랜, 재생성 플랜 모두에 사용.
     *
     * @param user      사용자
     * @param startDate 플랜 시작일
     * @param version   플랜 버전 (신규 플랜인 경우 0, 재생성 시 증가된 값)
     * @param archived  아카이브 여부 (재생성 전의 플랜은 true)
     * @return 생성된 UserDaily 리스트
     */
    private List<UserDaily> buildDailyPlansForUser(User user, LocalDate startDate, int version, boolean archived) {
        // frequency 내림차순 정렬
        List<Skill> sortedSkills = skillRepository.findAll().stream()
                .sorted(Comparator.comparing(Skill::getFrequency).reversed())
                .collect(Collectors.toList());

        List<UserDaily> dailyPlans = new ArrayList<>();
        for (int day = 1; day <= 30; day++) {
            UserDaily userDaily = new UserDaily();
            userDaily.setUser(user);
            userDaily.setDayNumber("Day" + day);
            userDaily.setCompleted(false);
            userDaily.setPlanDate(startDate.plusDays(day - 1));
            userDaily.setPlanVersion(version);
            userDaily.setArchived(archived);

            if (day <= 21) { // Week 1-3
                if (day % 7 == 6 || day % 7 == 0) {
                    // 주말: 복습일
                    userDaily.setPlannedSkills(new ArrayList<>());
                    userDaily.setReviewDay(true);
                } else {
                    int weekday = getWeekdayCount(day);
                    if (weekday > 0 && weekday <= 15) {
                        int skillIndex = (weekday - 1) * 2;
                        // 인덱스 범위 체크
                        if (skillIndex + 1 < sortedSkills.size()) {
                            List<Skill> daySkills = Arrays.asList(
                                    sortedSkills.get(skillIndex),
                                    sortedSkills.get(skillIndex + 1));
                            userDaily.setPlannedSkills(daySkills);
                        } else {
                            userDaily.setPlannedSkills(new ArrayList<>());
                        }
                    }
                    userDaily.setReviewDay(false);
                }
            } else {
                // Week 4: 종합 복습 및 모의 테스트 준비 (plannedSkills는 null 대신 빈 리스트 사용)
                userDaily.setPlannedSkills(new ArrayList<>());
                userDaily.setReviewDay(false);
                userDaily.setComprehensiveReviewDay(true);
            }
            dailyPlans.add(userDaily);
        }
        return dailyPlans;
    }

    /**
     * 주어진 day 숫자(1~30) 중 평일 수를 반환 (주말 제외).
     */
    private int getWeekdayCount(int day) {
        if (day % 7 == 6 || day % 7 == 0)
            return 0; // 주말

        int week = (day - 1) / 7; // 0-based week
        int dayInWeek = day % 7;
        if (dayInWeek == 0)
            dayInWeek = 7;
        return week * 5 + dayInWeek;
    }

    public boolean canAccessDay(Long userId, String currentDayNumber) {
        int currentDay = Integer.parseInt(currentDayNumber.replace("Day", ""));
        if (currentDay == 1)
            return true;
        String previousDayNumber = "Day" + (currentDay - 1);
        UserDaily previousDay = userDailyRepository.findByUserIdAndDayNumber(userId, previousDayNumber)
                .orElseThrow(() -> new CustomApiException("Previous day plan not found"));

        // 이전 Day가 완료되지 않았으면 접근 불가
        if (!previousDay.isCompleted()) {
            return false;
        }

        // 이전 Day가 오늘 완료되었으면 접근 불가
        if (previousDay.getCompletionDate() != null &&
                previousDay.getCompletionDate().equals(LocalDate.now())) {
            return false;
        }

        return true;
    }

    @Transactional
    public void completeDailyPlan(Long userId, String dayNumber) {
        log.info("Completing daily plan for user {} and day {}", userId, dayNumber);
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new CustomApiException("Daily plan not found"));

        userDaily.setCompleted(true);
        userDaily.setCompletionDate(LocalDate.now());
        userDailyRepository.save(userDaily);

        int day = Integer.parseInt(dayNumber.replace("Day", ""));
        if (day % 7 == 5 && day <= 19) {
            log.info("Updating weekend plan for day: {}", day);
            updateWeekendPlan(userId, day);
        }
        log.info("Daily plan completed for user {} and day {}", userId, dayNumber);
    }

    @Transactional
    public void updateWeekendPlan(Long userId, int currentDay) {
        log.info("Updating weekend plan for user {} and currentDay {}", userId, currentDay);
        UserDaily day6 = userDailyRepository.findByUserIdAndDayNumber(userId, "Day" + (currentDay + 1))
                .orElseThrow(() -> new CustomApiException("Day " + (currentDay + 1) + " plan not found"));
        UserDaily day7 = userDailyRepository.findByUserIdAndDayNumber(userId, "Day" + (currentDay + 2))
                .orElseThrow(() -> new CustomApiException("Day " + (currentDay + 2) + " plan not found"));

        day6.setReviewDay(true);
        day7.setReviewDay(true);

        List<Skill> day6Skills = new ArrayList<>(weekendPlanUtil.getWeekendPlannedSkills(userId, day6));
        List<Skill> day7Skills = new ArrayList<>(weekendPlanUtil.getWeekendPlannedSkills(userId, day7));

        day6.setPlannedSkills(day6Skills);
        day7.setPlannedSkills(day7Skills);

        userDailyRepository.save(day6);
        userDailyRepository.save(day7);

        log.info("Weekend plan updated for days {} and {}", currentDay + 1, currentDay + 2);
    }

    @Transactional
    public void updateWeekFourPlan(Long userId) {
        log.info("Starting updateWeekFourPlan for user {}", userId);
        List<UserDaily> weekFourPlans = userDailyRepository.findByUserIdAndDayNumberBetween(userId, "Day22", "Day30");

        if (weekFourPlans.isEmpty()) {
            log.warn("Week four plans not found for user {}", userId);
            return;
        }

        // Week 4 기간 계산: 1~3주차 활동을 기반으로 예측
        LocalDate startDate = weekFourPlans.get(0).getPlanDate().minusWeeks(3);
        LocalDate endDate = weekFourPlans.get(weekFourPlans.size() - 1).getPlanDate();
        List<UserActivity> userActivities = userActivityRepository.findByUserIdAndDateBetween(
                userId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));

        log.info("Found {} user activities for week four plan", userActivities.size());

        List<Double> predictions = dktService.getPredictions(userId, userActivities);
        log.info("Received predictions: {}", predictions);

        List<Skill> recommendedSkills;
        if (predictions.isEmpty() || predictions.stream().allMatch(p -> p == 0.0)) {
            log.warn("Predictions are empty or all zero for user {}. Using default skills.", userId);
            recommendedSkills = getDefaultSkills();
        } else {
            recommendedSkills = getRecommendedSkills(predictions);
        }

        log.info("Recommended skills: {}",
                recommendedSkills.stream().map(Skill::getKeyConcepts).collect(Collectors.toList()));

        int totalDays = 9; // Day22 ~ Day30
        int totalSkills = recommendedSkills.size();

        for (int i = 0; i < totalDays; i++) {
            String dayNumber = "Day" + (i + 22);
            UserDaily userDaily = weekFourPlans.stream()
                    .filter(plan -> plan.getDayNumber().equals(dayNumber))
                    .findFirst()
                    .orElse(null);

            if (userDaily == null) {
                log.warn("Plan for {} not found", dayNumber);
                continue;
            }

            int skillIndex = i % totalSkills; // 순환 선택
            Skill dailySkill = recommendedSkills.get(skillIndex);

            log.info("Updating UserDaily {} with skill: {}", userDaily.getDayNumber(), dailySkill.getKeyConcepts());
            updateSingleUserDaily(userDaily.getId(), Collections.singletonList(dailySkill));
        }

        log.info("Completed updateWeekFourPlan for user: {}", userId);
    }

    private List<Skill> getDefaultSkills() {
        List<Skill> allSkills = skillRepository.findAllByOrderByFrequencyDesc();
        return allSkills.stream().limit(9).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSingleUserDaily(Long userDailyId, List<Skill> skills) {
        UserDaily userDaily = userDailyRepository.findById(userDailyId)
                .orElseThrow(() -> new CustomApiException("UserDaily not found"));
        userDaily.setPlannedSkills(new ArrayList<>(skills));
        userDaily.setReviewDay(false);
        userDailyRepository.save(userDaily);
        log.info("Updated UserDaily {} with {} skills", userDaily.getDayNumber(), skills.size());
    }

    /**
     * 예측 결과(predictions)와 전체 스킬 리스트를 짝지어 하위 9개를 선택함.
     */
    private List<Skill> getRecommendedSkills(List<Double> predictions) {
        log.info("Getting recommended skills based on predictions: {}", predictions);

        List<Skill> allSkills = skillRepository.findAll();
        if (allSkills.isEmpty()) {
            log.warn("No skills found in the database");
            return Collections.emptyList();
        }

        int minSize = Math.min(predictions.size(), allSkills.size());
        int limitSize = Math.min(9, minSize);

        return IntStream.range(0, minSize)
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(allSkills.get(i), predictions.get(i)))
                .sorted(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue))
                .limit(limitSize)
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());
    }

    public LocalDate getPlanStartDate(LocalDate currentDate) {
        long daysSinceEpoch = ChronoUnit.DAYS.between(LocalDate.EPOCH, currentDate);
        long daysToSubtract = daysSinceEpoch % 30;
        return currentDate.minusDays(daysToSubtract);
    }

    public boolean isWeekFour(LocalDate date) {
        LocalDate startDate = getPlanStartDate(date);
        return ChronoUnit.WEEKS.between(startDate, date) == 3;
    }

    @Transactional(readOnly = true)
    public List<UserDailyDto> getUserDailyPlan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));

        // 프리뷰 테스트 상태 확인
        PreviewTestStatus status = user.getPreviewTestStatus();

        if (status == PreviewTestStatus.NOT_STARTED) {
            throw new CustomApiException("설문조사를 먼저 진행해 주세요.");
        } else if (status == PreviewTestStatus.SURVEY_COMPLETED) {
            throw new CustomApiException("프리뷰 테스트를 완료해 주세요.");
        }

        List<UserDaily> dailyPlans = userDailyRepository.findByUserIdWithPlannedSkillsOrderByPlanDateAsc(userId);
        return dailyPlans.stream()
                .map(UserDailyDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void regenerateDailyPlan(Long userId) {
        // 1. 재생성 조건 검증 (최소 학습일 및 마지막 재생성 후 3일 경과 여부)
        planVersionService.validatePlanRegeneration(userId);

        // 2. 오래된 버전 정리 (최대 3버전 유지)
        planVersionService.cleanupOldVersions(userId);

        // 3. 현재 플랜 아카이브
        planVersionService.archiveCurrentPlan(userId);

        // 4. 새로운 버전 생성 (동시성 문제를 고려하여 synchronized 처리)
        int newVersion = planVersionService.getNextVersion(userId);
        LocalDate startDate = LocalDate.now();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException("User not found"));

        List<UserDaily> newPlans = buildDailyPlansForUser(user, startDate, newVersion, false);
        userDailyRepository.saveAll(newPlans);
    }
}
