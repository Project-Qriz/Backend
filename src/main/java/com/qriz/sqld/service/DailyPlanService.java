package com.qriz.sqld.service;

import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.domain.skill.Skill;
import com.qriz.sqld.domain.skill.SkillRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.daily.UserDailyDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyPlanService {

    private final UserDailyRepository userDailyRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

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

    @Transactional(readOnly = true)
    public List<UserDailyDto> getUserDailyPlan(Long userId) {
        List<UserDaily> dailyPlans = userDailyRepository.findByUserIdWithPlannedSkillsOrderByPlanDateAsc(userId);
        return dailyPlans.stream()
                .map(UserDailyDto::new)
                .distinct() // 중복 제거
                .collect(Collectors.toList());
    }

    @Transactional
    public void completeDailyPlan(Long userId, String dayNumber) {
        UserDaily userDaily = userDailyRepository.findByUserIdAndDayNumber(userId, dayNumber)
                .orElseThrow(() -> new RuntimeException("Daily plan not found"));

        userDaily.setCompleted(true);
        userDaily.setCompletionDate(LocalDate.now());
        userDailyRepository.save(userDaily);
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
}