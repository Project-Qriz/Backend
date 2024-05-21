package com.qriz.sqld.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.application.ApplicationReqDto;
import com.qriz.sqld.dto.application.ApplicationRespDto;
import com.qriz.sqld.service.ApplyService;

import lombok.RequiredArgsConstructor;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApplyController {

    private final ApplyService applyService;

    // 시험 접수 목록 조회
    @GetMapping("/v1/application-list")
    public ResponseEntity<?> getApplicationList() {
        ApplicationRespDto.ApplyListRespDto applyListRespDto = applyService.applyList();
        return new ResponseEntity<>(new ResponseDto<>(1, "시험 접수 목록 불러오기 성공", applyListRespDto), HttpStatus.OK);
    }

    // 시험 접수
    @PostMapping("/v1/apply")
    public ResponseEntity<?> apply(@AuthenticationPrincipal LoginUser loginUser,
            @RequestBody @Valid ApplicationReqDto.ApplyReqDto applyReqDto) {

        System.out.println("applyReqDto = " + applyReqDto);
        ApplicationRespDto.ApplyRespDto applyRespDto = applyService.apply(applyReqDto, loginUser);
        return new ResponseEntity<>(new ResponseDto<>(1, "시험 접수 등록 성공", applyRespDto), HttpStatus.OK);
    }

}
