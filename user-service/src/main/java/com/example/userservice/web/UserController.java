package com.example.userservice.web;

import com.example.common.Result;
import com.example.userservice.domain.User;
import com.example.userservice.service.UserService;
import com.example.userservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public UserController(UserService userService, JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestHeader(value = "X-User-Name", required = false) String username,
                                              @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (username == null) {
            return Result.fail(401, "未登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("role", role);
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request.getUsername(), request.getPassword(), request.getPhone());
            return Result.success("注册成功", user);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> data = userService.login(request.getUsername(), request.getPassword());
            return Result.success("登录成功", data);
        } catch (IllegalArgumentException e) {
            return Result.fail(401, e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.fail(400, "Refresh Token 不能为空");
        }

        // 检查 Redis 黑名单
        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + refreshToken))) {
            return Result.fail(401, "Token 已失效");
        }

        try {
            Claims claims = jwtUtil.parseToken(refreshToken);
            if (!"refresh".equals(claims.get("type"))) {
                return Result.fail(401, "无效的 Refresh Token");
            }

            String username = claims.getSubject();
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Result.fail(401, "用户不存在");
            }

            // 生成新的 Token 对
            String newAccessToken = jwtUtil.generateAccessToken(username, "USER");
            String newRefreshToken = jwtUtil.generateRefreshToken(username, "USER");

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("username", username);
            data.put("accessToken", newAccessToken);
            data.put("refreshToken", newRefreshToken);
            return Result.success("刷新成功", data);

        } catch (Exception e) {
            return Result.fail(401, "Token 已过期或无效");
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken != null) {
            try {
                Claims claims = jwtUtil.parseToken(refreshToken);
                long expirationTime = claims.getExpiration().getTime();
                long ttl = expirationTime - System.currentTimeMillis();

                if (ttl > 0) {
                    // 将 Refresh Token 加入黑名单
                    redisTemplate.opsForValue().set("blacklist:" + refreshToken, "true", ttl, TimeUnit.MILLISECONDS);
                }
            } catch (Exception ignored) {
            }
        }
        return Result.success("退出成功", null);
    }

    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        return Result.success(user);
    }

    // 请求体类
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String phone;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
