package com.qriz.sqld.oauth.info.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qriz.sqld.oauth.info.OAuth2UserInfo;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

	private Logger log = LoggerFactory.getLogger(KakaoOAuth2UserInfo.class);

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

		log.debug("Kakao properties: {}", properties); // 실제 데이터 확인

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
}