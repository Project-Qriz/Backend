package com.qriz.sqld.controller;

import java.util.List;

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
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.service.survey.SurveyService;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/survey")
public class SurveyController {

    private final SurveyService surveyService;

    /**
     * 설문조사 제출
     * 
     * @param loginUser 로그인한 사용자
     * @param surveyReqDto 설문조사 요청 데이터
     * @return
     */
    @PostMapping
    public ResponseEntity<?> submitSurvey(@AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody SurveyReqDto surveyReqDto) {
        try {
            List<SurveyRespDto> responses = surveyService.submitSurvey(loginUser.getUser().getId(),
                    surveyReqDto.getKeyConcepts());
            return new ResponseEntity<>(new ResponseDto<>(1, "설문조사 제출 성공", responses), HttpStatus.OK);
        } catch (CustomApiException e) {
            return new ResponseEntity<>(new ResponseDto<>(-1, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 설문조사 완료 여부 확인
     * 
     * @param loginUser 로그인한 사용자
     * @return
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSurveyStatus(@AuthenticationPrincipal LoginUser loginUser) {
        boolean isCompleted = surveyService.isSurveyCompleted(loginUser.getUser().getId());
        return new ResponseEntity<>(new ResponseDto<>(1, "설문조사 상태 조회 성공", isCompleted), HttpStatus.OK);
    }

    /**
     * 설문조사 결과 조회
     * 
     * @param loginUser 로그인한 사용자
     * @return
     */
    @GetMapping("/results")
    public ResponseEntity<?> getSurveyResults(@AuthenticationPrincipal LoginUser loginUser) {
        List<SurveyRespDto> results = surveyService.getSurveyResults(loginUser.getUser().getId());
        return new ResponseEntity<>(new ResponseDto<>(1, "설문조사 결과 조회 성공", results), HttpStatus.OK);
    }
}