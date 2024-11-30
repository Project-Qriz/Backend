package com.qriz.sqld.service.user;

import com.qriz.sqld.domain.apply.UserApplyRepository;
import com.qriz.sqld.domain.daily.UserDailyRepository;
import com.qriz.sqld.domain.preview.UserPreviewTestRepository;
import com.qriz.sqld.domain.skillLevel.SkillLevelRepository;
import com.qriz.sqld.domain.survey.SurveyRepository;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.user.UserReqDto;
import com.qriz.sqld.domain.UserActivity.UserActivityRepository;
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
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserDailyRepository userDailyRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final UserPreviewTestRepository userPreviewTestRepository;
    private final UserActivityRepository userActivityRepository;
    private final SurveyRepository surveyRepository;
    private final UserApplyRepository userApplyRepository;

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
        Optional<User> user = userRepository.findByEmail(findUsernameReqDto.getEmail());

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
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));

        // 2. 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(password);
        user.setPassword(encodedPassword);
        userRepository.save(user);

        return new UserRespDto.ChangePwdRespDto(user.getUsername(), "비밀번호가 변경되었습니다.");
    }

    // 아이디 중복확인
    @Transactional
    public UserRespDto.UsernameDuplicateRespDto usernameDuplicate(
            UserReqDto.UsernameDuplicateReqDto usernameDuplicateReqDto) {
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

    // 내 정보 불러오기
    @Transactional(readOnly = true)
    public UserRespDto.ProfileRespDto getProfile(Long userId) {
        User userOp = userRepository.findById(userId).orElseThrow(() -> new CustomApiException("존재하지 않는 사용자 입니다."));
        return new UserRespDto.ProfileRespDto(userOp);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomApiException("존재하지 않는 사용자 입니다."));

        // 1. UserDaily 삭제
        userDailyRepository.deleteByUser(user);

        // 2. SkillLevel 삭제
        skillLevelRepository.deleteByUser(user);

        // 3. UserPreviewTest 삭제
        userPreviewTestRepository.deleteByUser(user);

        // 4. UserActivity 삭제
        userActivityRepository.deleteByUser(user);

        // 5. Survey 삭제
        surveyRepository.deleteByUser(user);

        // 6. UserApply 삭제
        userApplyRepository.deleteByUser(user);

        // 7. user 삭제
        userRepository.delete(user);
    }
}
