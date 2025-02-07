package com.qriz.sqld.service.daily;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.domain.clip.ClipRepository;
import com.qriz.sqld.domain.daily.UserDaily;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanVersionService {
    private static final int MAX_KEEP_VERSIONS = 3;
    private static final int MINIMUM_COMPLETED_DAYS = 5;

    private final UserDailyRepository userDailyRepository;
    private final ClipRepository clipRepository;

    @Transactional
    public void archiveCurrentPlan(Long userId) {
        List<UserDaily> currentPlans = userDailyRepository.findByUserIdAndIsArchivedFalse(userId);
        currentPlans.forEach(plan -> {
            plan.setArchived(true);
            plan.setArchivedAt(LocalDateTime.now());
        });
        userDailyRepository.saveAll(currentPlans);
    }

    @Transactional
    public int getNextVersion(Long userId) {
        return userDailyRepository.findMaxPlanVersionByUserId(userId).orElse(0) + 1;
    }

    @Transactional
    public void cleanupOldVersions(Long userId) {
        List<UserDaily> oldVersions = userDailyRepository.findOldVersionsByUserId(userId, MAX_KEEP_VERSIONS);
        if (!oldVersions.isEmpty()) {
            clipRepository.deleteByUserDailyIn(oldVersions);
            userDailyRepository.deleteAll(oldVersions);
        }
    }

    // 프랜 재생성 가능 여부 검증
    public void validatePlanRegeneration(Long userId) {
        // 최소 학습일 수 검증
        int completedDays = userDailyRepository.countByUserIdAndCompletedTrue(userId);
        if (completedDays < MINIMUM_COMPLETED_DAYS) {
            throw new CustomApiException(
                String.format("플랜 재생성은 최소 %d일 이상 학습 후에 가능합니다. (현재 %d일 완료)", 
                    MINIMUM_COMPLETED_DAYS, completedDays)
            );
        }
    }

    public int getCompletedDaysCount(Long userId) {
        return userDailyRepository.countByUserIdAndCompletedTrue(userId);
    }

    public boolean canRegeneratePlan(Long userId) {
        return getCompletedDaysCount(userId) >= MINIMUM_COMPLETED_DAYS;
    }
}
