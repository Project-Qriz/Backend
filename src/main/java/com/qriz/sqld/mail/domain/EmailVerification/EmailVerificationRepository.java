package com.qriz.sqld.mail.domain.EmailVerification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 이메일과 인증번호로 미인증 상태의 최신 인증 정보 조회
    Optional<EmailVerification> findByEmailAndAuthNumberAndVerifiedFalse(String email, String authNumber);

    // 특정 이메일의 모든 인증 정보 삭제
    void deleteByEmail(String email);

    // 특정 이메일의 최근 인증 시도 횟수 조회 (최근 10분 이내)
    @Query("SELECT COUNT(e) FROM EmailVerification e WHERE e.email = :email AND e.createdAt > :timeLimit")
    int countRecentVerificationAttempts(@Param("email") String email, @Param("timeLimit") LocalDateTime timeLimit);

    // 만료된 인증 정보 삭제
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiryDate < :now")
    void deleteExpiredVerifications(@Param("now") LocalDateTime now);

    // 인증 완료된 데이터 중 오래된 데이터 삭제
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.verified = true AND e.verifiedAt < :before")
    void deleteOldVerifiedData(@Param("before") LocalDateTime before);

    // 특정 이메일의 가장 최근 인증 정보 조회
    Optional<EmailVerification> findFirstByEmailOrderByCreatedAtDesc(String email);

    // 특정 시간 이후에 생성된 인증 요청 수 조회
    @Query("SELECT COUNT(e) FROM EmailVerification e WHERE e.email = :email AND e.createdAt > :since")
    int countVerificationsSince(@Param("email") String email, @Param("since") LocalDateTime since);

    // 최근 실패한 인증 시도 조회
    List<EmailVerification> findByEmailAndVerifiedFalseAndCreatedAtAfter(String email, LocalDateTime since);
}