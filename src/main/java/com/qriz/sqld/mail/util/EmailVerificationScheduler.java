package com.qriz.sqld.mail.util;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.qriz.sqld.mail.domain.EmailVerification.EmailVerificationRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EmailVerificationScheduler {
    
    private final EmailVerificationRepository verificationRepository;
    
    // 매시간 만료된 인증 정보 삭제
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredVerifications() {
        verificationRepository.deleteExpiredVerifications(LocalDateTime.now());
    }
    
    // 매일 자정에 오래된 인증 완료 데이터 삭제 (30일 이상)
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldVerifiedData() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        verificationRepository.deleteOldVerifiedData(thirtyDaysAgo);
    }
}