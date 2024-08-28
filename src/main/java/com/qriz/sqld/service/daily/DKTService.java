package com.qriz.sqld.service.daily;

import com.qriz.sqld.domain.userActivity.UserActivity;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class DKTService {

    private final String DKT_SERVER_URL = "http://localhost:5000/predict";
    private final RestTemplate restTemplate;

    private final Logger log = LoggerFactory.getLogger(DKTService.class);

    public List<Double> getPredictions(Long userId, List<UserActivity> activities) {
        log.info("Getting predictions for user {} with {} activities", userId, activities.size());

        if (activities.isEmpty()) {
            log.warn("No activities found for user {}", userId);
            return Collections.emptyList();
        }

        Map<String, Object> request = new HashMap<>();
        request.put("user_id", userId);
        request.put("activities", convertActivitiesToDktFormat(activities));

        log.info("Sending request to DKT server: {}", request);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(DKT_SERVER_URL, request, Map.class);
            log.info("Received response from DKT server: {}", response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object predictionsObj = response.getBody().get("predictions");
                if (predictionsObj instanceof List<?>) {
                    List<Double> predictions = ((List<?>) predictionsObj).stream()
                            .filter(obj -> obj instanceof Number)
                            .map(obj -> ((Number) obj).doubleValue())
                            .collect(Collectors.toList());
                    log.info("Received {} predictions from DKT server", predictions.size());
                    return predictions;
                }
            }
        } catch (Exception e) {
            log.error("Error while getting predictions from DKT server", e);
        }

        log.error("Failed to get valid predictions from DKT server");
        return Collections.emptyList();
    }

    private List<Map<String, Object>> convertActivitiesToDktFormat(List<UserActivity> activities) {
        return activities.stream().map(activity -> {
            Map<String, Object> activityMap = new HashMap<>();
            activityMap.put("question_id", activity.getQuestion().getId().intValue()); // Long을 int로 변환
            activityMap.put("correct", activity.isCorrection() ? 1 : 0); // boolean을 int로 변환
            activityMap.put("time_spent", activity.getTimeSpent().intValue()); // Long을 int로 변환
            return activityMap;
        }).collect(Collectors.toList());
    }
}