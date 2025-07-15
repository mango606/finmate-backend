package com.example.finmate.config;

import com.example.finmate.security.filter.JwtAuthenticationFilter;
import com.example.finmate.security.filter.JwtUsernamePasswordAuthenticationFilter;
import com.example.finmate.security.handler.CustomAccessDeniedHandler;
import com.example.finmate.security.handler.CustomAuthenticationEntryPoint;
import com.example.finmate.security.handler.LoginFailureHandler;
import com.example.finmate.security.handler.LoginSuccessHandler;
import com.example.finmate.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용)
                .csrf().disable()

                // CORS 설정
                .cors().configurationSource(corsConfigurationSource)

                .and()

                // 세션 관리 설정 (Stateless)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and()

                // 권한 설정
                .authorizeRequests()
                // 정적 리소스 허용
                .antMatchers("/resources/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Swagger 허용
                .antMatchers("/swagger-ui.html", "/swagger-ui/**", "/v2/api-docs", "/webjars/**").permitAll()
                // 인증 불필요 API
                .antMatchers("/api/auth/login", "/api/member/join").permitAll()
                .antMatchers("/api/member/checkUserId/**", "/api/member/checkEmail").permitAll()
                .antMatchers("/", "/index.html").permitAll()
                // 회원 관련 API - 인증 필요
                .antMatchers("/api/member/**").hasRole("USER")
                // 관리자 API
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                // 나머지 모든 요청 - 인증 필요
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()

                .and()

                // 예외 처리
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint)

                .and()

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(jwtUsernamePasswordAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter() throws Exception {
        return new JwtUsernamePasswordAuthenticationFilter(
                authenticationManagerBean(),
                loginSuccessHandler,
                loginFailureHandler
        );
    }
}