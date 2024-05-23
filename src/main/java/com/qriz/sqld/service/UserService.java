package com.qriz.sqld.service;

import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.user.UserReqDto;
import com.qriz.sqld.dto.user.UserRespDto;
import com.qriz.sqld.handler.ex.CustomApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // 회원 가입
    @Transactional
    public UserRespDto.JoinRespDto join(UserReqDto.JoinReqDto joinReqDto) {
        // 1. 동일 유저네임 존재 검사
        Optional<User> userOP = userRepository.findByUsername(joinReqDto.getUsername());
        if (userOP.isPresent()) {
            throw new CustomApiException("동일한 username이 존재합니다.");
        }

        // 2. 패스워드 인코딩 + 회원가입
        User userPS = userRepository.save(joinReqDto.toEntity(passwordEncoder));

        // 3. dto 응답
        return new UserRespDto.JoinRespDto(userPS);
    }

    // 아이디 찾기
    @Transactional
    public UserRespDto.FindUsernameRespDto findUsername(UserReqDto.FindUsernameReqDto findUsernameReqDto) {
        // 1. 입력 닉네임과 이메일에 해당하는 계정이 있는지 검사
        Optional<User> user = userRepository.findByNicknameAndEmail(findUsernameReqDto.getNickname(),
                findUsernameReqDto.getEmail());

        // 2. 사용자가 존재하지 않을 경우 예외 처리
        if (!user.isPresent()) {
            throw new CustomApiException("해당 계정이 존재하지 않습니다.");
        }

        return new UserRespDto.FindUsernameRespDto(user.get());
    }

    // 비밀번호 변경
    @Transactional
    public UserRespDto.ChangePwdRespDto changePwd(String username, String password) {
        // 1. 사용자 찾기
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));

        // 2. 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(password);
        user.setPassword(encodedPassword);
        userRepository.save(user);

        return new UserRespDto.ChangePwdRespDto(user.getUsername(), "비밀번호가 변경되었습니다.");
    }

    // 아이디 중복확인
    @Transactional
    public UserRespDto.UsernameDuplicateRespDto usernameDuplicate(UserReqDto.UsernameDuplicateReqDto usernameDuplicateReqDto) {
        // 1. 사용자 찾기
        Optional<User> userOP = userRepository.findByUsername(usernameDuplicateReqDto.getUsername());
        
        // 2. 사용자 존재 여부에 따라 응답 생성
        if (userOP.isPresent()) {
            // 아이디가 이미 사용 중인 경우
            return new UserRespDto.UsernameDuplicateRespDto(false);
        } else {
            // 사용 기능한 아이디인 경우
            return new UserRespDto.UsernameDuplicateRespDto(true);
        }
    }

    // 이메일 중복확인
    @Transactional
    public UserRespDto.EmailDuplicateRespDto emailDuplicate(UserReqDto.EmailDuplicateReqDto emailDuplicateReqDto) {
        // 1. 이메일이 존재하는지 찾기
        Optional<User> userOP = userRepository.findByEmail(emailDuplicateReqDto.getEmail());
        
        // 2. 이메일 존재 여부에 따라 응답 생성
        if (userOP.isPresent()) {
            // 이메일 이미 사용 중인 경우
            return new UserRespDto.EmailDuplicateRespDto(false);
        } else {
            // 사용 기능한 이메일인 경우
            return new UserRespDto.EmailDuplicateRespDto(true);
        }
    }
}
