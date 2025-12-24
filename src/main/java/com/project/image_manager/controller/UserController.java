package com.project.image_manager.controller;

import com.project.image_manager.entity.User;
import com.project.image_manager.repository.UserRepository;
import com.project.image_manager.util.JwtUtil;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.Optional;


/**
 * @author XRAY
 * @date 2025/12/24 20:30
 * @Version 1.0
 * @SpecialThanksTo : Unknown
 */


@RestController
@RequestMapping("/api/user")
@Data
public class UserController {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        // 1. 验证用户名唯一
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("用户名已存在");
        }
        // 2. 验证邮箱唯一
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("邮箱已存在");
        }
        // 3. 验证密码长度 ≥6
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body("密码至少6位");
        }
        // 4. 简单验证邮箱格式
        if (!user.getEmail().matches("^[^@]+@[^@]+\\.[^@]+$")) {
            return ResponseEntity.badRequest().body("邮箱格式不正确");
        }

        // 5. 密码加密保存
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("注册成功！");
    }

    // 登录接口
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("用户名不存在");
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("密码错误");
        }

        // 生成 JWT token
        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    // 登录请求体
    static class LoginRequest {
        private String username;
        private String password;
        // getter setter
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // 登录响应体
    static class LoginResponse {
        private String token;
        public LoginResponse(String token) { this.token = token; }
        public String getToken() { return token; }
    }
}