package com.qriz.sqld.dto.recommend;

import com.qriz.sqld.domain.question.Question;

import lombok.Getter;
import lombok.Setter;

public class RecommendRespDto {

    @Getter
    @Setter
    public static class DailyRespDto {
        private Long questionId;
        private Long skillId;
        private int category;
        private String question;
        private String option1;
        private String option2;
        private String option3;
        private String option4;
        private int timeLimit;
        private int difficulty;

        public DailyRespDto(Question question) {
            this.questionId = question.getId();
            this.skillId = question.getSkill().getId();
            this.category = question.getCategory();
            this.question = question.getQuestion();
            this.option1 = question.getOption1();
            this.option2 = question.getOption2();
            this.option3 = question.getOption3();
            this.option4 = question.getOption4();
            this.timeLimit = question.getTimeLimit();
            this.difficulty = question.getDifficulty();
        }
    }
}
