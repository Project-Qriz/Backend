package com.qriz.sqld.domain.question.option;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.qriz.sqld.domain.question.Question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "options")
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Long id;

    // 문제와 다대일 관계 (여러 선택지가 하나의 문제에 속함)
    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    // 선택지 내용
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    // 선택지 순서 (예: 1, 2, 3, 4)
    private int optionOrder;

    // 정답 여부 (true인 경우 정답)
    private boolean isAnswer;
}