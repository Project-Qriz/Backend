package com.qriz.sqld.domain.user;

import java.time.LocalDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.qriz.sqld.domain.apply.UserApply;
import com.qriz.sqld.domain.preview.PreviewTestStatus;
import com.qriz.sqld.domain.survey.Survey;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(nullable = true)
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

    @Enumerated(EnumType.STRING)
    private PreviewTestStatus previewTestStatus = PreviewTestStatus.NOT_STARTED;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserApply> userApplies;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Survey> surveys;

    @Builder
    public User(Long id, String username, String nickname, String password, String email, UserEnum role, String provider, String providerId, LocalDateTime createdAt, LocalDateTime updatedAt, PreviewTestStatus previewTestStatus) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.email = email;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.previewTestStatus = previewTestStatus.NOT_STARTED;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void updatePreviewTestStatus(PreviewTestStatus status) {
        this.previewTestStatus = status;
    }
}