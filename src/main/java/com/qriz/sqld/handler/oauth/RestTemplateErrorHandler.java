package com.qriz.sqld.handler.oauth;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.client.ResponseErrorHandler;

public class RestTemplateErrorHandler implements ResponseErrorHandler {
    
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }
    
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().is4xxClientError()) {
            throw new OAuth2AuthenticationException(null, 
                "OAuth provider returned client error: " + response.getStatusCode());
        } else if (response.getStatusCode().is5xxServerError()) {
            throw new OAuth2AuthenticationException(null,
                "OAuth provider returned server error: " + response.getStatusCode());
        }
    }
}