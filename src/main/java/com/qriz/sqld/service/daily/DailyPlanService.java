package com.qriz.sqld.service.daily;

import com.qriz.sqld.domain.UserActivity.UserActivity;
import com.qriz.sqld.domain.UserActivity.UserActivityRepository;
import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.daily.UserDailyDto;
import com.qriz.sqld.util.WeekendPlanUtil;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;
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

    private final Logger log = LoggerFactory.getLogger(DailyPlanService.class);

    @Transactional
    public void generateDailyPlan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Skill> allSkills = skillRepository.findAllByOrderByFrequencyDesc();
        LocalDate startDate = LocalDate.now();

        List<UserDaily> dailyPlans = new ArrayList<>();

        for (int day = 1; day <= 30; day++) {
            UserDaily userDaily = new UserDaily();
            userDaily.setUser(user);
            userDaily.setDayNumber("Day" + day);
            userDaily.setCompleted(false);
            userDaily.setPlanDate(startDate.plusDays(day - 1));

            if (day <= 21) {
                if (day % 7 == 6 || day % 7 == 0) {
                    // 주말 (6일, 7일차): 복습
                    userDaily.setPlannedSkills(new ArrayList<>());
                    userDaily.setReviewDay(true);
                } else {
                    // 평일: 출제 빈도수에 기반한 개념
                    int skillIndex = (day - 1) * 2 % allSkills.size();
                    Skill skill1 = allSkills.get(skillIndex);
                    Skill skill2 = allSkills.get((skillIndex + 1) % allSkills.size());
                    userDaily.setPlannedSkills(List.of(skill1, skill2));
                    userDaily.setReviewDay(false);
                }
            } else {
                // Week 4: DKT 기반 문제 추천을 위한 준비
                userDaily.setPlannedSkills(null); // null로 설정하여 DKT 기반 추천임을 나타냄
                userDaily.setReviewDay(false); // 리뷰 데이가 아님을 나타냄
            }

            dailyPlans.add(userDaily);
        }

        userDailyRepository.saveAll(dailyPlans);
    }

    public boolean canAccessDay(Long userId, String currentDayNumber) {
        int currentDay = Integer.parseInt(currentDayNumber.replace("Day", ""));

        if (currentDay == 1)
            return true;

        String previousDayNumber = "Day" + (currentDay - 1);
        UserDaily previousDay = userDailyRepository.findByUserIdAndDayNumber(userId, previousDayNumber)
                .orElseThrow(() -> new RuntimeException("Previous day plan not found"));

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
                .orElseThrow(() -> new RuntimeException("Daily plan not found"));

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
                .orElseThrow(() -> new RuntimeException("Day " + (currentDay + 1) + " plan not found"));
        UserDaily day7 = userDailyRepository.findByUserIdAndDayNumber(userId, "Day" + (currentDay + 2))
                .orElseThrow(() -> new RuntimeException("Day " + (currentDay + 2) + " plan not found"));

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

        List<UserActivity> userActivities = userActivityRepository.findByUserIdAndDateBetween(
                userId,
                weekFourPlans.get(0).getPlanDate().minusWeeks(3).atStartOfDay(),
                weekFourPlans.get(weekFourPlans.size() - 1).getPlanDate().atTime(23, 59, 59));

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

        int totalDays = 9; // Day22부터 Day30까지 9일
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

            int skillIndex = i % totalSkills; // 순환적으로 스킬 선택
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
                .orElseThrow(() -> new RuntimeException("UserDaily not found"));
        userDaily.setPlannedSkills(new ArrayList<>(skills));
        userDaily.setReviewDay(false);
        userDailyRepository.save(userDaily);
        log.info("Updated UserDaily {} with {} skills", userDaily.getDayNumber(), skills.size());
    }

    private List<Skill> getRecommendedSkills(List<Double> predictions) {
        log.info("Getting recommended skills based on predictions: {}", predictions);

        if (predictions.isEmpty()) {
            log.warn("Predictions list is empty");
            return Collections.emptyList();
        }

        // 정답률에 따라 스킬을 정렬하고 하위 9개를 선택 (Day22-Day30, 9일간)
        List<Skill> allSkills = skillRepository.findAll();
        log.info("Total available skills: {}", allSkills.size());

        if (allSkills.isEmpty()) {
            log.warn("No skills found in the database");
            return Collections.emptyList();
        }

        int minSize = Math.min(predictions.size(), allSkills.size());
        int limitSize = Math.min(9, minSize);

        return IntStream.range(0, minSize)
                .boxed()
                .sorted(Comparator.comparingDouble(predictions::get)) // 오름차순 정렬 (낮은 정답률부터)
                .limit(limitSize) // 하위 9개 또는 가능한 최대 개수 선택
                .map(allSkills::get)
                .collect(Collectors.toList());
    }

    public LocalDate getPlanStartDate(LocalDate currentDate) {
        // 30일 플랜의 시작 날짜를 계산
        // 현재 날짜로부터 가장 가까운 과거의 30의 배수 날짜를 찾아 반환
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
        List<UserDaily> dailyPlans = userDailyRepository.findByUserIdWithPlannedSkillsOrderByPlanDateAsc(userId);
        return dailyPlans.stream()
                .map(UserDailyDto::new)
                .collect(Collectors.toList());
    }
}