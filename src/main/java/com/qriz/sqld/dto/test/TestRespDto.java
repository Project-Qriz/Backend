package com.qriz.sqld.dto.test;

import com.qriz.sqld.domain.question.Question;

import lombok.AllArgsConstructor;
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
        private String description;
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
            this.description = question.getDescription();
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

        public TestSubmitRespDto(Long activityId, Long userId, QuestionRespDto question, int questionNum,
                String checked, Integer timeSpent, boolean correction) {
            this.activityId = activityId;
            this.userId = userId;
            this.question = question;
            this.questionNum = questionNum;
            this.checked = checked;
            this.timeSpent = timeSpent;
            this.correction = correction;
        }
    }

    @Getter
    @Setter
    public static class ExamSubmitRespDto {
        private Long activityId;
        private Long userId;
        private QuestionRespDto question;
        private int questionNum;
        private String checked;
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

        public ExamSubmitRespDto(Long activityId, Long userId, QuestionRespDto question, int questionNum,
                String checked, boolean correction) {
            this.activityId = activityId;
            this.userId = userId;
            this.question = question;
            this.questionNum = questionNum;
            this.checked = checked;
            this.correction = correction;
        }
    }

    @Getter
    @Setter
    public static class TestResultRespDto {
        private Long activityId;
        private Long userId;
        private QuestionRespDto question;
        private int questionNum;
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

        public TestResultRespDto(Long activityId, Long userId, QuestionRespDto question, int questionNum,
                boolean correction) {
            this.activityId = activityId;
            this.userId = userId;
            this.question = question;
            this.questionNum = questionNum;
            this.correction = correction;
        }
    }

    @Getter
    @Setter
    public static class TestResultDetailRespDto {
        private ActivityDto activity;
        private Long userId;
        private QuestionDto question;
        private int questionNum;
        private String checked;
        private boolean correction;

        @Getter
        @Setter
        public static class ActivityDto {
            private Long activityId;
            private String testInfo;

            public ActivityDto(Long activityId, String testInfo) {
                this.activityId = activityId;
                this.testInfo = testInfo;
            }
        }

        @Getter
        @Setter
        public static class QuestionDto {
            private Long questionId;
            private Long skillId;
            private String category;
            private String answer;
            private String solution;

            public QuestionDto(Long questionId, Long skillId, String category, String answer, String solution) {
                this.questionId = questionId;
                this.skillId = skillId;
                this.category = category;
                this.answer = answer;
                this.solution = solution;
            }
        }

        public TestResultDetailRespDto(ActivityDto activity, Long userId, QuestionDto question, int questionNum,
                String checked, boolean correction) {
            this.activity = activity;
            this.userId = userId;
            this.question = question;
            this.questionNum = questionNum;
            this.checked = checked;
            this.correction = correction;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ExamRespDto {
        private Long questionId;
        private Long skillId;
        private int category;
        private String question;
        private String description;
        private String option1;
        private String option2;
        private String option3;
        private String option4;

        public ExamRespDto(Question question) {
            this.questionId = question.getId();
            this.skillId = question.getSkill().getId();
            this.category = question.getCategory();
            this.question = question.getQuestion();
            this.description = question.getDescription();
            this.option1 = question.getOption1();
            this.option2 = question.getOption2();
            this.option3 = question.getOption3();
            this.option4 = question.getOption4();
        }
    }
}
