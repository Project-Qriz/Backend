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
@Entity
public class Application {
    
    @Id
    @Column(name = "apply_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 시험장
    private String location;

    // 시험 날짜
    private LocalDate date;

    // 시험 시간
    private String testTime;

    @OneToMany(mappedBy = "application")
    private List<UserApply> userApplies;

    public Application(String location, LocalDate date, String testTime) {
        this.location = location;
        this.date = date;
        this.testTime = testTime;
    }

    public void updateTestTime(String startTime, String endTime) {
        this.testTime = startTime + " ~ " + endTime;
    }
}