package com.project.image_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author XRAY
 * @date 2025/12/24 20:43
 * @Version 1.0
 * @SpecialThanksTo : Unknown
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // 关闭 CSRF
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()  // 临时允许所有请求（包括登录注册）
                )
                .httpBasic(httpBasic -> httpBasic.disable())  // 关闭 Basic 认证弹窗
                .formLogin(form -> form.disable());  // 关闭表单登录

        return http.build();
    }
}