package com.qriz.sqld.dto.daily;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class DaySubjectDetailsDto {
    
    @Getter
    @AllArgsConstructor
    public static class Response {
        private String dayNumber;
        private List<SubjectDetails> userDailyInfoList;
        private List<DailyResultDetailDto> subjectResultsList;
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

        public void addScore(String keyConcepts, double score) {
            ItemScore existingItem = items.stream()
                    .filter(item -> item.getType().equals(keyConcepts))
                    .findFirst()
                    .orElse(null);

            if (existingItem == null) {
                items.add(new ItemScore(keyConcepts, score));
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