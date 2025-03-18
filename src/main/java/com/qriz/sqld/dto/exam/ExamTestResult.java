package com.qriz.sqld.dto.exam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public static class ExamScoreDto {
        private String session; // 회차
        private List<SubjectDetails> userExamInfoList;
    }

    @Getter
    @AllArgsConstructor
    public static class SimpleSubjectDetails {
        private String title; // "1과목", "2과목"
        private double totalScore;
        private List<SimpleMajorItem> majorItems;
    }

    @Getter
    @AllArgsConstructor
    public static class SimpleMajorItem {
        private String majorItem; // 예: "데이터 모델링의 이해"
        private double score;
    }

    /**
     * 과목별 점수 정보를 담는 DTO
     */
    @Getter
    public static class SubjectDetails {
        private String title; // "1과목", "2과목"
        private double totalScore;
        private List<MajorItemDetail> majorItems; // 주요 항목 및 세부 항목 정보

        public SubjectDetails(String title) {
            this.title = title;
            this.majorItems = new ArrayList<>();
        }

        /**
         * 특정 주요 항목에 대한 점수를 추가합니다.
         * 만약 이미 해당 주요 항목이 있다면 점수를 누적하고, 없으면 새로 생성합니다.
         */
        public void addMajorItemScore(String majorItem, double score) {
            MajorItemDetail existing = majorItems.stream()
                    .filter(item -> item.getMajorItem().equals(majorItem))
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                majorItems.add(new MajorItemDetail(majorItem, score, new ArrayList<>()));
            } else {
                existing.addScore(score);
            }
            totalScore += score;
        }

        public void adjustTotalScore() {
            if (totalScore > 100) {
                double factor = 100.0 / totalScore;
                totalScore = 100.0;
                for (MajorItemDetail item : majorItems) {
                    item.adjustScore(factor);
                }
            }
        }
    }

    /**
     * 주요 항목 및 그 세부 항목 점수 정보를 담는 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class MajorItemDetail {
        private String majorItem; // 예: "데이터 모델링의 이해"
        private double score;
        private List<SubItemScore> subItemScores; // 하위 세부 항목 정보

        public void addScore(double additionalScore) {
            this.score += additionalScore;
        }

        public void adjustScore(double factor) {
            this.score *= factor;
            for (SubItemScore sub : subItemScores) {
                sub.adjustScore(factor);
            }
        }

        /**
         * 세부 항목 점수를 누적합니다.
         */
        public void addSubItemScore(String subItem, double additionalScore) {
            SubItemScore existing = subItemScores.stream()
                    .filter(s -> s.getSubItem().equals(subItem))
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                subItemScores.add(new SubItemScore(subItem, additionalScore));
            } else {
                existing.addScore(additionalScore);
            }
        }
    }

    /**
     * 각 세부 항목의 점수 정보를 담는 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class SubItemScore {
        private String subItem; // 예: "데이터모델의 이해", "엔터티" 등
        private double score;

        public void addScore(double additionalScore) {
            this.score += additionalScore;
        }

        public void adjustScore(double factor) {
            this.score *= factor;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ExamResultsDto {
        private List<ResultDto> problemResults; // 문제별 정/오답 정보
        private List<HistoricalScore> historicalScores; // 과거 점수 이력
    }

    @Getter
    @AllArgsConstructor
    public static class ResultDto {
        private int questionNum;
        private String skillName;
        private String question;
        private boolean correction;
    }

    @Getter
    @AllArgsConstructor
    public static class HistoricalScore {
        private LocalDateTime completionDateTime;
        private List<ItemScore> itemScores;
        private int attemptCount;
        private String displayDate;

        public HistoricalScore(LocalDateTime completionDateTime, List<ItemScore> itemScores, int attemptCount) {
            this.completionDateTime = completionDateTime;
            this.itemScores = itemScores;
            this.attemptCount = attemptCount;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM.dd");
            this.displayDate = completionDateTime.format(formatter);
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