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
import com.qriz.sqld.dto.preview.QuestionDto;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.service.PreviewService;

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
        List<QuestionDto> previewQuestions = testService.getPreviewTestQuestions(loginUser.getUser());
        return new ResponseEntity<>(new ResponseDto<>(1, "문제 불러오기 성공", previewQuestions), HttpStatus.OK);
    }

    /**
     * Preview Test 문제 풀이 제출
     * 
     * @param testSubmitReqDto 사용자가 선택한 선택지
     * @param loginUser 로그인한 사용자
     * @return
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitPreviewTest(@RequestBody TestReqDto testSubmitReqDto,
            @AuthenticationPrincipal LoginUser loginUser) {
        List<TestRespDto.TestSubmitRespDto> submitResponse = testService
                .processPreviewResults(loginUser.getUser().getId(), testSubmitReqDto.getActivities());
        return new ResponseEntity<>(new ResponseDto<>(1, "테스트 제출 성공", submitResponse), HttpStatus.OK);
    }
}
