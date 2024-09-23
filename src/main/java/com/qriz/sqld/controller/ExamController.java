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
import com.qriz.sqld.dto.exam.ExamTestResult;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.service.exam.ExamService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/exam")
public class ExamController {

    private final ExamService examService;

    /**
     * 특정 회차의 모의고사 문제 불러오기
     * 
     * @param session
     * @param loginUser
     * @return
     */
    @GetMapping("/get/{session}")
    public ResponseEntity<?> getExamSession(@PathVariable String session,
            @AuthenticationPrincipal LoginUser loginUser) {
        ExamTestResult examResult = examService.getExamQuestionsBySession(loginUser.getUser().getId(), session);
        return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 문제 불러오기 성공", examResult), HttpStatus.OK);
    }

    /**
     * 모의고사 결과를 제출하기
     * 
     * @param submission 테스트 제출 데이터
     * @param loginUser  로그인한 사용자
     * @return 테스트 제출 결과
     */
    @PostMapping("/submit/{session}")
    public ResponseEntity<?> submitExam(
            @PathVariable String session,
            @RequestBody TestReqDto submission,
            @AuthenticationPrincipal LoginUser loginUser) {
        List<TestRespDto.TestSubmitRespDto> results = examService.processExamSubmission(loginUser.getUser().getId(), session, submission);
        return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 제출 성공", results), HttpStatus.OK);
    }
}
