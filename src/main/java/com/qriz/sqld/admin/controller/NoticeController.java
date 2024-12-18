package com.qriz.sqld.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.admin.dto.notice.NoticeRespDto;
import com.qriz.sqld.admin.service.NoticeService;
import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.dto.ResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notice")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/get-notice/{noticeId}")
    private ResponseEntity<?> getNotice(@AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long noticeId) {
        NoticeRespDto.getNoticeRespDto getNotice = noticeService.getNotice(noticeId);
        return new ResponseEntity<>(new ResponseDto<>(1, "공지사항 불러오기 성공", getNotice), HttpStatus.OK);
    }

}
