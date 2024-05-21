package com.qriz.sqld.oauth.info;

import java.util.Map;

import com.qriz.sqld.oauth.info.impl.GoogleOAuth2UserInfo;
import com.qriz.sqld.oauth.info.impl.KakaoOAuth2UserInfo;
import com.qriz.sqld.oauth.provider.Provider;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(Provider provider, Map<String, Object> attributes) {
        switch (provider) {
            case GOOGLE:
                return new GoogleOAuth2UserInfo(attributes);
            case KAKAO:
                return new KakaoOAuth2UserInfo(attributes);
            default:
                throw new IllegalArgumentException("Invalid Provider Type: " + provider);
        }
    }
}