package com.qriz.sqld.service;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserEnum;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.dto.oauth2.OAuth2UserInfo;
import com.qriz.sqld.util.RedisUtil;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RedisUtil redisUtil;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private BCryptPasswordEncoder bCryptPasswordEncoder;

    /*
        userRequest 데이터에 대한 후처리 함수
        함수 종료 시 @AuthenticationPrincipal 어노테이션 생성
        access token을 이용해 서버로부터 사용자 정보를 받아옴 (DefaultOAuth2UserService에 구현되어 있기 때문에
         super.loadUser()로 호출
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {

        /*
          "registraionId" 로 어떤 OAuth 로 로그인 했는지 확인 (google,naver등)
          loadUser를 통해 회원의 profile을 불러올 수 있다.

          1. 로그인 버튼
          2. 로그인 창
          3. 로그인 성공
          4. authorization code 리턴 (OAuth-Client 라이브러리가 동작)
          5. 리턴된 code를 이용하여 Access Token을 요청
        */

        System.out.println("getClientRegistration: " + oAuth2UserRequest.getClientRegistration());
        System.out.println("getAccessToken: " + oAuth2UserRequest.getAccessToken().getTokenValue());
        System.out.println("getAttributes: " + super.loadUser(oAuth2UserRequest).getAttributes());

        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        log.info("getAttributes : {}", oAuth2User.getAttributes());

        // accessToken 가져오기
        String accessToken = oAuth2UserRequest.getAccessToken().getTokenValue();

        // provider 가져오기
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();

        System.out.println("registrationId: " + registrationId);

        // OAuth2UserInfo 객체 생성
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());

        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        String username = registrationId + "_" + providerId;
        String nickname = oAuth2UserInfo.getName();

        Optional<User> optionalUser = userRepository.findByUsername(username);
        User user = null;

        // 소셜 로그인 사용자 정보 저장
        try {
            if (optionalUser.isEmpty()) {
                log.info("신규 가입 사용자");
                user = User.builder()
                        .username(username)
                        .nickname(nickname)
                        .provider(registrationId)
                        .accessToken(accessToken)
                        .email(email)
                        .providerId(providerId)
                        .role(UserEnum.CUSTOMER)
                        .createdAt(LocalDateTime.now())
                        .build();

                userRepository.save(user);
                log.info("신규 가입자 저장됨: {}", user.getUsername());
            } else {
                log.info("기존 가입자");
                user = optionalUser.get();
            }

            // Redis 에 accessToken 저장
            redisUtil.setDataExpire("accessToken:" + user.getUsername(), accessToken, 60 * 60 * 24); // 24 시간 만료

            return new LoginUser(user, oAuth2User.getAttributes());
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }
}
