package com.qriz.sqld.dto.preview;

import lombok.Data;
import lombok.Builder;

import java.util.List;
import java.util.Map;

public class ResultDto {
    
    @Data
    @Builder
    public static class Request {
        private Map<String, Double> topicScores;
    }

    @Data
    @Builder
    public static class Response {
        private double estimatedScore;
        private ScoreBreakdown scoreBreakdown;
        private WeakAreaAnalysis weakAreaAnalysis;
        private List<String> topConceptsToImprove;
    }

    @Data
    @Builder
    public static class ScoreBreakdown {
        private int totalScore;
        private int part1Score;
        private int part2Score;
    }

    @Data
    @Builder
    public static class WeakAreaAnalysis {
        private int totalQuestions;
        private List<WeakArea> weakAreas;
    }

    @Data
    @Builder
    public static class WeakArea {
        private String topic;
        private int incorrectCount;
    }
}