package com.qriz.sqld.dto.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.qriz.sqld.domain.question.Question;
import com.qriz.sqld.domain.question.option.Option;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class TestRespDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private Long id;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DailyRespDto {
        private Long questionId;
        private Long skillId;
        private int category;
        private String question;
        private String description;
        private List<OptionDto> options;
        private int timeLimit;
        private int difficulty;
    
        public DailyRespDto(Question questionEntity, long seed) {
            this.questionId = questionEntity.getId();
            this.skillId = questionEntity.getSkill().getId();
            this.category = questionEntity.getCategory();
            this.question = questionEntity.getQuestion();
            this.description = questionEntity.getDescription();
            this.timeLimit = questionEntity.getTimeLimit();
            this.difficulty = questionEntity.getDifficulty();
            List<Option> sortedOptions = questionEntity.getSortedOptions();
            List<Option> randomized = new ArrayList<>(sortedOptions);
            Collections.shuffle(randomized, new Random(seed));
            this.options = randomized.stream()
                    .map(opt -> new OptionDto(opt.getId(), opt.getContent()))
                    .collect(Collectors.toList());
        }
    }

    // 선택지 랜덤화를 위한 내부 클래스
    @Getter
    public static class QuestionOptions {
        private final String option1;
        private final String option2;
        private final String option3;
        private final String option4;

        private QuestionOptions(List<String> randomizedOptions) {
            this.option1 = randomizedOptions.get(0);
            this.option2 = randomizedOptions.get(1);
            this.option3 = randomizedOptions.get(2);
            this.option4 = randomizedOptions.get(3);
        }

        /**
         * 기존 비결정적 랜덤화 메서드
         */
        public static QuestionOptions createRandomized(String... options) {
            List<OptionWithIndex> optionsWithIndices = new ArrayList<>();
            for (int i = 0; i < options.length; i++) {
                optionsWithIndices.add(new OptionWithIndex(options[i], i + 1));
            }
            Collections.shuffle(optionsWithIndices);
            List<String> randomizedOptions = optionsWithIndices.stream()
                    .map(OptionWithIndex::getOption)
                    .collect(Collectors.toList());
            return new QuestionOptions(randomizedOptions);
        }

        /**
         * 시드 값을 사용한 결정적 랜덤화 메서드
         */
        public static QuestionOptions createRandomized(String[] options, long seed) {
            List<OptionWithIndex> optionsWithIndices = new ArrayList<>();
            for (int i = 0; i < options.length; i++) {
                optionsWithIndices.add(new OptionWithIndex(options[i], i + 1));
            }
            Collections.shuffle(optionsWithIndices, new Random(seed));
            List<String> randomizedOptions = optionsWithIndices.stream()
                    .map(OptionWithIndex::getOption)
                    .collect(Collectors.toList());
            return new QuestionOptions(randomizedOptions);
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
            // 기존: this.option1 = question.getOption1();
            // 변경: Option 엔티티를 통해 옵션 값 할당
            List<Option> sortedOptions = question.getSortedOptions();
            this.option1 = sortedOptions.size() > 0 ? sortedOptions.get(0).getContent() : null;
            this.option2 = sortedOptions.size() > 1 ? sortedOptions.get(1).getContent() : null;
            this.option3 = sortedOptions.size() > 2 ? sortedOptions.get(2).getContent() : null;
            this.option4 = sortedOptions.size() > 3 ? sortedOptions.get(3).getContent() : null;
        }

    }
}
