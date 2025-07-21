package com.example.finmate.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "com.example.finmate.security",
        "com.example.finmate.auth",
        "com.example.finmate.member",
        "com.example.finmate.financial",
        "com.example.finmate.admin",
        "com.example.finmate.common",
        "com.example.finmate.controller"
})
public class ApplicationContextConfig {
    // 컴포넌트 스캔을 위한 설정 클래스
}