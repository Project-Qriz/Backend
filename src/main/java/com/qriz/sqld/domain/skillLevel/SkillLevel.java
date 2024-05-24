package com.qriz.sqld.domain.skillLevel;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
public class SkillLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_level_id")
    private Long id;

    // 사용자 외래 키
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // 스킬 외래 키
    @ManyToOne
    @JoinColumn(name = "skill_id")
    private Skill skill;

    // 난이도
    private Integer difficulty;

    // 사용자 현재 이해도
    private float currentAccuracy;

    // 사용자의 다음 예측값
    private float predictAccuracy;

    // 갱신 시간
    private LocalDateTime lastUpdated;
}