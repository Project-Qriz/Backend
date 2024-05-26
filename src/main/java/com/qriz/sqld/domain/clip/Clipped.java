package com.qriz.sqld.domain.clip;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.qriz.sqld.domain.userActivity.UserActivity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class Clipped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clipped_id")
    private Long id;

    // 사용자 문제 풀이 정보 외래 키
    @ManyToOne
    @JoinColumn(name = "activity_id")
    private UserActivity userActivity;

    // 클립한 날짜
    private LocalDateTime date;
}