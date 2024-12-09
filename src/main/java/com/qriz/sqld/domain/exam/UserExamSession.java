package com.qriz.sqld.domain.exam;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.data.annotation.CreatedDate;

import com.qriz.sqld.domain.user.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class UserExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    private Long id;

    // 사용자 외래 키
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // 회차 정보 (1회차, 2회차 등)
    private String session;

    // 과목별 점수 저장
    private Double subject1Score;  // 1과목 점수
    private Double subject2Score;  // 2과목 점수

    // 시도 횟수
    @Column(nullable = false)
    private Integer attemptCount;

    // 완료 날짜
    @CreatedDate
    private LocalDateTime completionDate;
}