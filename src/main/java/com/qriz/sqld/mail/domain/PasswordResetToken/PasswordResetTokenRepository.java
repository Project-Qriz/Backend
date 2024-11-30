package com.qriz.sqld.mail.domain.PasswordResetToken;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByEmailAndTokenAndUsedFalse(String email, String token);
    void deleteByEmail(String email);
}
