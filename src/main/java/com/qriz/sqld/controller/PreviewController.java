package com.qriz.sqld.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.preview.PreviewTestResult;
import com.qriz.sqld.dto.preview.QuestionDto;
import com.qriz.sqld.dto.preview.ResultDto;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
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
     * @param testSubmitReqDto 사용자가 선택한 선택지
     * @param loginUser        로그인한 사용자
     * @return
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitPreviewTest(@RequestBody TestReqDto testSubmitReqDto,
            @AuthenticationPrincipal LoginUser loginUser) {
        testService.processPreviewResults(loginUser.getUser().getId(), testSubmitReqDto.getActivities());
        return new ResponseEntity<>(new ResponseDto<>(1, "테스트 제출 성공", null), HttpStatus.OK);
    }

    /**
     * Preview Test 결과 분석
     * 
     * @param loginUser 로그인한 사용자
     * @param testInfo  테스트 정보 (예: "Preview Test")
     * @return
     */
    @GetMapping("/analyze/{testInfo}")
    public ResponseEntity<?> analyzePreviewTestResult(@AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String testInfo) {
        ResultDto.Response analysisResult = testService.analyzePreviewTestResult(loginUser.getUser().getId(), testInfo);
        return new ResponseEntity<>(new ResponseDto<>(1, "프리뷰 테스트 분석 결과 조회 성공", analysisResult), HttpStatus.OK);
    }
}
