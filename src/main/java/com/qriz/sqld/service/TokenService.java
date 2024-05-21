package com.qriz.sqld.service;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.stereotype.Service;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.config.jwt.JwtProcess;
import com.qriz.sqld.config.jwt.JwtVO;
import com.qriz.sqld.dto.token.TokenRespDto;
import com.qriz.sqld.util.RedisUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TokenService {

    private final RedisUtil redisUtil;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

    public TokenRespDto refreshJwtAccessToken(String refreshToken) {
        try {
            if (refreshToken != null && JwtProcess.verify(refreshToken) != null) {
                LoginUser loginUser = JwtProcess.verify(refreshToken);
                Object storedRefreshToken = redisUtil.getData("RT:" + loginUser.getUsername());

                if (storedRefreshToken != null && storedRefreshToken.equals(refreshToken)) {
                    String newAccessToken = JwtProcess.createAccessToken(loginUser);
                    redisUtil.setDataExpire("AT:" + loginUser.getUsername(), newAccessToken, JwtVO.ACCESS_TOKEN_EXPIRATION_TIME / 1000);
                    return new TokenRespDto(newAccessToken);
                } else {
                    throw new AuthenticationException("유효하지 않은 Refresh Token") {};
                }
            } else {
                throw new AuthenticationException("유효하지 않은 Refresh Token") {};
            }
        } catch (Exception e) {
            throw new AuthenticationException("유효하지 않은 Refresh Token") {};
        }
    }

    public String refreshSocialAccessToken(OAuth2AuthenticationToken authentication) {
        String clientRegistrationId = authentication.getAuthorizedClientRegistrationId();
        String principalName = authentication.getName();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName);

        if (authorizedClient == null || authorizedClient.getRefreshToken() == null) {
            throw new IllegalArgumentException("No authorized client or refresh token available");
        }

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(authorizedClient.getClientRegistration().getClientId())
                .authorizationUri(authorizedClient.getClientRegistration().getProviderDetails().getAuthorizationUri())
                .redirectUri(authorizedClient.getClientRegistration().getRedirectUriTemplate())
                .scopes(authorizedClient.getClientRegistration().getScopes())
                .state("state")
                .build();

        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri(authorizedClient.getClientRegistration().getRedirectUriTemplate())
                .state("state")
                .build();

        OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);

        OAuth2AccessTokenResponse response = accessTokenResponseClient.getTokenResponse(
                new OAuth2AuthorizationCodeGrantRequest(
                        authorizedClient.getClientRegistration(),
                        authorizationExchange
                )
        );

        OAuth2AuthorizedClient updatedAuthorizedClient = new OAuth2AuthorizedClient(
                authorizedClient.getClientRegistration(),
                authorizedClient.getPrincipalName(),
                response.getAccessToken(),
                response.getRefreshToken()
        );

        authorizedClientService.saveAuthorizedClient(updatedAuthorizedClient, authentication);

        return response.getAccessToken().getTokenValue();
    }
}