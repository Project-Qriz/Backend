package com.qriz.sqld.dto.daily;

import lombok.Getter;

@Getter
public class DailyScoreDto {
    private double subject1Score = 0;
    private double subject2Score = 0;
    private int subject1Count = 0;
    private int subject2Count = 0;
    private String passed;

    public void addScore(String subject, double score) {
        if ("1과목".equals(subject)) {
            subject1Score += score;
            subject1Count++;
        } else if ("2과목".equals(subject)) {
            subject2Score += score;
            subject2Count++;
        }
    }

    public double getSubject1Average() {
        return subject1Count > 0 ? subject1Score / subject1Count : 0;
    }

    public double getSubject2Average() {
        return subject2Count > 0 ? subject2Score / subject2Count : 0;
    }
}
