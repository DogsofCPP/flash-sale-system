package com.example.userservice.service;

import com.example.userservice.domain.User;
import com.example.userservice.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String USER_KEY_PREFIX = "user:";

    public User register(String username, String password, String phone) {
        User exist = userMapper.findByUsername(username);
        if (exist != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);

        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getId(), user, 1, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + "username:" + username, user, 1, TimeUnit.HOURS);
        return user;
    }

    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getId(), user, 1, TimeUnit.HOURS);
        return user;
    }

    public User getUserById(Long id) {
        User user = (User) redisTemplate.opsForValue().get(USER_KEY_PREFIX + id);
        if (user != null) return user;
        user = userMapper.findById(id);
        if (user != null) {
            redisTemplate.opsForValue().set(USER_KEY_PREFIX + id, user, 1, TimeUnit.HOURS);
        }
        return user;
    }

    public User getUserByUsername(String username) {
        User user = (User) redisTemplate.opsForValue().get(USER_KEY_PREFIX + "username:" + username);
        if (user != null) return user;
        user = userMapper.findByUsername(username);
        if (user != null) {
            redisTemplate.opsForValue().set(USER_KEY_PREFIX + "username:" + username, user, 1, TimeUnit.HOURS);
        }
        return user;
    }

    public List<User> listUsers() {
        return userMapper.findAll();
    }
}
