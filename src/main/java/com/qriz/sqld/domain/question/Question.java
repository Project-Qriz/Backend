package com.qriz.sqld.domain.question;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.qriz.sqld.domain.question.option.Option;
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

    // 문제 본문
    @Column(columnDefinition = "LONGTEXT")
    private String question;

    // 부가 설명 (상황, 테이블 구조 등)
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    // 해설
    @Column(columnDefinition = "LONGTEXT")
    private String solution;

    // 난이도
    private Integer difficulty;

    // 제한 시간
    private Integer timeLimit;

    // 선택지 리스트
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();

    /**
     * 헬퍼 메서드: 선택지를 optionOrder 기준으로 정렬하여 반환
     */
    public List<Option> getSortedOptions() {
        return options.stream()
                .sorted(Comparator.comparingInt(Option::getOptionOrder))
                .collect(Collectors.toList());
    }
}