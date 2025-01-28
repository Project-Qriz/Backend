package com.qriz.sqld.config.auth;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class RefreshToken {
    
    @Id
    private Long userId;
    
    @Column(length = 500)
    private String token;
    
    @Column
    private LocalDateTime expiryDate;

    public RefreshToken(Long userId, String token, LocalDateTime expiryDate) {
        this.userId = userId;
        this.token = token;
        this.expiryDate = expiryDate;
    }
    
    public void updateToken(String token, LocalDateTime expiryDate) {
        this.token = token;  // 기존 토큰을 새로운 토큰으로 덮어씀
        this.expiryDate = expiryDate;
    }
}
