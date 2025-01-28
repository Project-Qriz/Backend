package com.qriz.sqld.config;

import com.qriz.sqld.config.auth.RefreshTokenRepository;
import com.qriz.sqld.config.jwt.JwtAuthenticationFilter;
import com.qriz.sqld.config.jwt.JwtAuthorizationFilter;
import com.qriz.sqld.config.jwt.JwtVO;
import com.qriz.sqld.domain.user.UserEnum;
import com.qriz.sqld.util.CustomResponseUtil;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RefreshTokenRepository refreshTokenRepository;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        logger.debug("디버그 : BCryptPasswordEncoder 빈 등록됨");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DefaultAuthorizationCodeTokenResponseClient defaultAuthorizationCodeTokenResponseClient() {
        return new DefaultAuthorizationCodeTokenResponseClient();
    }

    // JWT 필터 등록
    public class CustomSecurityFilterManager extends AbstractHttpConfigurer<CustomSecurityFilterManager, HttpSecurity> {

        @Override
        public void configure(HttpSecurity builder) throws Exception {
            AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);

            // 필터 생성 시 refreshTokenRepository 주입
            JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager,
                    refreshTokenRepository);

            builder.addFilter(jwtAuthenticationFilter);
            builder.addFilter(new JwtAuthorizationFilter(authenticationManager, refreshTokenRepository));
            super.configure(builder);
        }

    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.debug("디버그 : filterChain 빈 등록됨");
        http.headers(headers -> headers.frameOptions().disable()); // iframe 사용 허용
        http.csrf(csrf -> csrf.disable());

        http.cors(cors -> cors.configurationSource(configurationSource()));

        // jSessionId를 서버쪽에서 관리안함
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.formLogin(login -> login.disable());
        http.httpBasic(basic -> basic.disable());

        // 필터 적용
        http.apply(new CustomSecurityFilterManager());

        // 인증 실패
        http.exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, authException) -> {
            CustomResponseUtil.fail(response, "로그인을 진행해 주세요", HttpStatus.UNAUTHORIZED);
        }));

        // 권한 실패
        http.exceptionHandling(handling -> handling.accessDeniedHandler((request, response, e) -> {
            CustomResponseUtil.fail(response, "권한이 없습니다", HttpStatus.FORBIDDEN);
        }));

        // URL 권한 설정
        http.authorizeRequests(requests -> requests
                .antMatchers("/api/v1/**").authenticated()
                .antMatchers("/api/admin/v1/**").hasRole("" + UserEnum.ADMIN)
                .antMatchers("/api/**").permitAll()
                .anyRequest().permitAll());

        // 로그아웃
        http.logout(logout -> logout
                .logoutUrl("/api/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    CustomResponseUtil.success(response, "로그아웃 성공");
                })
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID"));

        return http.build();
    }

    public CorsConfigurationSource configurationSource() {
        logger.debug("디버그 : configurationSource cors 설정이 SecurityFilterChain에 등록됨");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*"); // GET, POST, PUT, DELETE (Javascript 요청 허용)
        configuration.addAllowedOriginPattern("*"); // 모든 IP 주소 허용
        configuration.setAllowCredentials(true); // 클라이언트에서 쿠키 요청 허용

        // JWT 관련 헤더 노출 설정
        configuration.addExposedHeader(JwtVO.HEADER); // Authorization 헤더
        configuration.addExposedHeader(JwtVO.REFRESH_HEADER); // Refresh-Token 헤더

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}