package com.example.finmate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:8080", "http://127.0.0.1:3000", "http://127.0.0.1:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스 핸들링
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");

        registry.addResourceHandler("/css/**")
                .addResourceLocations("/resources/css/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("/resources/js/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("/resources/images/");

        // HTML 파일들을 직접 서빙
        registry.addResourceHandler("/*.html")
                .addResourceLocations("/")
                .setCachePeriod(0);

        // 루트 경로의 정적 파일들
        registry.addResourceHandler("/**")
                .addResourceLocations("/")
                .resourceChain(true);

        // Swagger UI 리소스 핸들링
        registry.addResourceHandler("/swagger-ui.html**")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .resourceChain(false);

        // Webjars 리소스 (CSS, JS 파일들)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(false);

        // 업로드 파일 핸들링
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:c:/upload/");

        // Vue.js 개발 환경 지원
        registry.addResourceHandler("/node_modules/**")
                .addResourceLocations("classpath:/static/node_modules/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 루트 경로를 index.html로 포워딩
        registry.addViewController("/").setViewName("forward:/index.html");

        // Vue.js 페이지 라우팅 추가
        registry.addViewController("/dashboard").setViewName("forward:/dashboard.html");
        registry.addViewController("/financial").setViewName("forward:/financial.html");
        registry.addViewController("/investment").setViewName("forward:/investment.html");
        registry.addViewController("/portfolio").setViewName("forward:/portfolio.html");
        registry.addViewController("/market").setViewName("forward:/market.html");
        registry.addViewController("/calculator").setViewName("forward:/calculator.html");
        registry.addViewController("/education").setViewName("forward:/education.html");
        registry.addViewController("/mypage").setViewName("forward:/mypage.html");

        // 회원 관련 페이지 라우팅
        registry.addViewController("/member").setViewName("forward:/member.html");
        registry.addViewController("/login").setViewName("forward:/member.html");
        registry.addViewController("/register").setViewName("forward:/member.html");

        // 관리자 페이지 라우팅
        registry.addViewController("/admin").setViewName("forward:/admindashboard.html");
        registry.addViewController("/admindashboard").setViewName("forward:/admindashboard.html");
    }
}