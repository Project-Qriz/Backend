package com.qriz.sqld.domain.user;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 아이디
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    // 사용자 성명
    private String nickname;

    // 비밀번호
    private String password;

    // 이메일
    @Column(nullable = true, length = 30)
    private String email;

    // 권한
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserEnum role;

    // 생성일
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 수정일
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 소셜 로그인
    // 어떤 OAuth (google, kakao)
    private String provider;

    // OAuth Key
    private String providerId;

    // 소셜 엑세스 토큰
    private String accessToken;

    // test
    private String name;


    @Builder
    public User(Long id, String username, String nickname, String name, String password, String email, UserEnum role, String provider, String providerId, String accessToken, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.name = name;
        this.password = password;
        this.email = email;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.accessToken = accessToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Object getName() {
        return null;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}