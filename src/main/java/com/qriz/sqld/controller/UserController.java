package com.qriz.sqld.controller;

import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.ResponseDto;
import com.qriz.sqld.dto.user.UserReqDto;
import com.qriz.sqld.dto.user.UserRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import com.qriz.sqld.service.MailSendService;
import com.qriz.sqld.service.UserService;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;


@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class UserController {

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final MailSendService mailService;

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody @Valid UserReqDto.JoinReqDto joinReqDto, BindingResult bindingResult) {
        UserRespDto.JoinRespDto joinRespDto = userService.회원가입(joinReqDto);
        return new ResponseEntity<>(new ResponseDto<>(1, "회원가입 성공", joinRespDto), HttpStatus.CREATED);
    }

    @GetMapping("/find-username")
    public ResponseEntity<?> findUSername(@RequestBody @Valid UserReqDto.FindUsernameReqDto findUsernameReqDto,
            BindingResult bindingResult) {
        UserRespDto.FindUsernameRespDto findUsernameRespDto = userService.findUsername(findUsernameReqDto);
        return new ResponseEntity<>(new ResponseDto<>(1, "아이디 찾기 성공", findUsernameRespDto), HttpStatus.OK);
    }

    @PostMapping("/find-pwd")
    public ResponseEntity<?> findPwd(@RequestBody @Valid UserReqDto.FindPwdReqDto findPwdReqDto,
            BindingResult bindingResult, HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors()
                    .stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.toList());

            // 클라이언트에 에러 응답 전송
            return ResponseEntity.badRequest().body(new ResponseDto<>(-1, "입력값 검증 실패", errorMessages));
        }

        // 해당 계정이 있는지 확인
        Optional<User> userOpt = userRepository.findByUsernameAndEmail(findPwdReqDto.getUsername(),
                findPwdReqDto.getEmail());

        logger.info("사용자 아이디: '{}' 와 이메일: '{}'", findPwdReqDto.getUsername(),
                findPwdReqDto.getEmail());

        try {
            if (userOpt.isPresent()) {

                // 계정이 확인되면 이메일 전송
            // 사용자 정보를 세션에 저장
            HttpSession session = request.getSession();
            session.setAttribute("username", findPwdReqDto.getUsername());
            session.setMaxInactiveInterval(300); // 세션 유지 시간 5분

            mailService.joinEmail(findPwdReqDto.getEmail());

            return new ResponseEntity<>(new ResponseDto<>(1, "해당 이메일로 인증번호가 전송되었습니다.", null), HttpStatus.OK);
            } else {
                return ResponseEntity.badRequest()
                    .body(new ResponseDto<>(-1, "해당 계정이 존재하지 않습니다. 아이디 혹은 이메일을 확인해주세요.", null));
            }
        } catch (CustomApiException e) {
            logger.error("디버깅", e.getMessage());
            return new ResponseEntity<>(new ResponseDto<>(-1, "에러: " + e.getMessage(), null), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            logger.error("예외 처리", e.getMessage());
            return new ResponseEntity<>(new ResponseDto<>(-1, "예외 발생", null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = { "/v1/change-pwd", "/change-pwd" })
    public ResponseEntity<?> ChangePwd(@RequestBody @Valid UserReqDto.ChangePwdReqDto changePwdReqDto,
            BindingResult bindingResult, HttpSession session) {

        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors()
                    .stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.toList());

            // 클라이언트에 에러 응답 전송
            return ResponseEntity.badRequest().body(new ResponseDto<>(-1, "입력값 검증 실패", errorMessages));
        }

        // 1. 세션에서 사용자 식별 정보 확인
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 시간이 만료되었습니다. 재시도 해주세요");
        }
        // 2. 비밀번호 변경
        UserRespDto.ChangePwdRespDto changePwdRespDto = userService.changePwd(username, changePwdReqDto.getPassword());
        // 3. 비밀번호 변경 후 세션 무효화
        session.invalidate();
        return new ResponseEntity<>(new ResponseDto<>(1, "비밀번호 변경 성공", changePwdRespDto), HttpStatus.OK);
    }

    @GetMapping("/username-duplicate")
    public ResponseEntity<?> usernameDuplicate(@RequestBody @Valid UserReqDto.UsernameDuplicateReqDto usernameDuplicateReqDto) {
        UserRespDto.UsernameDuplicateRespDto usernameDuplicateRespDto = userService.usernameDuplicate(usernameDuplicateReqDto);

        if (!usernameDuplicateRespDto.isAvailable()) {
            return new ResponseEntity<>(new ResponseDto<>(-1, "해당 아이디는 이미 사용중 입니다.", usernameDuplicateRespDto), HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(new ResponseDto<>(1, "사용 가능한 아이디입니다.", usernameDuplicateRespDto), HttpStatus.OK);
        }
    }

    @GetMapping("/email-duplicate")
    public ResponseEntity<?> emailDuplicate(@RequestBody @Valid UserReqDto.EmailDuplicateReqDto emailDuplicateReqDto) {
        UserRespDto.EmailDuplicateRespDto emailDuplicateRespDto = userService.emailDuplicate(emailDuplicateReqDto);
        
        if (!emailDuplicateRespDto.isAvailable()) {
            return new ResponseEntity<>(new ResponseDto<>(-1, "해당 이메일은 이미 사용중 입니다.", emailDuplicateRespDto), HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(new ResponseDto<>(1, "사용 가능한 이메일입니다.", emailDuplicateRespDto), HttpStatus.OK);
        }
    }
}
