package com.qriz.sqld.oauth.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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
import com.qriz.sqld.config.auth.RefreshToken;
import com.qriz.sqld.config.auth.RefreshTokenRepository;
import com.qriz.sqld.config.jwt.JwtProcess;
import com.qriz.sqld.config.jwt.JwtVO;
import com.qriz.sqld.domain.preview.PreviewTestStatus;
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
        private final RefreshTokenRepository refreshTokenRepository;

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

                // 해당 이메일로 가입된 계정 조회
                Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

                User user;

                if (existingUser.isPresent()) {
                        user = existingUser.get();
                        // 일반 회원가입 사용자인 경우 (provider가 null)
                        if (user.getProvider() == null) {
                                throw new OAuth2AuthenticationException(
                                                new OAuth2Error(
                                                                "email_already_exists",
                                                                "This email is already registered as a regular member. Please use regular login.",
                                                                null));
                        }
                        // 다른 소셜 로그인으로 가입한 경우
                        if (!provider.equals(user.getProvider())) {
                                throw new OAuth2AuthenticationException(
                                                new OAuth2Error(
                                                                "wrong_social_provider",
                                                                String.format("This email is already registered with %s. Please use %s login.",
                                                                                user.getProvider().toLowerCase(),
                                                                                user.getProvider().toLowerCase()),
                                                                null));
                        }
                } else {
                        // 새 사용자 생성
                        user = createNewUser(userInfo, provider);
                }

                // JWT 토큰 생성
                LoginUser loginUser = new LoginUser(user);
                String accessToken = JwtProcess.createAccessToken(loginUser);

                // Refresh Token 생성 및 DB 저장
                String refreshToken = JwtProcess.createRefreshToken(loginUser);
                RefreshToken refreshTokenEntity = new RefreshToken(
                                user.getId(),
                                refreshToken,
                                LocalDateTime.now().plusSeconds(JwtVO.REFRESH_TOKEN_EXPIRATION_TIME));
                refreshTokenRepository.save(refreshTokenEntity);

                log.debug("Social login successful for user: {}", user.getEmail());

                return OAuth2LoginResult.builder()
                                .accessToken(accessToken)
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
                                .previewTestStatus(PreviewTestStatus.NOT_STARTED)
                                .build();

                log.debug("Creating new user with social login: {}", userInfo.getEmail());
                return userRepository.save(newUser);
        }
}