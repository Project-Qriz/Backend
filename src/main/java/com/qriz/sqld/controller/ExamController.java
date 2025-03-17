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
         * @param examId    모의고사의 식별자 (예: 1)
         * @param loginUser 로그인한 사용자 정보
         * @return 모의고사 문제 및 관련 정보
         */
        @GetMapping("/get/{examId}")
        public ResponseEntity<?> getExamSession(@PathVariable Long examId,
                        @AuthenticationPrincipal LoginUser loginUser) {
                ExamTestResult examResult = examService.getExamQuestionsBySession(loginUser.getUser().getId(), examId);
                return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 문제 불러오기 성공", examResult), HttpStatus.OK);
        }

        /**
         * 모의고사 결과를 제출하기
         * 
         * @param submission 테스트 제출 데이터
         * @param loginUser  로그인한 사용자
         * @return 테스트 제출 결과
         */
        @PostMapping("/submit/{examId}")
        public ResponseEntity<?> submitExam(
                        @PathVariable Long examId,
                        @RequestBody ExamReqDto submission,
                        @AuthenticationPrincipal LoginUser loginUser) {
                examService.processExamSubmission(loginUser.getUser().getId(), examId, submission);
                return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 제출 성공", null), HttpStatus.OK);
        }

        /**
         * 모의고사 공부 결과 - 문제 상세보기
         * 
         * @param examId     모의고사 회차 정보
         * @param questionId 문제 아이디
         * @param loginUser  로그인한 사용자
         * @return
         */
        @GetMapping("/result/{examId}/{questionId}")
        public ResponseEntity<?> getDailyResultDetail(@PathVariable Long examId,
                        @PathVariable Long questionId,
                        @AuthenticationPrincipal LoginUser loginUser) {

                ResultDetailDto resultDetail = examService.getExamResultDetail(
                                loginUser.getUser().getId(),
                                examId,
                                questionId);
                return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 결과 상세 조회 성공", resultDetail), HttpStatus.OK);
        }

        @GetMapping("/{examId}/scores")
        public ResponseEntity<?> getExamScores(@PathVariable Long examId,
                        @RequestParam String subject,
                        @AuthenticationPrincipal LoginUser loginUser) {
                ExamTestResult.SimpleSubjectDetails scores = examService.getExamScoreBySubject(
                                loginUser.getUser().getId(), examId, subject);
                return new ResponseEntity<>(new ResponseDto<>(1, "특정 과목의 주요항목별 점수 조회 성공", scores),
                                HttpStatus.OK);
        }

        /**
         * 특정 모의고사의 문제별 채점 결과
         * 
         * @param examId
         * @param loginUser
         * @return
         */
        @GetMapping("/{examId}/results")
        public ResponseEntity<?> getExamResults(@PathVariable Long examId,
                        @AuthenticationPrincipal LoginUser loginUser) {

                ExamTestResult.ExamResultsDto results = examService.getExamResults(loginUser.getUser().getId(), examId);
                return new ResponseEntity<>(new ResponseDto<>(1, "문제 풀이 결과 조회 성공", results), HttpStatus.OK);
        }

        /**
         * 특정 모의고사의 과목별 세부항목별 점수
         * 
         * @param examId    모의고사의 식별자 (예: 1)
         * @param subject   subject1 또는 subject2
         * @param loginUser 로그인한 사용자 정보
         * @return 과목별 세부항목 점수
         */
        @GetMapping("/{examId}/subject-details")
        public ResponseEntity<?> getSubjectScoreDetails(
                        @PathVariable Long examId,
                        @RequestParam String subject,
                        @AuthenticationPrincipal LoginUser loginUser) {
                ExamTestResult.SubjectDetails details = examService.getSubjectScoreDetails(
                                loginUser.getUser().getId(), examId, subject);
                return new ResponseEntity<>(new ResponseDto<>(1, "과목별 세부항목 점수 조회 성공", details), HttpStatus.OK);
        }

        /**
         * 모의고사 리스트 불러오기
         * 
         * 전체 리스트: /api/v1/exam/session-list
         * 학습 완료: /api/v1/exam/session-list?status=completed
         * 학스 전 + 1회차부터 정렬 /api/v1/exam/session-list?status=incomplete&sort=asc
         * 
         * @param loginUser
         * @return
         */
        @GetMapping("/session-list")
        public ResponseEntity<?> getSessionList(
                        @AuthenticationPrincipal LoginUser loginUser,
                        @RequestParam(defaultValue = "all") String status,
                        @RequestParam(defaultValue = "asc") String sort) {
                List<ExamRespDto.SessionList> lists = examService.getSessionList(loginUser.getUser().getId(), status,
                                sort);
                return new ResponseEntity<>(new ResponseDto<>(1, "모의고사 리스트 불러오기 성공", lists), HttpStatus.OK);
        }
}
