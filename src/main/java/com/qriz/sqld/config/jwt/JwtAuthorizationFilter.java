package com.qriz.sqld.config.jwt;

import com.qriz.sqld.config.auth.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * 모든 주소에서 동작함 (토큰 검증)
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (isHeaderVerify(request, response)) {
            String accessToken = request.getHeader(JwtVO.HEADER).replace(JwtVO.TOKEN_PREFIX, "");
            String refreshToken = request.getHeader(JwtVO.REFRESH_HEADER);

            try {
                // Access Token 검증
                if (JwtProcess.isTokenValid(accessToken)) {
                    // Access Token이 유효한 경우
                    LoginUser loginUser = JwtProcess.verify(accessToken);

                    // 만료가 임박한 경우 갱신
                    if (JwtProcess.isTokenExpiringNear(accessToken)) {
                        String newToken = JwtProcess.extendAccessToken(loginUser);
                        response.setHeader(JwtVO.HEADER, newToken);
                        log.debug("디버그 : Access Token 자동 갱신 완료");
                    }

                    authenticateUser(loginUser);
                } else {
                    // Access Token이 만료된 경우, Refresh Token으로 갱신 시도
                    if (refreshToken != null && refreshToken.startsWith(JwtVO.TOKEN_PREFIX)) {
                        refreshToken = refreshToken.replace(JwtVO.TOKEN_PREFIX, "");

                        if (JwtProcess.isTokenValid(refreshToken)) {
                            LoginUser loginUser = JwtProcess.verify(refreshToken);
                            String newAccessToken = JwtProcess.createAccessToken(loginUser);
                            response.setHeader(JwtVO.HEADER, newAccessToken);

                            // Refresh Token도 만료가 임박한 경우 같이 갱신
                            if (JwtProcess.isTokenExpiringNear(refreshToken)) {
                                String newRefreshToken = JwtProcess.createRefreshToken(loginUser);
                                response.setHeader(JwtVO.REFRESH_HEADER, newRefreshToken);
                                log.debug("디버그 : Access Token과 Refresh Token 모두 갱신 완료");
                            } else {
                                log.debug("디버그 : Access Token 갱신 완료");
                            }

                            authenticateUser(loginUser);
                        } else {
                            log.error("디버그 : Refresh Token이 만료됨");
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token expired");
                            return;
                        }
                    } else {
                        log.error("디버그 : 유효한 Refresh Token이 없음");
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No valid refresh token");
                        return;
                    }
                }

            } catch (Exception e) {
                log.error("디버그 : 토큰 검증 실패", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void authenticateUser(LoginUser loginUser) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginUser,
                null,
                loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("디버그 : 토큰 검증 완료 및 임시 세션 생성됨");
    }

    private boolean isHeaderVerify(HttpServletRequest request, HttpServletResponse response) {
        String header = request.getHeader(JwtVO.HEADER);
        return header != null && header.startsWith(JwtVO.TOKEN_PREFIX);
    }
}