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
import com.qriz.sqld.mail.domain.EmailVerification.EmailVerification;
import com.qriz.sqld.mail.domain.EmailVerification.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final EmailVerificationRepository verificationRepository;
    private final UserActivityRepository userActivityRepository;
    private final SurveyRepository surveyRepository;
    private final UserApplyRepository userApplyRepository;
    private final RestTemplate restTemplate;

    @Value("${oauth2.google.client-id}")
    private String googleClientId;

    @Value("${oauth2.kakao.admin-key}")
    private String kakaoAdminKey;

    // 회원 가입
    @Transactional
    public UserRespDto.JoinRespDto join(UserReqDto.JoinReqDto joinReqDto) {
        // 1. 동일 유저네임 존재 검사
        Optional<User> userOP = userRepository.findByUsername(joinReqDto.getUsername());
        if (userOP.isPresent()) {
            throw new CustomApiException("동일한 username이 존재합니다.");
        }

        // 2. 이메일 중복 검사
        Optional<User> emailUser = userRepository.findByEmail(joinReqDto.getEmail());
        if (emailUser.isPresent()) {
            User existingUser = emailUser.get();
            if (existingUser.getProvider() != null) {
                throw new CustomApiException(
                        String.format("이미 %s 소셜 계정으로 가입된 이메일입니다. %s 로그인을 이용해주세요.",
                                existingUser.getProvider().toLowerCase(),
                                existingUser.getProvider().toLowerCase()));
            } else {
                throw new CustomApiException("이미 가입된 이메일입니다.");
            }
        }

        // 3. 패스워드 인코딩 + 회원가입
        User userPS = userRepository.save(joinReqDto.toEntity(passwordEncoder));

        // 4. dto 응답
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
    public UserRespDto.UsernameDuplicateRespDto usernameDuplicate(String username) {
        // 1. 사용자 찾기
        Optional<User> userOP = userRepository.findByUsername(username);

        // 2. 사용자 존재 여부에 따라 응답 생성
        if (userOP.isPresent()) {
            // 아이디가 이미 사용 중인 경우
            return new UserRespDto.UsernameDuplicateRespDto(false);
        } else {
            // 사용 가능한 아이디인 경우
            return new UserRespDto.UsernameDuplicateRespDto(true);
        }
    }

    @Transactional
    public void resetPassword(String newPassword, String resetToken) {
        // 토큰으로 특정 인증 정보를 찾음
        EmailVerification verification = verificationRepository
                .findByResetTokenAndVerifiedTrue(resetToken)
                .orElseThrow(() -> new CustomApiException("유효하지 않은 인증 정보입니다."));

        // 토큰의 만료 여부 확인
        if (verification.isExpired()) {
            throw new CustomApiException("인증이 만료되었습니다.");
        }

        User user = userRepository.findByEmail(verification.getEmail())
                .orElseThrow(() -> new CustomApiException("사용자를 찾을 수 없습니다."));

        user.setPassword(passwordEncoder.encode(newPassword));

        // 사용된 인증 정보 삭제
        verificationRepository.delete(verification);
    }

    @Transactional
    public void withdraw(Long userId, UserReqDto.WithdrawReqDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException("존재하지 않는 사용자 입니다."));

        // 소셜 회원인 경우에만 연결 해제 시도
        if (user.getProvider() != null && request != null && request.getAccessToken() != null) {
            disconnectSocialAccount(user, request.getAccessToken());
        }

        // 기존 데이터 삭제 로직은 동일하게 유지
        userDailyRepository.deleteByUser(user);
        skillLevelRepository.deleteByUser(user);
        userPreviewTestRepository.deleteByUser(user);
        userActivityRepository.deleteByUser(user);
        surveyRepository.deleteByUser(user);
        userApplyRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private void disconnectSocialAccount(User user, String accessToken) {
        switch (user.getProvider().toUpperCase()) {
            case "GOOGLE":
                disconnectGoogle(user, accessToken);
                break;
            case "KAKAO":
                disconnectKakao(user, accessToken);
                break;
            default:
                throw new CustomApiException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
    }

    private void disconnectGoogle(User user, String accessToken) {
        try {
            String revokeUrl = "https://oauth2.googleapis.com/revoke";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("token", accessToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            restTemplate.postForObject(revokeUrl, request, Void.class);
            log.info("Google account disconnected successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to disconnect Google account: {}", e.getMessage());
        }
    }

    private void disconnectKakao(User user, String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(accessToken); // Admin 키가 아닌 access token 사용

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);
            restTemplate.postForObject("https://kapi.kakao.com/v1/user/unlink", request, String.class);
            log.info("Kakao account disconnected successfully for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to disconnect Kakao account: {}", e.getMessage());
        }
    }

    // 내 정보 불러오기
    @Transactional(readOnly = true)
    public UserRespDto.UserInfoRespDto getUserInfo(Long userId) {
        User userOp = userRepository.findById(userId).orElseThrow(() -> new CustomApiException("존재하지 않는 사용자 입니다."));
        return new UserRespDto.UserInfoRespDto(userOp);
    }

}
