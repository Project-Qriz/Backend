package com.qriz.sqld.mail.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.mail.dto.EmailCheckDto;
import com.qriz.sqld.mail.dto.EmailReqDto;
import com.qriz.sqld.mail.service.MailSendService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class EmailController {
    
    private final MailSendService mailService;

    @PostMapping(value = {"/v1/email-send", "/email-send"})
    public ResponseEntity<String> mailSend(@RequestBody @Valid EmailReqDto emailDto) {
        System.out.println("이메일 인증 이메일 :" + emailDto.getEmail());
        String authNumber = mailService.joinEmail(emailDto.getEmail());
        return ResponseEntity.ok("이메일 전송이 요청되었습니다. 인증 번호: " + authNumber);
    }

    @PostMapping(value = {"/v1/email-authentication", "/email-authentication"})
    public ResponseEntity<String> AuthCheck(@RequestBody @Valid EmailCheckDto emailCheckDto) {
        Boolean Checked = mailService.CheckAuthNum(emailCheckDto.getEmail(), emailCheckDto.getAuthNum());
        if (Checked) {
            return ResponseEntity.ok("인증이 완료되었습니다.");
        } else {
            return ResponseEntity.status(400).body("인증에 실패하였습니다.");
        }
    }
}
