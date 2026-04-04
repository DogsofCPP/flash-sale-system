package com.example.seckillservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class LuaStockService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> decreaseScript;
    private DefaultRedisScript<Long> increaseScript;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_KEY_PREFIX = "seckill:user:";

    @PostConstruct
    public void init() {
        decreaseScript = new DefaultRedisScript<>();
        decreaseScript.setResultType(Long.class);
        decreaseScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/decrease_stock.lua")));

        increaseScript = new DefaultRedisScript<>();
        increaseScript.setResultType(Long.class);
        increaseScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/increase_stock.lua")));
    }

    /** 初始化秒杀库存到Redis */
    public void initStock(Long activityId, Long productId, int stock) {
        String key = stockKey(activityId, productId);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(stock));
    }

    /** 原子扣减库存（Lua） */
    public long decreaseStock(Long activityId, Long productId, int quantity, int limit) {
        String stockKey = stockKey(activityId, productId);
        String userKey = userKey(activityId, productId);
        List<String> keys = List.of(stockKey, userKey);
        Long result = stringRedisTemplate.execute(decreaseScript, keys,
                String.valueOf(quantity), String.valueOf(limit));
        return result != null ? result : -999;
    }

    /** 回滚库存 */
    public long increaseStock(Long activityId, Long productId, int quantity) {
        String key = stockKey(activityId, productId);
        Long result = stringRedisTemplate.execute(increaseScript, List.of(key), String.valueOf(quantity));
        return result != null ? result : -1;
    }

    /** 获取当前库存 */
    public Integer getStock(Long activityId, Long productId) {
        String val = stringRedisTemplate.opsForValue().get(stockKey(activityId, productId));
        return val != null ? Integer.parseInt(val) : null;
    }

    private String stockKey(Long activityId, Long productId) {
        return STOCK_KEY_PREFIX + activityId + ":" + productId;
    }

    private String userKey(Long activityId, Long productId) {
        return USER_KEY_PREFIX + activityId + ":" + productId;
    }
}
