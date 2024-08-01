package com.qriz.sqld.service;

import com.qriz.sqld.domain.userActivity.UserActivity;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DKTService {

    private final String DKT_SERVER_URL = "http://localhost:5000/predict";
    private final RestTemplate restTemplate;

    public List<Double> getPredictions(Long userId, List<UserActivity> activities) {
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", userId);
        request.put("activities", convertActivitiesToDktFormat(activities));

        ResponseEntity<Map> response = restTemplate.postForEntity(DKT_SERVER_URL, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Object predictionsObj = response.getBody().get("predictions");
            if (predictionsObj instanceof List<?>) {
                return ((List<?>) predictionsObj).stream()
                        .filter(obj -> obj instanceof Double)
                        .map(obj -> (Double) obj)
                        .collect(Collectors.toList());
            } else {
                throw new RuntimeException("DKT 서버에서 올바르지 못한 값을 반환 받았습니다");
            }
        } else {
            throw new RuntimeException("DKT 서버에서 응답을 가져올 수 없습니다");
        }
    }

    private List<Map<String, Object>> convertActivitiesToDktFormat(List<UserActivity> activities) {
        return activities.stream().map(activity -> {
            Map<String, Object> activityMap = new HashMap<>();
            activityMap.put("question_id", activity.getQuestion().getId());
            activityMap.put("correct", activity.isCorrection());
            activityMap.put("time_spent", activity.getTimeSpent());
            return activityMap;
        }).collect(Collectors.toList());
    }
}