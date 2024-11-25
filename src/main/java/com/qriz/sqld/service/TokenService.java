package com.qriz.sqld.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TokenService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

    // 소셜 로그인 토큰 갱신
    public String refreshSocialAccessToken(OAuth2AuthenticationToken authentication) {
        String clientRegistrationId = authentication.getAuthorizedClientRegistrationId();
        String principalName = authentication.getName();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                clientRegistrationId,
                principalName);

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

        OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(
                authorizationRequest,
                authorizationResponse);

        OAuth2AccessTokenResponse response = accessTokenResponseClient.getTokenResponse(
                new OAuth2AuthorizationCodeGrantRequest(
                        authorizedClient.getClientRegistration(),
                        authorizationExchange));

        OAuth2AuthorizedClient updatedAuthorizedClient = new OAuth2AuthorizedClient(
                authorizedClient.getClientRegistration(),
                authorizedClient.getPrincipalName(),
                response.getAccessToken(),
                response.getRefreshToken());

        authorizedClientService.saveAuthorizedClient(updatedAuthorizedClient, authentication);
        
        log.debug("Social token refreshed for client: {}", clientRegistrationId);
        return response.getAccessToken().getTokenValue();
    }
}