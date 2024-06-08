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

    /**
     * 데일리 문제 불러오기
     * 
     * @param loginUser
     * @param numRecommendations
     * @return DailyRespDto
     */
    @GetMapping("/daily/get")
    public ResponseEntity<?> recommendDaily(@AuthenticationPrincipal LoginUser loginUser,
            @RequestParam int numRecommendations) {
        List<TestRespDto.DailyRespDto> recommendedProblems = testService.recommendDaily(loginUser.getUser().getId(),
                numRecommendations);
        return new ResponseEntity<>(new ResponseDto<>(1, "문제 불러오기 성공", recommendedProblems), HttpStatus.OK);
    }

    /**
     * 데일리 문제 제출
     * 
     * @param loginUser
     * @param testSubmitReqDto
     * @return TestSubmitRespDto
     */
    @PostMapping("/daily/submit")
    public ResponseEntity<?> submitDaily(@AuthenticationPrincipal LoginUser loginUser,
            @RequestBody @Valid TestReqDto testSubmitReqDto) {
        List<TestRespDto.TestSubmitRespDto> submitResponse = testService.processActivity(loginUser.getUser().getId(),
                testSubmitReqDto);
        return new ResponseEntity<>(new ResponseDto<>(1, "테스트 제출 성공", submitResponse), HttpStatus.OK);
    }

    /**
     * 데일리 결과 불러오기
     * @param loginUser
     * @param dayNumber
     * @return TestResultRespDto
     */
    @GetMapping("/daily/result")
    public ResponseEntity<?> resultDaily(@AuthenticationPrincipal LoginUser loginUser, @RequestParam String dayNumber) {
        List<TestRespDto.TestResultRespDto> dailyResults = testService
                .getDailyResults(loginUser.getUser().getId(), dayNumber);
        return new ResponseEntity<>(new ResponseDto<>(1, "테스트 결과 조회 성공", dailyResults), HttpStatus.OK);
    }

    /**
     * 테스트 결과 페이지에서 문제 상세보기
     * @param loginUser
     * @param activityId
     * @return TestResultDetailRespDto
     */
    @GetMapping("/test/detail")
    public ResponseEntity<?> resultDetailDaily(@AuthenticationPrincipal LoginUser loginUser, @RequestParam Long activityId) {
        TestRespDto.TestResultDetailRespDto dailyResultDetail = testService.getDailyResultDetail(loginUser.getUser().getId(), activityId);
        return new ResponseEntity<>(new ResponseDto<>(1, "테스트 결과 상세보기 성공", dailyResultDetail), HttpStatus.OK);
    }

    // 진단 고사 문제 추천
    @GetMapping("/preview/get")
    public ResponseEntity<?> recommendPreview(@AuthenticationPrincipal LoginUser loginUser,
            @RequestParam int numProblems) {
        List<TestRespDto.DailyRespDto> recommendedProblems = testService.recommendPreview(loginUser.getUser().getId(),
                numProblems);
        return new ResponseEntity<>(new ResponseDto<>(1, "진단 고사 문제 추천 성공", recommendedProblems), HttpStatus.OK);
    }

    // 진단 고사 결과 제출
    @PostMapping("/preview/submit")
    public ResponseEntity<?> submitPreview(@AuthenticationPrincipal LoginUser loginUser,
            @RequestBody @Valid TestReqDto testSubmitReqDto) {
        List<TestRespDto.TestSubmitRespDto> submitResponse = testService
                .processPreviewResults(loginUser.getUser().getId(), testSubmitReqDto.getActivities());
        return new ResponseEntity<>(new ResponseDto<>(1, "진단 고사 제출 성공", submitResponse), HttpStatus.OK);
    }
}
