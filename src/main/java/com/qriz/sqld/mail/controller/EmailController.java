package com.qriz.sqld.mail.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.qriz.sqld.util.RedisUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class EmailController {

    private final MailSendService mailService;
    private final EmailService emailService;
    private final UserService userService;
    private final RedisUtil redisUtil;

    /**
     * 로그인 하지 않은 상태: 회원가입, 아이디 및 비밀번호 찾기 시
     * 
     * @param emailDto
     * @return
     */
    @PostMapping("/email-send")
    public ResponseEntity<?> mailSendUnloggedIn(@RequestBody @Valid EmailReqDto emailDto) {
        // 이메일 중복 확인
        UserReqDto.EmailDuplicateReqDto emailDuplicateReqDto = new UserReqDto.EmailDuplicateReqDto();
        emailDuplicateReqDto.setEmail(emailDto.getEmail());

        UserRespDto.EmailDuplicateRespDto emailDuplicateRespDto = emailService.emailDuplicate(emailDuplicateReqDto);

        if (!emailDuplicateRespDto.isAvailable()) {
            return new ResponseEntity<>(new ResponseDto<>(-1, "해당 이메일은 이미 사용중입니다.", null), HttpStatus.BAD_REQUEST);
        }

        // 이메일이 사용 가능한 경우, 인증 이메일 전송
        String authNumber = mailService.joinEmail(emailDto.getEmail());
        return new ResponseEntity<>(new ResponseDto<>(1, "이메일 전송이 요청되었습니다.", authNumber), HttpStatus.OK);
    }

    /**
     * 로그인 한 상태: 비밀번호 변경 시
     * 
     * @param emailDto
     * @return
     */
    @PostMapping(value = { "/v1/email-send" })
    public ResponseEntity<String> mailSendLoggedIn(@RequestBody @Valid EmailReqDto emailDto) {
        System.out.println("이메일 인증 이메일 :" + emailDto.getEmail());
        String authNumber = mailService.joinEmail(emailDto.getEmail());
        return ResponseEntity.ok("이메일 전송이 요청되었습니다. 인증 번호: " + authNumber);
    }

    // 이메일 중복확인
    @GetMapping("/email-duplicate")
    public ResponseEntity<?> emailDuplicate(@RequestBody @Valid UserReqDto.EmailDuplicateReqDto emailDuplicateReqDto) {
        UserRespDto.EmailDuplicateRespDto emailDuplicateRespDto = emailService.emailDuplicate(emailDuplicateReqDto);

        if (!emailDuplicateRespDto.isAvailable()) {
            return new ResponseEntity<>(new ResponseDto<>(-1, "해당 이메일은 이미 사용중 입니다.", emailDuplicateRespDto),
                    HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(new ResponseDto<>(1, "사용 가능한 이메일입니다.", emailDuplicateRespDto), HttpStatus.OK);
        }
    }

    @PostMapping(value = { "/v1/email-authentication", "/email-authentication" })
    public ResponseEntity<String> AuthCheck(@RequestBody @Valid EmailCheckDto emailCheckDto) {
        Boolean Checked = mailService.CheckAuthNum(emailCheckDto.getEmail(), emailCheckDto.getAuthNum());
        if (Checked) {
            return ResponseEntity.ok("인증이 완료되었습니다.");
        } else {
            return ResponseEntity.status(400).body("인증에 실패하였습니다.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword) {
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
