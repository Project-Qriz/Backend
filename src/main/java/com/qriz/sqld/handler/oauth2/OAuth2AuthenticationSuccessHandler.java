package com.qriz.sqld.handler.oauth2;

import com.qriz.sqld.config.auth.LoginUser;
import com.qriz.sqld.config.jwt.JwtProcess;
import com.qriz.sqld.config.jwt.JwtVO;
import com.qriz.sqld.dto.user.UserRespDto;
import com.qriz.sqld.handler.ex.OAuth2AuthenticationProcessingException;
import com.qriz.sqld.domain.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.qriz.sqld.util.CookieUtils;
import javax.servlet.http.Cookie;

import com.qriz.sqld.util.CustomResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.info("Authentication Class Type: {}", authentication.getClass().getSimpleName());
        log.info("Principal Class Type: {}", authentication.getPrincipal().getClass().getSimpleName());

        log.info("OAuth2 authentication successful, processing token issuance...");

        // OAuth 로그인 시 jwt 토큰을 발급 받도록
        if (authentication.getPrincipal() instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            String token = JwtProcess.create(loginUser);
            log.info("JWT Token generated: " + "{" + token + "}");

            response.addHeader(JwtVO.HEADER, token);

            UserRespDto.LoginRespDto loginRespDto = new UserRespDto.LoginRespDto(loginUser.getUser());
            CustomResponseUtil.success(response, loginRespDto);

            log.info("JWT Token generated and added to the response header");
        } else {
            log.info("Authentication failed: Principal is not an instance of LoginUser");
        }

        String targetUrl;

        targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }
        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {

        Optional<String> redirectUri = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        String mode = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.MODE_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse("");

        LoginUser principal = getOAuth2UserPrincipal(authentication);

        if (principal == null) {
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error", "Login failed")
                    .build().toUriString();
        }

        if ("login".equalsIgnoreCase(mode)) {

            log.info("email={}, name={}, nickname={}, accessToken={}", principal.getUser().getEmail(),
                    principal.getUser().getName(),
                    principal.getUser().getNickname(),
                    principal.getUser().getAccessToken()
            );

            String accessToken = "test_access_token";
            String refreshToken = "test_refresh_token";

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("access_token", accessToken)
                    .queryParam("refresh_token", refreshToken)
                    .build().toUriString();

        }

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", "Login failed")
                .build().toUriString();
    }

    private LoginUser getOAuth2UserPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }
        return null;
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

}
