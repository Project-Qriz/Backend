package com.qriz.sqld.domain.daily;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.springframework.data.annotation.CreatedDate;

import com.qriz.sqld.domain.skill.Skill;
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
public class UserDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_id")
    private Long id;

    // 사용자 외래 키
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // 데일리 정보 (Day1, Day2 등)
    private String dayNumber;

    // 완료 여부
    private boolean completed;

    // 완료 날짜
    @CreatedDate
    private LocalDate completionDate;

    // 복습날짜 확인
    private boolean reviewDay;

    // 종합 복습 날짜 여부
    private boolean comprehensiveReviewDay;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_daily_skills",
            joinColumns = @JoinColumn(name = "daily_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private List<Skill> plannedSkills = new ArrayList<>();

    private LocalDate planDate;

    // 통과 여부
    private boolean passed;

    // 시도횟수
    private int attemptCount;

    // 재시험 가능 여부
    private boolean retestEligible;

    // 플랜 버전
    private int planVersion = 1;

    // 아카이브 여부 (이전 버전 구분)
    private boolean isArchived = false;

    // 아카이브된 시점
    private LocalDateTime archivedAt;

    public void updateTestStatus(boolean isPassed) {
        this.attemptCount++;
        this.passed = isPassed;
        this.completed = true;
        this.completionDate = LocalDate.now();

        if (!isPassed && attemptCount == 1) {
            this.retestEligible = true;
        } else {
            this.retestEligible = false;
        }
    }
}