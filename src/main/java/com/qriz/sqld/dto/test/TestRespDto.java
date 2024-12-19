package com.qriz.sqld.dto.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.qriz.sqld.domain.question.Question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class TestRespDto {

    @Getter
    @Setter
    @NoArgsConstructor
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
        // 옵션의 원래 순서를 저장하기 위한 필드 추가
        private List<Integer> optionOrder;

        @Builder
        public DailyRespDto(Long questionId, Long skillId, int category, String question,
                String description, String option1, String option2,
                String option3, String option4, int timeLimit,
                int difficulty, List<Integer> optionOrder) {
            this.questionId = questionId;
            this.skillId = skillId;
            this.category = category;
            this.question = question;
            this.description = description;
            this.option1 = option1;
            this.option2 = option2;
            this.option3 = option3;
            this.option4 = option4;
            this.timeLimit = timeLimit;
            this.difficulty = difficulty;
            this.optionOrder = optionOrder;
        }

        public DailyRespDto(Question question) {
            QuestionOptions randomizedOptions = QuestionOptions.createRandomized(
                    question.getOption1(),
                    question.getOption2(),
                    question.getOption3(),
                    question.getOption4());

            this.questionId = question.getId();
            this.skillId = question.getSkill().getId();
            this.category = question.getCategory();
            this.question = question.getQuestion();
            this.description = question.getDescription();
            this.option1 = randomizedOptions.getOption1();
            this.option2 = randomizedOptions.getOption2();
            this.option3 = randomizedOptions.getOption3();
            this.option4 = randomizedOptions.getOption4();
            this.timeLimit = question.getTimeLimit();
            this.difficulty = question.getDifficulty();
            this.optionOrder = randomizedOptions.getOriginalOrder();
        }

        // 원본 순서로 선택지를 생성하는 메서드
        public static DailyRespDto createWithOriginalOrder(Question question) {
            return new DailyRespDto(
                    question.getId(),
                    question.getSkill().getId(),
                    question.getCategory(),
                    question.getQuestion(),
                    question.getDescription(),
                    question.getOption1(),
                    question.getOption2(),
                    question.getOption3(),
                    question.getOption4(),
                    question.getTimeLimit(),
                    question.getDifficulty(),
                    Arrays.asList(1, 2, 3, 4));
        }
    }

    // 선택지 랜덤화를 위한 내부 클래스
    @Getter
    private static class QuestionOptions {
        private final String option1;
        private final String option2;
        private final String option3;
        private final String option4;
        private final List<Integer> originalOrder;

        private QuestionOptions(List<String> randomizedOptions, List<Integer> originalOrder) {
            this.option1 = randomizedOptions.get(0);
            this.option2 = randomizedOptions.get(1);
            this.option3 = randomizedOptions.get(2);
            this.option4 = randomizedOptions.get(3);
            this.originalOrder = originalOrder;
        }

        public static QuestionOptions createRandomized(String... options) {
            List<OptionWithIndex> optionsWithIndices = new ArrayList<>();
            for (int i = 0; i < options.length; i++) {
                optionsWithIndices.add(new OptionWithIndex(options[i], i + 1));
            }

            Collections.shuffle(optionsWithIndices);

            List<String> randomizedOptions = optionsWithIndices.stream()
                    .map(OptionWithIndex::getOption)
                    .collect(Collectors.toList());

            List<Integer> originalOrder = optionsWithIndices.stream()
                    .map(OptionWithIndex::getOriginalIndex)
                    .collect(Collectors.toList());

            return new QuestionOptions(randomizedOptions, originalOrder);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class OptionWithIndex {
        private final String option;
        private final int originalIndex;
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
