package com.qriz.sqld.dto.daily;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeeklyTestResultDto {
    private Map<String, DailyScoreDto> dailyScores;

    public Map<String, Map<String, Double>> getFormattedScores() {
        Map<String, Map<String, Double>> formattedScores = new HashMap<>();
        for (Map.Entry<String, DailyScoreDto> entry : dailyScores.entrySet()) {
            Map<String, Double> scores = new HashMap<>();
            scores.put("1과목", entry.getValue().getSubject1Average());
            scores.put("2과목", entry.getValue().getSubject2Average());
            formattedScores.put(entry.getKey(), scores);
        }
        return formattedScores;
    }
}
