package com.qriz.sqld.config.jwt;

import java.io.IOException;
import java.time.LocalDateTime;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.config.auth.RefreshToken;
import com.qriz.sqld.config.auth.RefreshTokenRepository;
import com.qriz.sqld.dto.user.UserReqDto;
import com.qriz.sqld.dto.user.UserRespDto;
import com.qriz.sqld.util.CustomResponseUtil;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager,
            RefreshTokenRepository refreshTokenRepository) {
        super(authenticationManager);
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        setFilterProcessesUrl("/api/login");
    }

    // Post : /api/v1/login
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        log.debug("디버그 : attemptAuthentication 호출됨");
        try {
            ObjectMapper om = new ObjectMapper();
            UserReqDto.LoginReqDto loginReqDto = om.readValue(request.getInputStream(), UserReqDto.LoginReqDto.class);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    loginReqDto.getUsername(), loginReqDto.getPassword());

            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            return authentication;
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException(e.getMessage());
        }
    }

    // 로그인 실패
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {
        CustomResponseUtil.fail(response, "로그인실패", HttpStatus.UNAUTHORIZED);
    }

    // return authentication 잘 작동하면 successfulAuthentication 메서드 호출
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authResult) throws IOException, ServletException {
        log.debug("디버그 : successfulAuthentication 호출됨");

        LoginUser loginUser = (LoginUser) authResult.getPrincipal();

        // Access Token 생성
        String accessToken = JwtProcess.createAccessToken(loginUser);

        // Refresh Token 생성
        String refreshToken = JwtProcess.createRefreshToken(loginUser);

        // Refresh Token DB 저장
        RefreshToken refreshTokenEntity = new RefreshToken(
                loginUser.getUser().getId(),
                refreshToken,
                LocalDateTime.now().plusSeconds(JwtVO.REFRESH_TOKEN_EXPIRATION_TIME));
        refreshTokenRepository.save(refreshTokenEntity);

        // Access Token만 응답 헤더에 추가
        response.addHeader(JwtVO.HEADER, accessToken);

        UserRespDto.LoginRespDto loginRespDto = new UserRespDto.LoginRespDto(loginUser.getUser());
        CustomResponseUtil.success(response, loginRespDto);
    }
}
