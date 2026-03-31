package com.example.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisStockService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_KEY_PREFIX = "seckill:user:";
    private static final String ORDER_KEY_PREFIX = "seckill:order:";

    public void initStock(Long activityId, Long productId, Integer stock) {
        String key = getStockKey(activityId, productId);
        redisTemplate.opsForValue().set(key, stock, 24L, TimeUnit.HOURS);
    }

    public Integer getStock(Long activityId, Long productId) {
        String key = getStockKey(activityId, productId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value.toString()) : null;
    }

    public boolean decreaseStock(Long activityId, Long productId) {
        String key = getStockKey(activityId, productId);
        Long result = redisTemplate.opsForValue().decrement(key);
        return result != null && result >= 0;
    }

    public void increaseStock(Long activityId, Long productId, Integer count) {
        String key = getStockKey(activityId, productId);
        redisTemplate.opsForValue().increment(key, count);
    }

    public boolean isUserLimitExceeded(Long activityId, Long productId, Long userId, Integer limitPerUser) {
        String key = getUserKey(activityId, productId, userId);
        Object value = redisTemplate.opsForValue().get(key);
        int used = value != null ? Integer.parseInt(value.toString()) : 0;
        return used >= limitPerUser;
    }

    public void addUserPurchaseCount(Long activityId, Long productId, Long userId) {
        String key = getUserKey(activityId, productId, userId);
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, 24L, TimeUnit.HOURS);
    }

    public boolean tryCreateOrder(Long activityId, Long productId, Long userId) {
        String key = getOrderKey(activityId, productId, userId);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", 24L, TimeUnit.HOURS);
        return Boolean.TRUE.equals(result);
    }

    public void deleteOrderKey(Long activityId, Long productId, Long userId) {
        String key = getOrderKey(activityId, productId, userId);
        redisTemplate.delete(key);
    }

    public void syncStockToRedis(Integer stock, Long activityId, Long productId) {
        String key = getStockKey(activityId, productId);
        redisTemplate.opsForValue().set(key, stock, 24L, TimeUnit.HOURS);
    }

    private String getStockKey(Long activityId, Long productId) {
        return STOCK_KEY_PREFIX + activityId + ":" + productId;
    }

    private String getUserKey(Long activityId, Long productId, Long userId) {
        return USER_KEY_PREFIX + activityId + ":" + productId + ":" + userId;
    }

    private String getOrderKey(Long activityId, Long productId, Long userId) {
        return ORDER_KEY_PREFIX + activityId + ":" + productId + ":" + userId;
    }
}
