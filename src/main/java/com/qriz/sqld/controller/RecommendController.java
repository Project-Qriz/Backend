package com.qriz.sqld.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.recommend.RecommendRespDto;
import com.qriz.sqld.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {
    
    private final RecommendationService recommendationService;

    @GetMapping("/v1/problem")
    public ResponseEntity<?> recommendProblem(@AuthenticationPrincipal LoginUser loginUser, @RequestParam int numRecommendations) {
        List<RecommendRespDto.DailyRespDto> recommendedProblems = recommendationService.recommendProblem(loginUser.getUser().getId(), numRecommendations);
        return new ResponseEntity<>(new ResponseDto<>(1, "문제 불러오기 성공", recommendedProblems), HttpStatus.OK);
    }
}
