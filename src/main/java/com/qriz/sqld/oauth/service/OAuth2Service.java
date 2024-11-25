package com.qriz.sqld.oauth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.config.jwt.JwtProcess;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserEnum;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.oauth.dto.OAuth2LoginResult;
import com.qriz.sqld.oauth.dto.SocialReqDto;
import com.qriz.sqld.oauth.info.OAuth2UserInfo;
import com.qriz.sqld.oauth.info.OAuth2UserInfoFactory;
import com.qriz.sqld.oauth.provider.Provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OAuth2Service {
        private final ClientRegistrationRepository clientRegistrationRepository;
        private final DefaultAuthorizationCodeTokenResponseClient authorizationCodeTokenResponseClient;
        private final UserRepository userRepository;

        @Value("${oauth2.redirect-uri}")
        private String redirectUri;

        public OAuth2LoginResult processOAuth2Login(SocialReqDto socialReqDto) {
                String authCode = socialReqDto.getAuthCode();
                String provider = socialReqDto.getProvider();

                // 소셜 로그인 프로세스
                ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);

                OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                                .clientId(clientRegistration.getClientId())
                                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                                .redirectUri(redirectUri)
                                .scopes(clientRegistration.getScopes())
                                .state("state")
                                .build();

                OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(authCode)
                                .redirectUri(redirectUri)
                                .state("state")
                                .build();

                OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(
                                authorizationRequest,
                                authorizationResponse);

                OAuth2AuthorizationCodeGrantRequest authCodeGrantRequest = new OAuth2AuthorizationCodeGrantRequest(
                                clientRegistration,
                                authorizationExchange);

                // 소셜 토큰 획득
                OAuth2AccessTokenResponse tokenResponse = authorizationCodeTokenResponseClient
                                .getTokenResponse(authCodeGrantRequest);

                // 사용자 정보 획득
                OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration,
                                tokenResponse.getAccessToken());
                DefaultOAuth2UserService userService = new DefaultOAuth2UserService();
                OAuth2User oauth2User = userService.loadUser(userRequest);

                // 사용자 정보 파싱
                OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                                Provider.valueOf(provider.toUpperCase()),
                                oauth2User.getAttributes());

                // 사용자 조회 또는 생성
                User user = userRepository.findByEmail(userInfo.getEmail())
                                .orElseGet(() -> createNewUser(userInfo, provider));

                // JWT 토큰 생성
                LoginUser loginUser = new LoginUser(user);
                String accessToken = JwtProcess.createAccessToken(loginUser);
                String refreshToken = JwtProcess.createRefreshToken(loginUser);

                log.debug("Social login successful for user: {}", user.getEmail());

                // OAuth2LoginResult 객체로 리턴
                return OAuth2LoginResult.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .user(user)
                                .build();
        }

        private User createNewUser(OAuth2UserInfo userInfo, String provider) {
                User newUser = User.builder()
                                .email(userInfo.getEmail())
                                .nickname(userInfo.getName())
                                .provider(provider)
                                .providerId(userInfo.getId())
                                .role(UserEnum.CUSTOMER)
                                .build();

                log.debug("Creating new user with social login: {}", userInfo.getEmail());
                return userRepository.save(newUser);
        }
}
