package com.qriz.sqld.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.clip.ClipReqDto;
import com.qriz.sqld.dto.clip.ClipRespDto;
import com.qriz.sqld.dto.daily.ResultDetailDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.service.clip.ClipService;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clips")
@RequiredArgsConstructor
public class ClipController {

    private final ClipService clipService;

    private final Logger log = LoggerFactory.getLogger(ClipController.class);

    /**
     * 오답노트 리스트 조회
     * 
     * @param loginUser 로그인 사용자
     * @return
     */
    @GetMapping
    public ResponseEntity<?> getClippedQuestions(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) List<String> keyConcepts,
            @RequestParam(required = false, defaultValue = "false") boolean onlyIncorrect,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) String dayNumber) {
        List<ClipRespDto> clippedQuestions;
        if (dayNumber != null) {
            clippedQuestions = clipService.getFilteredClippedQuestions(
                    loginUser.getUser().getId(), keyConcepts, onlyIncorrect, category, dayNumber);
        } else {
            clippedQuestions = clipService.getClippedQuestions(
                    loginUser.getUser().getId(), keyConcepts, onlyIncorrect, category);
        }
        return new ResponseEntity<>(new ResponseDto<>(1, "오답노트 조회 성공", clippedQuestions), HttpStatus.OK);
    }

    /**
     * 오답노트에 등록한 문제 상세보기
     * 
     * @param loginUser 로그인 사용자
     * @param clipId    오답노트 PK
     * @return
     */
    @GetMapping("/{clipId}/detail")
    public ResponseEntity<?> getClippedQuestionDetail(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long clipId) {
        try {
            ResultDetailDto detailDto = clipService.getClippedQuestionDetail(loginUser.getUser().getId(), clipId);
            return new ResponseEntity<>(new ResponseDto<>(1, "오답노트 문제 상세 조회 성공", detailDto), HttpStatus.OK);
        } catch (CustomApiException e) {
            log.error("Error getting clipped question detail for user: {} and clip: {}", loginUser.getUser().getId(),
                    clipId, e);
            return new ResponseEntity<>(new ResponseDto<>(-1, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Unexpected error getting clipped question detail for user: {} and clip: {}",
                    loginUser.getUser().getId(), clipId, e);
            return new ResponseEntity<>(new ResponseDto<>(-1, "서버 오류가 발생했습니다.", null),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/days")
    public ResponseEntity<?> getClippedDays(@AuthenticationPrincipal LoginUser loginUser) {
        ClipRespDto.ClippedDaysDto clippedDaysDto = clipService.getClippedDaysDtos(loginUser.getUser().getId());
        return new ResponseEntity<>(new ResponseDto<>(1, "완료한 데일리 리스트 조회 성공", clippedDaysDto), HttpStatus.OK);
    }
}
