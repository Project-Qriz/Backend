package com.qriz.sqld.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.survey.SurveyReqDto;
import com.qriz.sqld.dto.survey.SurveyRespDto;
import com.qriz.sqld.service.SurveyService;

import java.util.List;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class SurveyController {
    
    private final SurveyService surveyService;

    @PostMapping("/v1/survey")
    public ResponseEntity<?> submitSurvey(@AuthenticationPrincipal LoginUser loginUser, @Valid @RequestBody SurveyReqDto surveyReqDto) {
        List<SurveyRespDto> responses = surveyService.submitSurvey(loginUser.getUser().getId(), surveyReqDto.getKeyConcepts());
        return new ResponseEntity<>(new ResponseDto<>(1, "설문조사 제출 성공", responses), HttpStatus.OK);
    }
}
