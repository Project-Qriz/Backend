package com.qriz.sqld.oauth.info.impl;

import java.util.Map;

import com.qriz.sqld.oauth.info.OAuth2UserInfo;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
		super(attributes);
	}

    @Override
	public String getId() {
		return attributes.get("id").toString();
	}

	@Override
	public String getName() {
		Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

		if (properties == null) {
			return null;
		}

		return (String) properties.get("nickname");
	}

	@Override
	public String getEmail() {
		Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");

		return (String) account.get("email");
	}


    @Override
    public Long getTokenExpiration() {
        return 7200L; // 2시간 (7200초)
    }
}