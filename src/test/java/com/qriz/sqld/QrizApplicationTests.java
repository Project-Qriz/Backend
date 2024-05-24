package com.qriz.sqld;

import com.qriz.sqld.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QrizApplicationTests {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
    }

    @Test
    void testLogout() {
        // 초기 설정: Redis에 테스트 데이터 추가
        String username = "testuser";
        String accessTokenKey = "AT:" + username;
        String refreshTokenKey = "RT:" + username;
        String accessTokenValue = "testAccessToken";
        String refreshTokenValue = "testRefreshToken";
        redisUtil.setData(accessTokenKey, accessTokenValue);
        redisUtil.setData(refreshTokenKey, refreshTokenValue);

        // 로그아웃 요청 보내기
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/api/logout"; // 기본 포트는 8080, 필요시 8081로 변경
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        // JWT 토큰을 헤더에 추가 (테스트 토큰 사용)
        String jwtToken = "Bearer testJwtToken";
        headers.set("Authorization", jwtToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        // 응답 확인
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).contains("로그아웃 성공");

        // Redis에서 키가 삭제되었는지 확인
        assertThat(redisUtil.getData(accessTokenKey)).isNull();
        assertThat(redisUtil.getData(refreshTokenKey)).isNull();
    }
}
