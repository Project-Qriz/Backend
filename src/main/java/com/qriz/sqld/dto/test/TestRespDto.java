package com.qriz.sqld.dto.test;

import com.qriz.sqld.domain.question.Question;

import lombok.Getter;
import lombok.Setter;

public class TestRespDto {

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

    @Getter
    @Setter
    public static class TestSubmitRespDto {
        private Long activityId;
        private Long userId;
        private QuestionRespDto question;
        private int questionNum;
        private String checked;
        private Integer timeSpent;
        private boolean correction;

        @Getter
        @Setter
        public static class QuestionRespDto {
            private Long questionId;
            private String category;
            
            public QuestionRespDto(Long questionId, String category) {
                this.questionId = questionId;
                this.category = category;
            }
        }

        public TestSubmitRespDto(Long activityId, Long userId, QuestionRespDto question, int questionNum, String checked, Integer timeSpent, boolean correction) {
            this.activityId = activityId;
            this.userId = userId;
            this.question = question;
            this.questionNum = questionNum;
            this.checked = checked;
            this.timeSpent = timeSpent;
            this.correction = correction;
        }
    }
}
