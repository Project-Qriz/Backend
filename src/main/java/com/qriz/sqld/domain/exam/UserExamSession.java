package com.qriz.sqld.domain.exam;

import java.time.LocalDate;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
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

    // 완료 여부
    private boolean completed;

    // 완료 횟수
    private int completedCount;

    // 완료 날짜
    @CreatedDate
    private LocalDate completionDate;

    public void updateTestStatus(boolean isCompleted) {
        this.completedCount++;
        this.completed = true;
        this.completionDate = LocalDate.now();
    }
}