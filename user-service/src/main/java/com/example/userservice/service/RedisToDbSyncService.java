package com.example.userservice.service;

import com.example.userservice.domain.SeckillActivity;
import com.example.userservice.domain.SeckillProduct;
import com.example.userservice.mapper.SeckillProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RedisToDbSyncService {

    private static final Logger log = LoggerFactory.getLogger(RedisToDbSyncService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SeckillProductMapper seckillProductMapper;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    @Scheduled(fixedDelay = 30000)
    public void syncStockToDatabase() {
        log.info("开始同步 Redis 库存到数据库...");

        Set<String> keys = redisTemplate.keys(STOCK_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        int syncCount = 0;
        for (String key : keys) {
            try {
                String[] parts = key.replace(STOCK_KEY_PREFIX, "").split(":");
                if (parts.length < 2) continue;

                Long activityId = Long.parseLong(parts[0]);
                Long productId = Long.parseLong(parts[1]);

                Object value = redisTemplate.opsForValue().get(key);
                if (value == null) continue;

                int redisStock = Integer.parseInt(value.toString());
                SeckillProduct sp = seckillProductMapper.findByActivityAndProduct(activityId, productId);

                if (sp != null && !sp.getStock().equals(redisStock)) {
                    int dbStock = sp.getStock();
                    int diff = redisStock - dbStock;

                    if (diff > 0) {
                        seckillProductMapper.increaseStock(activityId, productId, 0);
                        updateStockDirectly(activityId, productId, redisStock);
                    } else if (diff < 0) {
                        updateStockDirectly(activityId, productId, redisStock);
                    }

                    syncCount++;
                    log.info("同步库存: activityId={}, productId={}, redisStock={}, dbStock={}",
                            activityId, productId, redisStock, dbStock);
                }
            } catch (Exception e) {
                log.error("同步库存失败: key={}, error={}", key, e.getMessage());
            }
        }

        if (syncCount > 0) {
            log.info("库存同步完成，共同步 {} 条记录", syncCount);
        }
    }

    private void updateStockDirectly(Long activityId, Long productId, int stock) {
        seckillProductMapper.updateStock(activityId, productId, stock);
    }
}
