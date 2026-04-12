package com.example.userservice.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀库存服务
 * 使用Redis Lua脚本实现原子扣减，防止超卖
 */
@Service
public class SeckillStockService {

    private static final Logger log = LoggerFactory.getLogger(SeckillStockService.class);

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String DEDUP_KEY_PREFIX = "seckill:dup:";
    private static final int DEFAULT_EXPIRE_SECONDS = 3600; // 默认1小时

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> stockDeductScript;

    @PostConstruct
    public void init() {
        stockDeductScript = new DefaultRedisScript<>();
        stockDeductScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/stock_deduct.lua")));
        stockDeductScript.setResultType(Long.class);
    }

    /**
     * 预热库存到Redis
     *
     * @param productId 商品ID
     * @param stock    库存数量
     * @param expireSeconds 过期时间（秒）
     */
    public void warmupStock(Long productId, Long stock, Integer expireSeconds) {
        if (productId == null || stock == null || stock < 0) {
            throw new IllegalArgumentException("无效的商品ID或库存");
        }

        String stockKey = STOCK_KEY_PREFIX + productId;
        int expire = expireSeconds != null ? expireSeconds : DEFAULT_EXPIRE_SECONDS;

        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock), expire, TimeUnit.SECONDS);
        log.info("库存预热成功: productId={}, stock={}, expire={}s", productId, stock, expire);
    }

    /**
     * 原子扣减库存（使用Lua脚本保证原子性）
     *
     * @param userId     用户ID
     * @param productId  商品ID
     * @param activityId 活动ID
     * @param quantity   购买数量
     * @return >=0: 扣减成功，返回剩余库存; -1: 库存不足; -2: 用户已参与; -3: 库存未初始化
     */
    public Long deductStock(Long userId, Long productId, Long activityId, Integer quantity) {
        if (userId == null || productId == null || activityId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("参数无效");
        }

        String stockKey = STOCK_KEY_PREFIX + productId;
        String dupKey = DEDUP_KEY_PREFIX + userId + ":" + productId + ":" + activityId;

        try {
            Long result = stringRedisTemplate.execute(
                    stockDeductScript,
                    Arrays.asList(stockKey, dupKey),
                    String.valueOf(quantity),
                    String.valueOf(DEFAULT_EXPIRE_SECONDS)
            );
            log.info("库存扣减: userId={}, productId={}, quantity={}, result={}", userId, productId, quantity, result);
            return result != null ? result : -3L;
        } catch (Exception e) {
            log.error("库存扣减异常: userId={}, productId={}", userId, productId, e);
            return -3L;
        }
    }

    /**
     * 回滚库存
     *
     * @param productId 商品ID
     * @param quantity  回滚数量
     */
    public void rollbackStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("参数无效");
        }

        String stockKey = STOCK_KEY_PREFIX + productId;

        try {
            stringRedisTemplate.opsForValue().increment(stockKey, quantity.longValue());
            log.info("库存回滚: productId={}, quantity={}", productId, quantity);
        } catch (Exception e) {
            log.error("库存回滚异常: productId={}, quantity={}", productId, quantity, e);
        }
    }

    /**
     * 获取当前库存
     *
     * @param productId 商品ID
     * @return 剩余库存，未初始化返回null
     */
    public Long getStock(Long productId) {
        if (productId == null) {
            return null;
        }

        String stockKey = STOCK_KEY_PREFIX + productId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);

        if (stockStr == null) {
            return null;
        }

        try {
            return Long.parseLong(stockStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 清除用户去重标记
     *
     * @param userId     用户ID
     * @param productId  商品ID
     * @param activityId 活动ID
     */
    public void clearDedupKey(Long userId, Long productId, Long activityId) {
        if (userId == null || productId == null || activityId == null) {
            return;
        }

        String dupKey = DEDUP_KEY_PREFIX + userId + ":" + productId + ":" + activityId;
        stringRedisTemplate.delete(dupKey);
        log.info("清除去重标记: userId={}, productId={}, activityId={}", userId, productId, activityId);
    }
}
