package com.qriz.sqld.dto.preview;

import com.qriz.sqld.domain.question.Question;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionDto {
    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private Integer timeLimit;

    public static QuestionDto from(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setQuestion(question.getQuestion());
        dto.setOption1(question.getOption1());
        dto.setOption2(question.getOption2());
        dto.setOption3(question.getOption3());
        dto.setOption4(question.getOption4());
        dto.setTimeLimit(question.getTimeLimit());
        return dto;
    }
}
