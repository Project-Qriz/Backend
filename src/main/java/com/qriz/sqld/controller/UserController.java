package com.qriz.sqld.controller;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.user.UserReqDto;
import com.qriz.sqld.dto.user.UserRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.mail.domain.EmailVerification.EmailVerification;
import com.qriz.sqld.mail.domain.EmailVerification.EmailVerificationRepository;
import com.qriz.sqld.mail.service.MailSendService;
import com.qriz.sqld.service.user.UserService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final MailSendService mailService;
    private final EmailVerificationRepository verificationRepository;

    // 회원 가입
    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody @Valid UserReqDto.JoinReqDto joinReqDto, BindingResult bindingResult) {
        UserRespDto.JoinRespDto joinRespDto = userService.join(joinReqDto);
        return new ResponseEntity<>(new ResponseDto<>(1, "회원가입 성공", joinRespDto), HttpStatus.CREATED);
    }

    @PostMapping("/find-username")
    public ResponseEntity<?> findUsername(@RequestBody @Valid UserReqDto.FindUsernameReqDto findUsernameReqDto) {
        mailService.sendUsernameEmail(findUsernameReqDto.getEmail());
        return new ResponseEntity<>(
                new ResponseDto<>(1, "입력하신 이메일로 아이디가 전송되었습니다.", null),
                HttpStatus.OK);
    }

    // 비밀번호 찾기
    @PostMapping("/find-pwd")
    public ResponseEntity<?> findPassword(@Valid @RequestBody UserReqDto.FindPwdReqDto findPwdReqDto) {
        // 1. 사용자 존재 확인
        userRepository.findByEmail(findPwdReqDto.getEmail())
                .orElseThrow(() -> new CustomApiException("해당 이메일로 등록된 계정이 없습니다."));

        // 2. 비밀번호 재설정 이메일 발송
        mailService.sendPasswordResetEmail(findPwdReqDto.getEmail());

        return ResponseEntity.ok(
                new ResponseDto<>(1, "비밀번호 재설정 링크가 이메일로 발송되었습니다.", null));
    }

    @PostMapping("/pwd-reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody UserReqDto.ResetPasswordReqDto resetPasswordReqDto) {
        // 1. 토큰 검증
        EmailVerification verification = verificationRepository
                .findByEmailAndAuthNumberAndVerifiedFalse(resetPasswordReqDto.getEmail(),
                        resetPasswordReqDto.getToken())
                .orElseThrow(() -> new CustomApiException("유효하지 않거나 만료된 링크입니다."));

        if (verification.isExpired()) {
            verificationRepository.delete(verification);
            throw new CustomApiException("링크가 만료되었습니다. 비밀번호 찾기를 다시 시도해주세요.");
        }

        // 2. 비밀번호 변경
        userService.resetPassword(resetPasswordReqDto.getEmail(), resetPasswordReqDto.getNewPassword());

        // 3. 검증 정보 업데이트
        verification.verify();
        verificationRepository.save(verification);

        return ResponseEntity.ok(
                new ResponseDto<>(1, "비밀번호가 성공적으로 변경되었습니다.", null));
    }

    // 비밀번호 변경
    @PostMapping(value = { "/v1/change-pwd", "/change-pwd" })
    public ResponseEntity<?> changePwd(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestBody @Valid UserReqDto.ChangePwdReqDto changePwdReqDto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors()
                    .stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(-1, "입력값 검증 실패", errorMessages));
        }

        UserRespDto.ChangePwdRespDto changePwdRespDto = userService.changePwd(loginUser.getUser().getEmail(),
                changePwdReqDto.getPassword());

        // 401 응답으로 클라이언트가 로그아웃 처리하도록 함
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ResponseDto<>(1, "비밀번호가 변경되었습니다. 다시 로그인해 주세요.", changePwdRespDto));
    }

    // 아이디 중복 확인
    @GetMapping("/username-duplicate")
    public ResponseEntity<?> usernameDuplicate(
            @RequestBody @Valid UserReqDto.UsernameDuplicateReqDto usernameDuplicateReqDto) {
        UserRespDto.UsernameDuplicateRespDto usernameDuplicateRespDto = userService
                .usernameDuplicate(usernameDuplicateReqDto);

        if (!usernameDuplicateRespDto.isAvailable()) {
            return new ResponseEntity<>(new ResponseDto<>(-1, "해당 아이디는 이미 사용중 입니다.", usernameDuplicateRespDto),
                    HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(new ResponseDto<>(1, "사용 가능한 아이디입니다.", usernameDuplicateRespDto),
                    HttpStatus.OK);
        }
    }

    // 내 정보 불러오기
    @GetMapping("/v1/my-profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal LoginUser loginUser) {
        UserRespDto.ProfileRespDto profileRespDto = userService.getProfile(loginUser.getUser().getId());
        return new ResponseEntity<>(new ResponseDto<>(1, "회원 정보 불러오기 성공", profileRespDto), HttpStatus.OK);
    }

    /**
     * 회원 탈퇴
     * 
     * @param loginUser
     * @return
     */
    @PostMapping("/v1/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal LoginUser loginUser) {
        try {
            userService.withdraw(loginUser.getUser().getId());
            return new ResponseEntity<>(new ResponseDto<>(1, "회원 탈퇴 완료", null), HttpStatus.OK);
        } catch (CustomApiException e) {
            return new ResponseEntity<>(new ResponseDto<>(-1, e.getMessage(), null), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseDto<>(-1, "회원 탈퇴 중 오류 발생", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
