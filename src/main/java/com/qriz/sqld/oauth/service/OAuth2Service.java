package com.qriz.sqld.oauth.service;

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
import com.qriz.sqld.config.jwt.JwtVO;
import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserEnum;
import com.qriz.sqld.domain.user.UserRepository;
import com.qriz.sqld.oauth.dto.SocialReqDto;
import com.qriz.sqld.oauth.info.OAuth2UserInfo;
import com.qriz.sqld.oauth.info.OAuth2UserInfoFactory;
import com.qriz.sqld.oauth.provider.Provider;
import com.qriz.sqld.util.RedisUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class OAuth2Service {
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final DefaultAuthorizationCodeTokenResponseClient authorizationCodeTokenResponseClient;
    private final UserRepository userRepository;
    private final JwtProcess jwtProcess;
    private final RedisUtil redisUtil;

    @Value("${oauth2.redirect-uri}")
    private String redirectUri;

    public String processOAuth2Login(SocialReqDto socialReqDto) {
        String authCode = socialReqDto.getAuthCode();
        String provider = socialReqDto.getProvider();

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

        OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);

        OAuth2AuthorizationCodeGrantRequest authCodeGrantRequest = new OAuth2AuthorizationCodeGrantRequest(clientRegistration, authorizationExchange);

        OAuth2AccessTokenResponse tokenResponse = authorizationCodeTokenResponseClient.getTokenResponse(authCodeGrantRequest);

        OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, tokenResponse.getAccessToken());
        DefaultOAuth2UserService userService = new DefaultOAuth2UserService();
        OAuth2User oauth2User = userService.loadUser(userRequest);

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(Provider.valueOf(provider.toUpperCase()), oauth2User.getAttributes());

        User user = userRepository.findByEmail(userInfo.getEmail()).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(userInfo.getEmail())
                    .nickname(userInfo.getName())
                    .provider(provider)
                    .providerId(userInfo.getId())
                    .role(UserEnum.CUSTOMER)
                    .build();
            userRepository.save(user);
        }

        String accessToken = jwtProcess.createAccessToken(new LoginUser(user));
        String refreshToken = jwtProcess.createRefreshToken(new LoginUser(user));

        redisUtil.setDataExpire("AT" + user.getEmail(), accessToken, JwtVO.ACCESS_TOKEN_EXPIRATION_TIME / 1000);
        redisUtil.setDataExpire("RT:" + user.getEmail(), refreshToken, JwtVO.REFRESH_TOKEN_EXPIRATION_TIME / 1000);

        return accessToken;
    }
}