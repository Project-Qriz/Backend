package com.qriz.sqld.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.service.TestService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class TestController {

    private final TestService testService;

    // 데일리 문제 불러오기
    @GetMapping("/daily/get")
    public ResponseEntity<?> recommendDaily(@AuthenticationPrincipal LoginUser loginUser,
            @RequestParam int numRecommendations) {
        List<TestRespDto.DailyRespDto> recommendedProblems = testService.recommendDaily(loginUser.getUser().getId(),
                numRecommendations);
        return new ResponseEntity<>(new ResponseDto<>(1, "문제 불러오기 성공", recommendedProblems), HttpStatus.OK);
    }

    // 데일리 문제 제출
    @PostMapping("/daily/submit")
    public ResponseEntity<?> submitDaily(@AuthenticationPrincipal LoginUser loginUser,
            @RequestBody @Valid TestReqDto testSubmitReqDto) {
        List<TestRespDto.TestSubmitRespDto> submitResponse = testService.processActivity(loginUser.getUser().getId(),
                testSubmitReqDto);
        return new ResponseEntity<>(new ResponseDto<>(1, "테스트 제출 성공", submitResponse), HttpStatus.OK);
    }
}
