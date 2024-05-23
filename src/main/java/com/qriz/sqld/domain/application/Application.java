package com.qriz.sqld.domain.application;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.qriz.sqld.domain.apply.UserApply;

import java.util.List;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class Application {
    
    @Id
    @Column(name = "apply_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 시험 접수 시작 날짜
    private LocalDate startDate;

    // 시험 접수 종료 날짜
    private LocalDate endDate;

    // 시험 날짜
    private LocalDate examDate;

    // 시험 시간
    private String testTime;

    @OneToMany(mappedBy = "application")
    private List<UserApply> userApplies;

    public Application(LocalDate startDate, LocalDate endDate, LocalDate examDate, String testTime) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.examDate = examDate;
        this.testTime = testTime;
    }

    public void updateTestTime(String startTime, String endTime) {
        this.testTime = startTime + " ~ " + endTime;
    }
}