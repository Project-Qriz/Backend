package com.qriz.sqld.oauth.dto;

import com.qriz.sqld.domain.user.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OAuth2LoginResult {
    private final String accessToken;
    private final String refreshToken;
    private final User user;
}
