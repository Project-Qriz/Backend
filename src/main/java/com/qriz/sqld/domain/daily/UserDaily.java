package com.qriz.sqld.domain.daily;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
    private LocalDate completionDate;
}