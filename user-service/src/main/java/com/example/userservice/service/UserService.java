package com.example.userservice.service;

import com.example.userservice.domain.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ConcurrentHashMap<Long, User> inMemoryUsers = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(100);

    public UserService() {
        // 初始化测试用户
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("user1");
        u1.setPasswordHash(passwordEncoder.encode("123456"));
        u1.setPhone("13800138000");
        u1.setStatus(1);
        inMemoryUsers.put(1L, u1);

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("admin");
        u2.setPasswordHash(passwordEncoder.encode("admin123"));
        u2.setPhone("13900139000");
        u2.setStatus(1);
        inMemoryUsers.put(2L, u2);
    }

    public User register(String username, String password, String phone) {
        // 检查用户名是否已存在
        for (User u : inMemoryUsers.values()) {
            if (u.getUsername().equals(username)) {
                throw new IllegalArgumentException("用户名已存在");
            }
        }

        User user = new User();
        user.setId(idCounter.incrementAndGet());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setStatus(1);
        inMemoryUsers.put(user.getId(), user);
        return user;
    }

    public User login(String username, String password) {
        User user = inMemoryUsers.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        return user;
    }

    public User getUserById(Long id) {
        return inMemoryUsers.get(id);
    }

    public User getUserByUsername(String username) {
        return inMemoryUsers.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    public List<User> listUsers() {
        return new ArrayList<>(inMemoryUsers.values());
    }
}
