package com.qriz.sqld.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.clip.ClipReqDto;
import com.qriz.sqld.dto.clip.ClipRespDto;
import com.qriz.sqld.dto.daily.DailyResultDetailDto;
import com.qriz.sqld.service.ClipService;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clip")
@RequiredArgsConstructor
public class ClipController {

    private final ClipService clipService;

    @PostMapping
    public ResponseEntity<?> clipQuestion(@AuthenticationPrincipal LoginUser loginUser,
                                          @RequestBody ClipReqDto clipReqDto) {
        clipService.clipQuestion(loginUser.getUser().getId(), clipReqDto);
        return new ResponseEntity<>(new ResponseDto<>(1, "오답노트 등록 성공", null), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> getClippedQuestions(@AuthenticationPrincipal LoginUser loginUser) {
        List<ClipRespDto> clippedQuestions = clipService.getClippedQuestions(loginUser.getUser().getId());
        return new ResponseEntity<>(new ResponseDto<>(1, "오답노트 조회 성공", clippedQuestions), HttpStatus.OK);
    }

    @DeleteMapping("/{clipId}")
    public ResponseEntity<?> unclipQuestion(@AuthenticationPrincipal LoginUser loginUser,
                                            @PathVariable Long clipId) {
        clipService.unclipQuestion(loginUser.getUser().getId(), clipId);
        return new ResponseEntity<>(new ResponseDto<>(1, "오답노트 삭제 성공", null), HttpStatus.OK);
    }

    @GetMapping("/{clipId}/detail")
    public ResponseEntity<?> getClippedQuestionDetail(@AuthenticationPrincipal LoginUser loginUser,
                                                      @PathVariable Long clipId) {
        DailyResultDetailDto detailDto = clipService.getClippedQuestionDetail(loginUser.getUser().getId(), clipId);
        return new ResponseEntity<>(new ResponseDto<>(1, "오답노트 문제 상세 조회 성공", detailDto), HttpStatus.OK);
    }
}
