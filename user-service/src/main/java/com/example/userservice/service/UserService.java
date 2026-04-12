package com.example.userservice.service;

import com.example.userservice.domain.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.util.JwtUtil;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initAdmin() {
        try {
            Optional<User> existingAdmin = userRepository.findByUsername("admin");
            if (existingAdmin.isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setPhone("13900000000");
                admin.setStatus(1);
                userRepository.save(admin);
                System.out.println("Default admin created: admin / admin123");
            }
        } catch (Exception e) {
            System.out.println("Admin check skipped: " + e.getMessage());
        }

        try {
            Optional<User> existingUser1 = userRepository.findByUsername("user1");
            if (existingUser1.isEmpty()) {
                User user1 = new User();
                user1.setUsername("user1");
                user1.setPasswordHash(passwordEncoder.encode("123456"));
                user1.setPhone("13800138000");
                user1.setStatus(1);
                userRepository.save(user1);
                System.out.println("Default user created: user1 / 123456");
            }
        } catch (Exception e) {
            System.out.println("User1 check skipped: " + e.getMessage());
        }
    }

    @Transactional
    public User register(String username, String password, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setStatus(1);
        return userRepository.save(user);
    }

    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        // 生成双 Token
        String accessToken = jwtUtil.generateAccessToken(username, "USER");
        String refreshToken = jwtUtil.generateRefreshToken(username, "USER");

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        return result;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        // 验证 Refresh Token
        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token 已过期");
        }

        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalArgumentException("无效的 Refresh Token");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 生成新的 Token 对
        String newAccessToken = jwtUtil.generateAccessToken(username, "USER");
        String newRefreshToken = jwtUtil.generateRefreshToken(username, "USER");

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        return result;
    }
}
