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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.daily.ResultDetailDto;
import com.qriz.sqld.dto.exam.ExamReqDto;
import com.qriz.sqld.dto.exam.ExamRespDto;
import com.qriz.sqld.dto.exam.ExamTestResult;
import com.qriz.sqld.dto.test.TestReqDto;
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
            @RequestBody ExamReqDto submission,
            @AuthenticationPrincipal LoginUser loginUser) {
        examService.processExamSubmission(loginUser.getUser().getId(), session, submission);
        return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 제출 성공", null), HttpStatus.OK);
    }

    /**
     * 모의고사 공부 결과 - 문제 상세보기
     * 
     * @param session    모의고사 회차 정보
     * @param questionId 문제 아이디
     * @param loginUser  로그인한 사용자
     * @return
     */
    @GetMapping("/result/{session}/{questionId}")
    public ResponseEntity<?> getDailyResultDetail(@PathVariable String session,
            @PathVariable Long questionId,
            @AuthenticationPrincipal LoginUser loginUser) {
        ResultDetailDto resultDetail = examService.getExamResultDetail(loginUser.getUser().getId(),
                session,
                questionId);
        return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 결과 상세 조회 성공", resultDetail), HttpStatus.OK);
    }

    /**
     * 특정 모의고사 회차의 과목별 세부 항목 점수, 문제 풀이 결과 조회
     * 
     * @param session
     * @param loginUser
     * @return
     */
    @GetMapping("/subject-details/{session}")
    public ResponseEntity<?> getDaySubjectDetails(@PathVariable String session,
            @AuthenticationPrincipal LoginUser loginUser) {
        ExamTestResult.Response details = examService.getExamSubjectDetails(loginUser.getUser().getId(),
                session);
        return new ResponseEntity<>(new ResponseDto<>(1, "과목별 세부 항목 점수, 문제 풀이 결과 조회 성공", details),
                HttpStatus.OK);
    }

    /**
     * 모의고사 리스트 불러오기
     * 
     * 전체 리스트: /api/v1/exam/session-list
     * 학습 완료: /api/v1/exam/session-list?status=completed
     * 학스 전 + 1회차부터 정렬 /api/v1/exam/session-list?status=incomplete&sort=desc
     * 
     * @param loginUser
     * @return
     */
    @GetMapping("/session-list")
    public ResponseEntity<?> getSessionList(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "asc") String sort) {
        List<ExamRespDto.SessionList> lists = examService.getSessionList(loginUser.getUser().getId(), status, sort);
        return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 리스트 불러오기 성공", lists), HttpStatus.OK);
    }
}
