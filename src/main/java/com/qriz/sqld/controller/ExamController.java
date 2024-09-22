package com.qriz.sqld.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
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
        List<TestRespDto.ExamRespDto> examQuestions = examService.getExamQuestionsBySession(loginUser.getUser().getId(),
                session);
        return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 문제 불러오기 성공", examQuestions), HttpStatus.OK);
    }
}
