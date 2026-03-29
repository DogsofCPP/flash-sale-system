package com.example.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class LuaStockService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private DefaultRedisScript<Long> decreaseStockScript;
    private DefaultRedisScript<Long> increaseStockScript;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_KEY_PREFIX = "seckill:user:";
    private static final String ORDER_KEY_PREFIX = "seckill:order:";

    @PostConstruct
    public void init() {
        decreaseStockScript = new DefaultRedisScript<>();
        decreaseStockScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/decrease_stock.lua")));
        decreaseStockScript.setResultType(Long.class);

        increaseStockScript = new DefaultRedisScript<>();
        increaseStockScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/increase_stock.lua")));
        increaseStockScript.setResultType(Long.class);
    }

    public void initStock(Long activityId, Long productId, Integer stock) {
        String key = getStockKey(activityId, productId);
        redisTemplate.opsForValue().set(key, stock, 24L, TimeUnit.HOURS);
    }

    public Integer getStock(Long activityId, Long productId) {
        String key = getStockKey(activityId, productId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value.toString()) : null;
    }

    /**
     * Lua 脚本原子扣减库存
     * @return 剩余库存，-1 表示库存不足
     */
    public long decreaseStock(Long activityId, Long productId, int quantity) {
        String key = getStockKey(activityId, productId);
        Long result = redisTemplate.execute(decreaseStockScript,
                Collections.singletonList(key), quantity);
        return result != null ? result : -1;
    }

    /**
     * Lua 脚本原子增加库存（用于回滚）
     * @return 增加后的库存
     */
    public long increaseStock(Long activityId, Long productId, int quantity) {
        String key = getStockKey(activityId, productId);
        Long result = redisTemplate.execute(increaseStockScript,
                Collections.singletonList(key), quantity);
        return result != null ? result : 0;
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
