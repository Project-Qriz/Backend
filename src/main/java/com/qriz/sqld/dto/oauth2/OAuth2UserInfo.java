package com.qriz.sqld.dto.oauth2;

import java.util.Map;

import javax.security.auth.message.AuthException;

import com.qriz.sqld.domain.user.User;
import com.qriz.sqld.domain.user.UserEnum;
import com.qriz.sqld.handler.ex.CustomApiException;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OAuth2UserInfo {
    private String name;
    private String email;
    private String providerId;

    // registrationId에 따라 userInfo 생성
    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        switch (registrationId) { 
            case "google":
                return ofGoogle(attributes);
            case "kakao":
                return ofKakao(attributes);
            default:
                throw new CustomApiException("Illegal Registration ID");
        }
    }

    // Google 유저 정보 생성
    private static OAuth2UserInfo ofGoogle(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .providerId((String) attributes.get("sub"))
                .build();
    }

    // Kakao 유저 정보 생성
    private static OAuth2UserInfo ofKakao(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");

        return OAuth2UserInfo.builder()
                .name((String) profile.get("nickname"))
                .email((String) account.get("email"))
                .providerId(String.valueOf(attributes.get("id")))
                .build();
    }

    // User 엔티티로 변환
    public User toEntity() {
        return User.builder()
                .nickname(name)
                .email(email)
                .role(UserEnum.CUSTOMER)
                .build();
    }
}