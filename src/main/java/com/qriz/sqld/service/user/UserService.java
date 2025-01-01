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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserDailyRepository userDailyRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final UserPreviewTestRepository userPreviewTestRepository;
    private final UserActivityRepository userActivityRepository;
    private final SurveyRepository surveyRepository;
    private final UserApplyRepository userApplyRepository;
    private final RestTemplate restTemplate;

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
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomApiException("존재하지 않는 사용자 입니다."));

        // 소셜 연결 해제
        if (user.getProvider() != null) {
            disconnectSocialAccount(user);
        }

        // 기존 데이터 삭제 로직
        userDailyRepository.deleteByUser(user);
        skillLevelRepository.deleteByUser(user);
        userPreviewTestRepository.deleteByUser(user);
        userActivityRepository.deleteByUser(user);
        surveyRepository.deleteByUser(user);
        userApplyRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private void disconnectSocialAccount(User user) {
        switch (user.getProvider().toUpperCase()) {
            case "GOOGLE":
                disconnectGoogle(user);
                break;
            case "KAKAO":
                disconnectKakao(user);
                break;
            default:
                throw new CustomApiException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
    }

    private void disconnectGoogle(User user) {
        try {
            // Google의 경우 토큰 취소 엔드포인트 호출
            String revokeEndpoint = "https://accounts.google.com/o/oauth2/revoke?token=" + user.getProviderId();
            restTemplate.getForObject(revokeEndpoint, String.class);
        } catch (Exception e) {
            log.warn("Google 계정 연결 해제 중 오류 발생: {}", e.getMessage());
            // 실패해도 계속 진행 (사용자 데이터는 삭제)
        }
    }

    private void disconnectKakao(User user) {
        try {
            String kakaoApiUrl = "https://kapi.kakao.com/v1/user/unlink";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "KakaoAK " + "Admin_키");  // 관리자 키 필요

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("target_id_type", "user_id");
            params.add("target_id", user.getProviderId());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            restTemplate.postForObject(kakaoApiUrl, request, String.class);
        } catch (Exception e) {
            log.warn("Kakao 계정 연결 해제 중 오류 발생: {}", e.getMessage());
            // 실패해도 계속 진행 (사용자 데이터는 삭제)
        }
    }
}
