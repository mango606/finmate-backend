package com.example.finmate.config;

import com.example.finmate.security.service.CustomUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(customUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("Security 설정 적용 중...");

        http
                // CSRF 비활성화 (REST API용)
                .csrf(csrf -> csrf.disable())

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 관리 (JWT 사용)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())

                // Form Login 비활성화 (REST API 전용)
                .formLogin(form -> form.disable())

                // 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 허용
                        .antMatchers("/resources/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // HTML 파일들 허용
                        .antMatchers("/", "/index.html", "/member.html", "/*.html").permitAll()
                        // Swagger 허용
                        .antMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .antMatchers("/v2/api-docs", "/v3/api-docs/**").permitAll()
                        .antMatchers("/swagger-resources/**").permitAll()
                        .antMatchers("/configuration/ui", "/configuration/security").permitAll()
                        .antMatchers("/webjars/**").permitAll()
                        // 인증 불필요 API
                        .antMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        .antMatchers("/api/auth/refresh", "/api/auth/status").permitAll()
                        .antMatchers("/api/member/join", "/api/member/checkUserId/**", "/api/member/checkEmail").permitAll()
                        .antMatchers("/api/member/health", "/api/health/**").permitAll()
                        // 테스트 엔드포인트 허용
                        .antMatchers("/ping", "/hello").permitAll()
                        // 회원 관련 API - 인증 필요
                        .antMatchers("/api/member/**").hasRole("USER")
                        // 관리자 API
                        .antMatchers("/api/admin/**").hasRole("ADMIN")
                        // 나머지 모든 요청
                        .anyRequest().authenticated()
                )

                // 예외 처리
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("접근 거부: {}", request.getRequestURI());
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            try {
                                response.getWriter().write("{\"success\":false,\"message\":\"접근 권한이 없습니다\",\"errorCode\":\"ACCESS_DENIED\"}");
                            } catch (IOException e) {
                                log.error("응답 작성 실패", e);
                            }
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("인증 필요: {}", request.getRequestURI());
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            try {
                                response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다\",\"errorCode\":\"AUTHENTICATION_REQUIRED\"}");
                            } catch (IOException e) {
                                log.error("응답 작성 실패", e);
                            }
                        })
                );

        log.info("Security 설정 완료");
        return http.build();
    }
}