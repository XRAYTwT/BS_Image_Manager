package com.project.image_manager.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * @author XRAY
 * @date 2025/12/24 21:11
 * @Version 1.0
 * @SpecialThanksTo : Unknown
 */
@Component
public class JwtUtil {

    // 密钥（生产环境建议从配置读取）
    private final String SECRET_KEY = "MySuperSecretKey2025ZhejiangUniversityVeryLongAndSecureKey1234567890ABCDEF";
    private final long EXPIRATION_TIME = 86400000; // 1天

    // 生成 token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS512)
                .compact();
    }

    // 提取用户名
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    // 提取所有 claims
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 验证 token 是否有效
    public boolean validateToken(String token, String username) {
        String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // 检查 token 是否过期
    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}