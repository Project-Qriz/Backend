package com.qriz.sqld.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.recommend.WeeklyRecommendationDto;
import com.qriz.sqld.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/recommend")
public class RecommendController {

    private final RecommendationService recommendationService;
    
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyRecommended(@AuthenticationPrincipal LoginUser loginUser) {
        WeeklyRecommendationDto recommendation = recommendationService.getWeeklyRecommendation(loginUser.getUser().getId());
        return new ResponseEntity<>(new ResponseDto<>(1, "주간 추천 개념 조회 성공", recommendation), HttpStatus.OK);
    }

}
