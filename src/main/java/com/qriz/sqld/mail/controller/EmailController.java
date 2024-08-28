package com.qriz.sqld.mail.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.mail.dto.EmailCheckDto;
import com.qriz.sqld.mail.dto.EmailReqDto;
import com.qriz.sqld.mail.service.MailSendService;
import com.qriz.sqld.service.user.UserService;
import com.qriz.sqld.util.RedisUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class EmailController {
    
    private final MailSendService mailService;
    private final UserService userService;
    private final RedisUtil redisUtil;

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

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("token") String token, @RequestParam("newPassword") String newPassword) {
        String email = redisUtil.getData(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이메일이 유효하지 않거나 토큰이 유효하지 않습니다.");
        }

        // 비밀번호 변경
        userService.resetPassword(email, newPassword);
        redisUtil.deleteData(token);
        return ResponseEntity.ok("비빌번호가 성공적으로 초기화되었습니다.");
    }
}
