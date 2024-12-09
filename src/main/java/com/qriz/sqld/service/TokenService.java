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

        public String refreshSocialAccessToken(OAuth2AuthenticationToken authentication) {
                try {
                        String clientRegistrationId = authentication.getAuthorizedClientRegistrationId();
                        String principalName = authentication.getName();

                        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                                        clientRegistrationId,
                                        principalName);

                        if (authorizedClient == null || authorizedClient.getRefreshToken() == null) {
                                log.error("Failed to refresh token: No authorized client or refresh token available for {}",
                                                clientRegistrationId);
                                throw new IllegalArgumentException("No authorized client or refresh token available");
                        }

                        // Create new authorization exchange
                        OAuth2AuthorizationRequest authorizationRequest = createAuthorizationRequest(authorizedClient);
                        OAuth2AuthorizationResponse authorizationResponse = createAuthorizationResponse(
                                        authorizedClient);
                        OAuth2AuthorizationExchange authorizationExchange = new OAuth2AuthorizationExchange(
                                        authorizationRequest,
                                        authorizationResponse);

                        // Get new token response
                        OAuth2AccessTokenResponse response = accessTokenResponseClient.getTokenResponse(
                                        new OAuth2AuthorizationCodeGrantRequest(
                                                        authorizedClient.getClientRegistration(),
                                                        authorizationExchange));

                        // Save the new authorized client
                        saveNewAuthorizedClient(authorizedClient, response, authentication);

                        log.debug("Social token refreshed successfully for client: {}", clientRegistrationId);
                        return response.getAccessToken().getTokenValue();

                } catch (Exception e) {
                        log.error("Error refreshing social access token", e);
                        throw new RuntimeException("Failed to refresh social access token", e);
                }
        }

        private OAuth2AuthorizationRequest createAuthorizationRequest(OAuth2AuthorizedClient authorizedClient) {
                return OAuth2AuthorizationRequest.authorizationCode()
                                .clientId(authorizedClient.getClientRegistration().getClientId())
                                .authorizationUri(authorizedClient.getClientRegistration().getProviderDetails()
                                                .getAuthorizationUri())
                                .redirectUri(authorizedClient.getClientRegistration().getRedirectUriTemplate())
                                .scopes(authorizedClient.getClientRegistration().getScopes())
                                .state("state")
                                .build();
        }

        private OAuth2AuthorizationResponse createAuthorizationResponse(OAuth2AuthorizedClient authorizedClient) {
                return OAuth2AuthorizationResponse.success("code")
                                .redirectUri(authorizedClient.getClientRegistration().getRedirectUriTemplate())
                                .state("state")
                                .build();
        }

        private void saveNewAuthorizedClient(OAuth2AuthorizedClient authorizedClient,
                        OAuth2AccessTokenResponse response,
                        OAuth2AuthenticationToken authentication) {
                OAuth2AuthorizedClient updatedAuthorizedClient = new OAuth2AuthorizedClient(
                                authorizedClient.getClientRegistration(),
                                authorizedClient.getPrincipalName(),
                                response.getAccessToken(),
                                response.getRefreshToken());

                authorizedClientService.saveAuthorizedClient(updatedAuthorizedClient, authentication);
        }
}