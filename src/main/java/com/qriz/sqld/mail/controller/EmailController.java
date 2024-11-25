package com.qriz.sqld.mail.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.user.UserReqDto;
import com.qriz.sqld.dto.user.UserRespDto;
import com.qriz.sqld.mail.dto.EmailCheckDto;
import com.qriz.sqld.mail.dto.EmailReqDto;
import com.qriz.sqld.mail.service.EmailService;
import com.qriz.sqld.mail.service.MailSendService;
import com.qriz.sqld.service.user.UserService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class EmailController {

    private final MailSendService mailService;
    private final EmailService emailService;
    private final UserService userService;

    @PostMapping("/email-send")
    public ResponseEntity<?> mailSendUnloggedIn(@RequestBody @Valid EmailReqDto emailDto) {
        // 이메일 중복 확인
        UserReqDto.EmailDuplicateReqDto emailDuplicateReqDto = new UserReqDto.EmailDuplicateReqDto();
        emailDuplicateReqDto.setEmail(emailDto.getEmail());

        UserRespDto.EmailDuplicateRespDto emailDuplicateRespDto = emailService.emailDuplicate(emailDuplicateReqDto);

        if (!emailDuplicateRespDto.isAvailable()) {
            return new ResponseEntity<>(
                    new ResponseDto<>(-1, "해당 이메일은 이미 사용중입니다.", null),
                    HttpStatus.BAD_REQUEST);
        }

        String authNumber = mailService.joinEmail(emailDto.getEmail());
        return new ResponseEntity<>(
                new ResponseDto<>(1, "이메일 전송이 요청되었습니다.", "인증번호가 발송되었습니다."),
                HttpStatus.OK);
    }

    @PostMapping(value = { "/v1/email-authentication", "/email-authentication" })
    public ResponseEntity<?> authCheck(@RequestBody @Valid EmailCheckDto emailCheckDto) {
        boolean isVerified = mailService.CheckAuthNum(emailCheckDto.getEmail(), emailCheckDto.getAuthNum());

        if (isVerified) {
            return new ResponseEntity<>(
                    new ResponseDto<>(1, "인증이 완료되었습니다.", null),
                    HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                    new ResponseDto<>(-1, "인증번호가 유효하지 않거나 만료되었습니다.", null),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid EmailCheckDto emailCheckDto,
            @RequestParam("newPassword") String newPassword) {
        boolean isVerified = mailService.CheckAuthNum(emailCheckDto.getEmail(), emailCheckDto.getAuthNum());

        if (!isVerified) {
            return new ResponseEntity<>(
                    new ResponseDto<>(-1, "인증번호가 유효하지 않거나 만료되었습니다.", null),
                    HttpStatus.BAD_REQUEST);
        }

        userService.resetPassword(emailCheckDto.getEmail(), newPassword);
        return new ResponseEntity<>(
                new ResponseDto<>(1, "비밀번호가 성공적으로 변경되었습니다.", null),
                HttpStatus.OK);
    }
}