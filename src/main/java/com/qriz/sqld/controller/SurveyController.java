package com.qriz.sqld.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.survey.SurveyReqDto;
import com.qriz.sqld.dto.survey.SurveyRespDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.service.SurveyService;
import com.qriz.sqld.service.TestService;

import java.util.List;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class SurveyController {
    
    private final SurveyService surveyService;
    private final TestService testService;

    @PostMapping("/v1/survey")
    public ResponseEntity<?> submitSurvey(@AuthenticationPrincipal LoginUser loginUser, @Valid @RequestBody SurveyReqDto surveyReqDto) {
        List<SurveyRespDto> responses = surveyService.submitSurvey(loginUser.getUser().getId(), surveyReqDto.getKeyConcepts());
        
        // 진단고사 문제 추천
        // 30은 진단고사 문제 수
        List<TestRespDto.DailyRespDto> previewProblems = testService.recommendPreview(loginUser.getUser().getId(), 30);

        Map<String, Object> result = new HashMap<>();
        result.put("surveyResponses", responses);
        result.put("previewProblems", previewProblems);

        return new ResponseEntity<>(new ResponseDto<>(1, "설문조사 제출 성공 및 진단고사 준비", result), HttpStatus.OK);
    }

    @GetMapping("/v1/survey/status")
    public ResponseEntity<?> getSurveyStatus(@AuthenticationPrincipal LoginUser loginUser) {
        boolean isCompleted = surveyService.isSurveyCompleted(loginUser.getUser().getId());
        return new ResponseEntity<>(new ResponseDto<>(1, "설문조사 상태 조회 성공", isCompleted), HttpStatus.OK);
    }
}