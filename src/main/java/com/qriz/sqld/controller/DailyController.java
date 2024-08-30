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
import com.qriz.sqld.dto.daily.DailyResultDetailDto;
import com.qriz.sqld.dto.daily.DaySubjectDetailsDto;
import com.qriz.sqld.dto.daily.UserDailyDto;
import com.qriz.sqld.dto.daily.WeeklyTestResultDto;
import com.qriz.sqld.dto.test.TestReqDto;
import com.qriz.sqld.dto.test.TestRespDto;
import com.qriz.sqld.service.daily.DailyPlanService;
import com.qriz.sqld.service.daily.DailyService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/daily")
public class DailyController {

        private final DailyService dailyService;
        private final DailyPlanService dailyPlanService;

        /**
         * 오늘의 데일리 테스트 문제를 가져오기
         * 
         * @param loginUser 로그인한 사용자
         * @return 데일리 테스트 문제 목록
         */
        @GetMapping("/get/{dayNumber}")
        public ResponseEntity<?> getDailyTestByDay(@PathVariable String dayNumber,
                        @AuthenticationPrincipal LoginUser loginUser) {
                List<TestRespDto.DailyRespDto> dailyQuestions = dailyService
                                .getDailyTestQuestionsByDay(loginUser.getUser().getId(), dayNumber);
                return new ResponseEntity<>(new ResponseDto<>(1, "문제 불러오기 성공", dailyQuestions), HttpStatus.OK);
        }

        /**
         * 사용자의 데일리 플랜을 가져오기
         * 
         * @param loginUser 로그인한 사용자
         * @return 데일리 플랜 목록
         */
        @GetMapping("/plan")
        public ResponseEntity<?> getDailyPlan(@AuthenticationPrincipal LoginUser loginUser) {
                List<UserDailyDto> dailyPlan = dailyPlanService.getUserDailyPlan(loginUser.getUser().getId());
                return new ResponseEntity<>(new ResponseDto<>(1, "플랜 불러오기 성공", dailyPlan), HttpStatus.OK);
        }

        /**
         * 데일리 테스트 결과를 제출하기
         * 
         * @param submission 테스트 제출 데이터
         * @param loginUser  로그인한 사용자
         * @return 테스트 제출 결과
         */
        @PostMapping("/submit/{dayNumber}")
        public ResponseEntity<?> submitDailyTest(
                        @PathVariable String dayNumber,
                        @RequestBody TestReqDto submission,
                        @AuthenticationPrincipal LoginUser loginUser) {
                dailyService.processDailyTestSubmission(loginUser.getUser().getId(), dayNumber, submission);
                return new ResponseEntity<>(new ResponseDto<>(1, "테스트 제출 성공", null), HttpStatus.OK);
        }

        /**
         * 오늘의 공부 결과 - 문제 상세보기
         * 
         * @param dayNumber  데일리 정보
         * @param questionId 문제 아이디
         * @param loginUser  로그인한 사용자
         * @return
         */
        @GetMapping("/result/{dayNumber}/{questionId}")
        public ResponseEntity<?> getDailyResultDetail(@PathVariable String dayNumber,
                        @PathVariable Long questionId,
                        @AuthenticationPrincipal LoginUser loginUser) {
                DailyResultDetailDto resultDetail = dailyService.getDailyResultDetail(loginUser.getUser().getId(),
                                dayNumber,
                                questionId);
                return new ResponseEntity<>(new ResponseDto<>(1, "데일리 결과 상세 조회 성공", resultDetail), HttpStatus.OK);
        }

        /**
         * 특정 Day 가 포함된 주의 과목별 테스트 결과 점수
         * 
         * @param dayNumber
         * @param loginUser
         * @return
         */
        @GetMapping("/detailed-weekly-result/{dayNumber}")
        public ResponseEntity<?> getDetailedWeeklyTestResult(@PathVariable String dayNumber,
                        @AuthenticationPrincipal LoginUser loginUser) {
                WeeklyTestResultDto result = dailyService.getDetailedWeeklyTestResult(loginUser.getUser().getId(),
                                dayNumber);
                return new ResponseEntity<>(new ResponseDto<>(1, "주간 과목점수 비교 조회 성공", result), HttpStatus.OK);
        }

        /**
         * 특정 Day 의 과목별 세부 항목 점수, 문제 풀이 결과 조회
         * 
         * @param dayNumber
         * @param loginUser
         * @return
         */
        @GetMapping("/subject-details/{dayNumber}")
        public ResponseEntity<?> getDaySubjectDetails(@PathVariable String dayNumber,
                        @AuthenticationPrincipal LoginUser loginUser) {
                DaySubjectDetailsDto.Response details = dailyService.getDaySubjectDetails(loginUser.getUser().getId(),
                                dayNumber);
                return new ResponseEntity<>(new ResponseDto<>(1, "과목별 세부 항목 점수, 문제 풀이 결과 조회 성공", details),
                                HttpStatus.OK);
        }

        /**
         * 특정 Day 에 대한 세부항목과 통과 여부 확인
         * 
         * @param dayNumber
         * @param loginUser
         * @return
         */
        @GetMapping("/details/{dayNumber}")
        public ResponseEntity<?> getDailyDetails(@PathVariable String dayNumber,
                        @AuthenticationPrincipal LoginUser loginUser) {
                UserDailyDto.DailyDetailsDto details = dailyService.getDailyDetails(loginUser.getUser().getId(),
                                dayNumber);
                return new ResponseEntity<>(new ResponseDto<>(1, "일일 상세 정보 조회 성공", details), HttpStatus.OK);
        }

        /**
         * 특정 Day의 테스트 상태 확인
         * 
         * @param dayNumber
         * @param loginUser
         * @return
         */
        @GetMapping("/test-status/{dayNumber}")
        public ResponseEntity<?> getDailyTestStatus(@PathVariable String dayNumber,
                        @AuthenticationPrincipal LoginUser loginUser) {
                UserDailyDto.TestStatusDto status = dailyService.getDailyTestStatus(loginUser.getUser().getId(),
                                dayNumber);
                return new ResponseEntity<>(new ResponseDto<>(1, "데일리 테스트 상태 조회 성공", status), HttpStatus.OK);
        }

        // 테스트용
        @PostMapping("/complete/{dayNumber}")
        public ResponseEntity<?> completeDailyTest(@PathVariable String dayNumber,
                        @AuthenticationPrincipal LoginUser loginUser) {
                dailyService.completeDailyTest(loginUser.getUser().getId(), dayNumber);
                return new ResponseEntity<>(new ResponseDto<>(1, "데일리 테스트 완료 처리 성공", null), HttpStatus.OK);
        }
}