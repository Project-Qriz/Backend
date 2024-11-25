package com.qriz.sqld.domain.question;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.qriz.sqld.domain.skill.Skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    // 문제 유형 외래 키
    @ManyToOne
    @JoinColumn(name = "skill_id")
    private Skill skill;

    // 문제 유형 (1: preview / 2: Daily / 3: exam)
    private int category;

    // 모의고사 회차 정보
    private String examSession;

    // 문항
    @Column(columnDefinition = "LONGTEXT")
    private String question;

    // 선택지 1
    @Column(columnDefinition = "LONGTEXT")
    private String option1;

    // 선택지 2
    @Column(columnDefinition = "LONGTEXT")
    private String option2;

    // 선택지 3
    @Column(columnDefinition = "LONGTEXT")
    private String option3;

    // 선택지 4
    @Column(columnDefinition = "LONGTEXT")
    private String option4;

    // 정답 (선택지 1/선택지 2/선택지 3/선택지 4)
    @Column(columnDefinition = "LONGTEXT")
    private String answer;

    // 해설
    @Column(columnDefinition = "LONGTEXT")
    private String solution;

    // 난이도
    private Integer difficulty;

    // 제한 시간
    private Integer timeLimit;
}