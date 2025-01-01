package com.qriz.sqld.oauth.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.config.jwt.JwtProcess;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserEnum;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.oauth.dto.OAuth2LoginResult;
import com.qriz.sqld.oauth.dto.SocialReqDto;
import com.qriz.sqld.oauth.info.OAuth2UserInfo;
import com.qriz.sqld.oauth.info.impl.GoogleOAuth2UserInfo;
import com.qriz.sqld.oauth.info.impl.KakaoOAuth2UserInfo;
import com.qriz.sqld.oauth.provider.Provider;

import org.springframework.security.oauth2.core.OAuth2Error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OAuth2Service {
        private final UserRepository userRepository;
        private final RestTemplate restTemplate;

        @Value("${oauth2.google.client-id}")
        private String googleClientId;

        @Value("${oauth2.google.android.client-id:${oauth2.google.client-id}}")
        private String googleAndroidClientId;

        @Value("${oauth2.google.ios.client-id:${oauth2.google.client-id}}")
        private String googleIosClientId;

        @Value("${oauth2.kakao.client-id}")
        private String kakaoClientId;

        private String getGoogleClientId(String platform) {
                if ("android".equalsIgnoreCase(platform)) {
                        return googleAndroidClientId;
                } else if ("ios".equalsIgnoreCase(platform)) {
                        return googleIosClientId;
                }
                return googleClientId;
        }

        public OAuth2LoginResult processOAuth2Login(SocialReqDto socialReqDto) {
                String provider = socialReqDto.getProvider().toUpperCase();
                String idToken = socialReqDto.getAuthCode();
                String platform = socialReqDto.getPlatform();

                // Provider별 토큰 검증 및 사용자 정보 획득
                OAuth2UserInfo userInfo = verifyTokenAndGetUserInfo(
                                Provider.valueOf(provider),
                                idToken,
                                platform);

                // 사용자 조회 또는 생성
                User user = userRepository.findByEmail(userInfo.getEmail())
                                .orElseGet(() -> createNewUser(userInfo, provider));

                // JWT 토큰 생성
                LoginUser loginUser = new LoginUser(user);
                String accessToken = JwtProcess.createAccessToken(loginUser);
                String refreshToken = JwtProcess.createRefreshToken(loginUser);

                log.debug("Social login successful for user: {}", user.getEmail());

                return OAuth2LoginResult.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .user(user)
                                .build();
        }

        private OAuth2UserInfo verifyTokenAndGetUserInfo(Provider provider, String token, String platform) {
                switch (provider) {
                        case GOOGLE:
                                return verifyGoogleToken(token, platform);
                        case KAKAO:
                                return verifyKakaoToken(token);
                        default:
                                throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
                }
        }

        private OAuth2UserInfo verifyGoogleToken(String idToken, String platform) {
                try {
                        // Google Token Info endpoint 호출
                        String googleTokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

                        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                        googleTokenInfoUrl,
                                        HttpMethod.GET,
                                        null,
                                        new ParameterizedTypeReference<Map<String, Object>>() {
                                        });

                        // 토큰 유효성 검증
                        Map<String, Object> attributes = response.getBody();

                        // platform이 null이면 기본값으로 "web" 사용
                        validateGoogleTokenClaims(attributes, platform != null ? platform : "web");

                        return new GoogleOAuth2UserInfo(attributes);
                } catch (Exception e) {
                        log.error("Google token verification failed", e);
                        throw new OAuth2AuthenticationException(
                                        new OAuth2Error("invalid_token",
                                                        "Failed to verify Google token: " + e.getMessage(), null));
                }
        }

        private void validateGoogleTokenClaims(Map<String, Object> claims, String platform) {
                try {
                        String aud = (String) claims.get("aud");
                        String expectedClientId = getGoogleClientId(platform);

                        // 디버깅을 위한 로그 추가
                        log.debug("Token aud: {}", aud);
                        log.debug("Expected client ID: {}", expectedClientId);

                        if (!expectedClientId.equals(aud)) {
                                throw new OAuth2AuthenticationException(
                                                new OAuth2Error("invalid_token",
                                                                String.format("Invalid token audience. Expected: %s, Got: %s",
                                                                                expectedClientId, aud),
                                                                null));
                        }
                        // ... 나머지 코드
                } catch (Exception e) {
                        log.error("Token validation failed", e);
                        throw new OAuth2AuthenticationException(
                                        new OAuth2Error("invalid_token", "Token validation failed: " + e.getMessage(),
                                                        null));
                }
        }

        private OAuth2UserInfo verifyKakaoToken(String accessToken) {
                try {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(accessToken);

                        HttpEntity<String> entity = new HttpEntity<>(headers);
                        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                        "https://kapi.kakao.com/v2/user/me",
                                        HttpMethod.GET,
                                        entity,
                                        new ParameterizedTypeReference<Map<String, Object>>() {
                                        });

                        Map<String, Object> body = response.getBody();
                        log.debug("Kakao API response: {}", body); // 카카오에서 받은 데이터 확인

                        return new KakaoOAuth2UserInfo(body);
                } catch (Exception e) {
                        throw new OAuth2AuthenticationException(
                                        new OAuth2Error("invalid_token",
                                                        "Failed to verify Kakao token: " + e.getMessage(), null));
                }
        }

        private User createNewUser(OAuth2UserInfo userInfo, String provider) {
                // provider를 대문자로 저장
                String upperProvider = provider.toUpperCase();

                User newUser = User.builder()
                                .email(userInfo.getEmail())
                                .nickname(userInfo.getName())
                                .username(userInfo.getEmail())
                                .provider(upperProvider) // 대문자로 저장
                                .providerId(userInfo.getId())
                                .role(UserEnum.CUSTOMER)
                                .build();

                log.debug("Creating new user with social login: {}", userInfo.getEmail());
                return userRepository.save(newUser);
        }
}