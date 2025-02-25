package com.qriz.sqld.dto.preview;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.option.Option;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionDto {
    private Long questionId;
    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private Integer timeLimit;

    public static QuestionDto from(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setQuestionId(question.getId());
        dto.setQuestion(question.getQuestion());
        dto.setTimeLimit(question.getTimeLimit());

        // Option 엔티티 리스트를 getSortedOptions()를 통해 가져오고,
        List<Option> sortedOptions = question.getSortedOptions();

        dto.setOption1(sortedOptions.size() > 0 ? sortedOptions.get(0).getContent() : null);
        dto.setOption2(sortedOptions.size() > 1 ? sortedOptions.get(1).getContent() : null);
        dto.setOption3(sortedOptions.size() > 2 ? sortedOptions.get(2).getContent() : null);
        dto.setOption4(sortedOptions.size() > 3 ? sortedOptions.get(3).getContent() : null);

        return dto;
    }
}
