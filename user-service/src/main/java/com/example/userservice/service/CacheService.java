package com.example.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_PREFIX = "user:cache:";
    private static final long DEFAULT_TTL_SECONDS = 300; // 默认缓存 5 分钟

    /**
     * 缓存用户信息
     */
    public void cacheUser(Long userId, Object userData) {
        String key = USER_CACHE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, userData, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的用户信息
     */
    public Object getCachedUser(Long userId) {
        String key = USER_CACHE_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存的用户信息
     */
    public void deleteCachedUser(Long userId) {
        String key = USER_CACHE_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * 缓存任意数据（带 TTL）
     */
    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存数据
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 分布式锁（简单实现，用于防缓存击穿）
     */
    public boolean tryLock(String lockKey, String lockValue, long timeout, TimeUnit unit) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String lockKey, String lockValue) {
        String currentValue = (String) redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }
}
