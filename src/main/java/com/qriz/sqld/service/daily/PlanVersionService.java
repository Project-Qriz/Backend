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
    private static final int MINIMUM_DAYS_BETWEEN_REGENERATION = 3;

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
    public synchronized int getNextVersion(Long userId) {
        return userDailyRepository.findMaxPlanVersionByUserId(userId).orElse(0) + 1;
    }

    @Transactional
    public void cleanupOldVersions(Long userId) {
        List<UserDaily> oldVersions = userDailyRepository.findOldVersionsByUserId(userId, MAX_KEEP_VERSIONS);
        if (!oldVersions.isEmpty()) {
            // DKT 모델 등 다른 분석에 필요한 데이터가 있다면 별도 보관 고려
            clipRepository.deleteByUserDailyIn(oldVersions);
            userDailyRepository.deleteAll(oldVersions);
        }
    }

    /**
     * 재생성 가능 여부 검증:
     * - 최소 완료일 수 (MINIMUM_COMPLETED_DAYS) 이상
     * - 마지막 재생성(아카이브) 후 최소 MINIMUM_DAYS_BETWEEN_REGENERATION 일 경과 여부
     */
    public void validatePlanRegeneration(Long userId) {
        int completedDays = userDailyRepository.countByUserIdAndCompletedTrue(userId);
        if (completedDays < MINIMUM_COMPLETED_DAYS) {
            throw new CustomApiException(String.format("플랜 재생성은 최소 %d일 이상 학습 후에 가능합니다. (현재 %d일 완료)",
                    MINIMUM_COMPLETED_DAYS, completedDays));
        }
        List<UserDaily> archivedPlans = userDailyRepository.findByUserIdAndArchivedTrueOrderByArchivedAtDesc(userId);
        if (!archivedPlans.isEmpty()) {
            LocalDateTime lastArchivedAt = archivedPlans.get(0).getArchivedAt();
            if (lastArchivedAt != null
                    && lastArchivedAt.plusDays(MINIMUM_DAYS_BETWEEN_REGENERATION).isAfter(LocalDateTime.now())) {
                throw new CustomApiException("재생성은 마지막 재생성 후 최소 " + MINIMUM_DAYS_BETWEEN_REGENERATION + "일 후에 가능합니다.");
            }
        }
    }

    public int getCompletedDaysCount(Long userId) {
        return userDailyRepository.countByUserIdAndCompletedTrue(userId);
    }

    public boolean canRegeneratePlan(Long userId) {
        return getCompletedDaysCount(userId) >= MINIMUM_COMPLETED_DAYS;
    }
}
