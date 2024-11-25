package com.qriz.sqld.mail.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_email_authNumber", columnList = "email,authNumber")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String authNumber;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private int attemptCount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        attemptCount = 0;
        verified = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public void verify() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    // 최대 시도 횟수 (예: 5회)를 초과했는지 확인
    public boolean hasExceededMaxAttempts() {
        return this.attemptCount >= 5;
    }
}
