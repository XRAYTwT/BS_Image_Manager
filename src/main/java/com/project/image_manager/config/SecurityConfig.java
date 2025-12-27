package com.project.image_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/register", "/api/user/login").permitAll()
                        .requestMatchers("/", "/index.html", "/static/**", "/uploads/**", "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg").permitAll()
                        .anyRequest().permitAll()  // 开发阶段临时全开放，提交前可改 authenticated
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

}