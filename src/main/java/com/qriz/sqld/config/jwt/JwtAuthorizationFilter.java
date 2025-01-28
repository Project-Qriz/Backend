package com.qriz.sqld.config.jwt;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.config.auth.RefreshToken;
import com.qriz.sqld.config.auth.RefreshTokenRepository;

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
import java.time.LocalDateTime;
import java.util.Optional;

/*
 * 모든 주소에서 동작함 (토큰 검증)
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RefreshTokenRepository refreshTokenRepository;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager,
            RefreshTokenRepository refreshTokenRepository) {
        super(authenticationManager);
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (isHeaderVerify(request, response)) {
            String accessToken = request.getHeader(JwtVO.HEADER).replace(JwtVO.TOKEN_PREFIX, "");

            try {
                // Access Token 검증
                if (JwtProcess.isTokenValid(accessToken)) {
                    LoginUser loginUser = JwtProcess.verify(accessToken);
                    authenticateUser(loginUser);
                } else {
                    // Access Token이 만료된 경우
                    LoginUser loginUser = JwtProcess.verifyAndExtractUser(accessToken); // 수정된 메서드 사용
                    Long userId = loginUser.getUser().getId();

                    // RefreshToken을 DB에서 조회
                    Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findById(userId);

                    if (refreshTokenOptional.isPresent()) {
                        RefreshToken refreshTokenEntity = refreshTokenOptional.get();
                        String refreshToken = refreshTokenEntity.getToken();

                        if (JwtProcess.isTokenValid(refreshToken)) {
                            // 새로운 Access Token 발급
                            String newAccessToken = JwtProcess.createAccessToken(loginUser);
                            response.setHeader(JwtVO.HEADER, JwtVO.TOKEN_PREFIX + newAccessToken);

                            // Refresh Token 만료 3일 이내 체크
                            if (JwtProcess.isTokenExpiringNear(refreshToken, 60 * 24 * 3)) { // 3일
                                // 새로운 Refresh Token 발급
                                String newRefreshToken = JwtProcess.createRefreshToken(loginUser);

                                // DB의 Refresh Token 업데이트
                                refreshTokenEntity.updateToken(
                                        newRefreshToken,
                                        LocalDateTime.now().plusSeconds(JwtVO.REFRESH_TOKEN_EXPIRATION_TIME));
                                refreshTokenRepository.save(refreshTokenEntity);

                                log.debug("Refresh Token 자동 갱신 완료");
                            }

                            authenticateUser(loginUser);
                        } else {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token expired");
                            return;
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token not found");
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("토큰 검증 실패", e);
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