package com.qriz.sqld.dto.exam;

import java.util.ArrayList;
import java.util.List;
import com.qriz.sqld.dto.test.TestRespDto;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class ExamTestResult {
    private List<TestRespDto.ExamRespDto> questions;
    private int totalTimeLimit;

    @Getter
    @AllArgsConstructor
    public static class Response {
        private String dayNumber;
        private List<SubjectDetails> userDailyInfoList;
        private List<ResultDto> subjectResultsList;
    }

    @Getter
    public static class SubjectDetails {
        private String title;
        private double totalScore;
        private List<ItemScore> items;

        public SubjectDetails(String title) {
            this.title = title;
            this.items = new ArrayList<>();
        }

        public void addScore(String type, double score) {
            ItemScore existingItem = items.stream()
                    .filter(item -> item.getType().equals(type))
                    .findFirst()
                    .orElse(null);

            if (existingItem == null) {
                items.add(new ItemScore(type, score));
            } else {
                existingItem.addScore(score);
            }

            totalScore += score;
        }

        public void adjustTotalScore() {
            if (totalScore > 100) {
                double factor = 100.0 / totalScore;
                totalScore = 100.0;
                for (ItemScore item : items) {
                    item.adjustScore(factor);
                }
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ResultDto {
        private String skillName;
        private String question;
        private boolean correction;
    }

    @Getter
    @AllArgsConstructor
    public static class ItemScore {
        private String type;
        private double score;

        public void addScore(double additionalScore) {
            this.score += additionalScore;
        }

        public void adjustScore(double factor) {
            this.score *= factor;
        }
    }
}