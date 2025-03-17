package com.qriz.sqld.dto.preview;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.option.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionDto {
    private Long questionId;
    private Long skillId;
    private int category;
    private String question;
    private String description;
    private List<OptionDto> options;
    private int timeLimit;
    private int difficulty;

    public QuestionDto(Question question, long seed) {
        this.questionId = question.getId();
        this.skillId = (question.getSkill() != null) ? question.getSkill().getId() : 0L;
        this.category = question.getCategory();
        this.question = question.getQuestion();
        this.description = question.getDescription();
        this.timeLimit = (question.getTimeLimit() != null) ? question.getTimeLimit() : 0;
        this.difficulty = (question.getDifficulty() != null) ? question.getDifficulty() : 1;
        List<Option> sortedOptions = question.getSortedOptions();
        List<Option> randomized = new ArrayList<>(sortedOptions);
        Collections.shuffle(randomized, new Random(seed));
        this.options = randomized.stream()
                .map(opt -> new OptionDto(opt.getId(), opt.getContent()))
                .collect(Collectors.toList());
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class OptionDto {
        private Long id;
        private String content;
    }
}
