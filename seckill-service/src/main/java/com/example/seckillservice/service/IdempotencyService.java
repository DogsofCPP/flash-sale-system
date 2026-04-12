package com.example.seckillservice.service;

import com.example.seckillservice.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * Redis 幂等性检查服务
 *
 * 使用 Redis SETNX 实现分布式幂等锁
 * 防止同一用户对同一活动重复提交秒杀请求
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private static final String DEDUP_KEY_PREFIX = "seckill:dedup:";
    private static final int DEDUP_TTL_SECONDS = 300; // 5分钟内不允许重复提交

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取幂等锁
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return true=可以继续处理（首次请求），false=重复请求
     */
    public boolean tryAcquire(Long userId, Long activityId) {
        String key = dedupKey(userId, activityId);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", DEDUP_TTL_SECONDS, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(success)) {
            log.debug("幂等锁获取成功: userId={}, activityId={}", userId, activityId);
            return true;
        }
        log.warn("幂等检查拦截重复请求: userId={}, activityId={}", userId, activityId);
        return false;
    }

    /**
     * 释放幂等锁（用于回滚场景）
     */
    public void release(Long userId, Long activityId) {
        String key = dedupKey(userId, activityId);
        redisTemplate.delete(key);
        log.debug("幂等锁释放: userId={}, activityId={}", userId, activityId);
    }

    /**
     * 检查是否已处理过
     */
    public boolean isProcessed(Long userId, Long activityId) {
        String key = dedupKey(userId, activityId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String dedupKey(Long userId, Long activityId) {
        return DEDUP_KEY_PREFIX + activityId + ":" + userId;
    }
}
