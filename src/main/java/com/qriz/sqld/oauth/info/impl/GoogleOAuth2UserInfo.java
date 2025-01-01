package com.qriz.sqld.oauth.info.impl;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.qriz.sqld.oauth.info.OAuth2UserInfo;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        if (attributes == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_token", "User attributes are null", null)
            );
        }
        
        String email = (String) attributes.get("email");
        if (email == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_token", "Email not found in token", null)
            );
        }
        
        return email;
    }
}