package com.qriz.sqld.controller;

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
import com.qriz.sqld.dto.exam.ExamReqDto;
import com.qriz.sqld.dto.preview.PreviewTestResult;
import com.qriz.sqld.dto.preview.ResultDto;
import com.qriz.sqld.service.preview.PreviewService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/preview")
public class PreviewController {

    private final PreviewService testService;

    /**
     * Preview Test 문제 불러오기
     * 
     * @param loginUser 로그인한 사용자
     * @return
     */
    @GetMapping("/get")
    public ResponseEntity<?> getPreviewTest(@AuthenticationPrincipal LoginUser loginUser) {
        PreviewTestResult previewTestResult = testService.getPreviewTestQuestions(loginUser.getUser());
        return new ResponseEntity<>(new ResponseDto<>(1, "문제 불러오기 성공", previewTestResult), HttpStatus.OK);
    }

    /**
     * Preview Test 문제 풀이 제출
     * 
     * @param examSubmitReqDto 사용자가 선택한 선택지
     * @param loginUser        로그인한 사용자
     * @return
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitPreviewTest(@RequestBody ExamReqDto examSubmitReqDto,
            @AuthenticationPrincipal LoginUser loginUser) {
        testService.processPreviewResults(loginUser.getUser().getId(), examSubmitReqDto.getActivities());
        return new ResponseEntity<>(new ResponseDto<>(1, "테스트 제출 성공", null), HttpStatus.OK);
    }

    /**
     * Preview Test 결과 분석
     * 
     * @param loginUser 로그인한 사용자
     * @return
     */
    @GetMapping("/analyze")
    public ResponseEntity<?> analyzePreviewTestResult(@AuthenticationPrincipal LoginUser loginUser) {
        ResultDto.Response analysisResult = testService.analyzePreviewTestResult(loginUser.getUser().getId());
        return new ResponseEntity<>(new ResponseDto<>(1, "프리뷰 테스트 분석 결과 조회 성공", analysisResult), HttpStatus.OK);
    }
}
