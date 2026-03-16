package com.example.userservice.service;

import com.example.userservice.domain.User;
import com.example.userservice.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final CacheService cacheService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, CacheService cacheService) {
        this.userMapper = userMapper;
        this.cacheService = cacheService;
    }

    @Transactional
    public User register(String username, String rawPassword, String phone) {
        User exist = userMapper.findByUsername(username);
        if (exist != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    public User login(String username, String rawPassword) {
        // 先从缓存读取用户信息
        User cachedUser = (User) cacheService.get("user:username:" + username);
        if (cachedUser != null) {
            if (passwordEncoder.matches(rawPassword, cachedUser.getPasswordHash())) {
                return cachedUser;
            }
            // 缓存密码错误，仍查库验证（避免缓存投毒）
        }

        // 缓存未命中，查数据库
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 缓存用户信息（5 分钟）
        cacheService.set("user:username:" + username, user, 300, java.util.concurrent.TimeUnit.SECONDS);
        return user;
    }

    /**
     * 根据 ID 查询用户（带缓存）
     */
    public User findById(Long id) {
        // 先查缓存
        User cached = (User) cacheService.get("user:id:" + id);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，查数据库
        User user = userMapper.findById(id);
        if (user != null) {
            // 写入缓存
            cacheService.set("user:id:" + id, user, 300, java.util.concurrent.TimeUnit.SECONDS);
        }
        return user;
    }
}

