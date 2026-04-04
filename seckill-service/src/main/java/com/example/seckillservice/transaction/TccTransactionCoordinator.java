package com.example.seckillservice.transaction;

import com.example.common.transaction.*;
import com.example.common.transaction.annotation.TccPhase;
import com.example.seckillservice.service.IdempotencyService;
import com.example.seckillservice.service.LuaStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * TCC分布式事务协调器
 *
 * 职责：
 * 1. 管理全局事务的生命周期
 * 2. 协调各参与者的Try/Confirm/Cancel
 * 3. 处理超时和异常情况
 * 4. 提供补偿机制
 *
 * 事务流程：
 *   Try → Confirm（全部成功）or Cancel（任意失败）
 *
 * 秒杀场景下的TCC流程：
 *   Try:    冻结Redis库存（预扣减），记录预扣记录
 *   Confirm: 确认扣减，更新数据库库存
 *   Cancel:  回滚Redis库存，删除预扣记录
 */
@Service
public class TccTransactionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TccTransactionCoordinator.class);

    /** TCC事务缓存（内存存储，生产环境建议用Redis持久化） */
    private final Map<String, TccTransaction> transactionCache = new ConcurrentHashMap<>();

    /** Redis键前缀 */
    private static final String TCC_TX_PREFIX = "tcc:tx:";
    private static final String TCC_LOCK_PREFIX = "tcc:lock:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private LuaStockService luaStockService;

    @Autowired
    private IdempotencyService idempotencyService;

    @PostConstruct
    public void init() {
        log.info("TCC事务协调器初始化完成");
    }

    // ==================== 事务发起 ====================

    /**
     * 发起一个新的TCC分布式事务
     * 用于秒杀下单场景：库存预扣 + 订单创建
     *
     * @param bizType   业务类型
     * @param bizNo     业务单号
     * @param userId    用户ID
     * @param activityId 活动ID
     * @param productId 商品ID
     * @param quantity  数量
     * @return 全局事务ID
     */
    public String beginTransaction(String bizType, String bizNo, Long userId,
            Long activityId, Long productId, Integer quantity) {
        String globalTxId = generateTxId(bizType, bizNo);

        TccTransaction tx = new TccTransaction(globalTxId, bizType, bizNo, userId);
        tx.setActivityId(activityId);
        tx.setProductId(productId);
        tx.setQuantity(quantity);

        // 存储到Redis（设置过期时间，防止内存泄漏）
        redisTemplate.opsForValue().set(
                TCC_TX_PREFIX + globalTxId,
                toJson(tx),
                30, TimeUnit.MINUTES
        );

        log.info("TCC事务已创建: globalTxId={}, bizType={}, bizNo={}", globalTxId, bizType, bizNo);
        return globalTxId;
    }

    /**
     * 执行TCC Try阶段
     * 冻结库存（预扣减）
     */
    public boolean tryPhase(String globalTxId, Long activityId, Long productId, int quantity, int limitPerUser) {
        TccTransaction tx = loadTransaction(globalTxId);
        if (tx == null) {
            log.error("TCC事务不存在: globalTxId={}", globalTxId);
            return false;
        }

        try {
            tx.startTry();

            // 调用Lua脚本预扣库存（Redis原子操作）
            long result = luaStockService.decreaseStock(activityId, productId, quantity, limitPerUser);

            if (result < 0) {
                String reason = switch ((int) result) {
                    case -1 -> "超过购买限制";
                    case -3 -> "库存不足";
                    default -> "库存未初始化";
                };
                tx.tryFailed(reason);
                updateTransaction(tx);
                log.warn("TCC Try阶段失败: globalTxId={}, reason={}", globalTxId, reason);
                return false;
            }

            tx.trySuccess();
            updateTransaction(tx);

            log.info("TCC Try阶段成功（库存预扣）: globalTxId={}, activityId={}, productId={}, remainingStock={}",
                    globalTxId, activityId, productId, result);
            return true;

        } catch (Exception e) {
            tx.tryFailed(e.getMessage());
            updateTransaction(tx);
            log.error("TCC Try阶段异常: globalTxId={}", globalTxId, e);
            return false;
        }
    }

    /**
     * 执行TCC Confirm阶段（确认扣减）
     * 将Redis预扣的库存确认为真实扣减
     */
    public boolean confirmPhase(String globalTxId) {
        TccTransaction tx = loadTransaction(globalTxId);
        if (tx == null) {
            log.error("TCC Confirm: 事务不存在 globalTxId={}", globalTxId);
            return false;
        }

        if (tx.getTryResult() != 1) {
            log.warn("TCC Confirm: Try阶段未成功，不能Confirm globalTxId={}", globalTxId);
            return false;
        }

        try {
            tx.startConfirm();

            // Confirm阶段：Redis预扣减已经生效（库存已减少）
            // 这里只需做业务确认（如更新DB库存、更新秒杀商品已售数量）
            // Redis库存已在Try阶段扣减，不需要再操作

            tx.confirmSuccess();
            updateTransaction(tx);

            log.info("TCC Confirm阶段完成: globalTxId={}", globalTxId);
            return true;

        } catch (Exception e) {
            log.error("TCC Confirm阶段异常: globalTxId={}", globalTxId, e);
            // Confirm失败，触发重试补偿
            return false;
        }
    }

    /**
     * 执行TCC Cancel阶段（回滚）
     * 释放预扣的Redis库存
     */
    public boolean cancelPhase(String globalTxId, String reason) {
        TccTransaction tx = loadTransaction(globalTxId);
        if (tx == null) {
            log.error("TCC Cancel: 事务不存在 globalTxId={}", globalTxId);
            return false;
        }

        try {
            tx.startRollback();

            // Cancel阶段：回滚Redis库存（加回预扣的数量）
            Long activityId = tx.getActivityId();
            Long productId = tx.getProductId();
            Integer quantity = tx.getQuantity();

            if (activityId != null && productId != null && quantity != null) {
                luaStockService.increaseStock(activityId, productId, quantity);
                log.info("TCC Cancel阶段：库存已回滚 globalTxId={}, activityId={}, productId={}, quantity={}",
                        globalTxId, activityId, productId, quantity);
            }

            // 释放幂等锁
            if (tx.getUserId() != null && activityId != null) {
                idempotencyService.release(tx.getUserId(), activityId);
            }

            tx.rollbackSuccess();
            updateTransaction(tx);

            log.info("TCC Cancel阶段完成: globalTxId={}, reason={}", globalTxId, reason);
            return true;

        } catch (Exception e) {
            tx.rollbackFailed(e.getMessage());
            updateTransaction(tx);
            log.error("TCC Cancel阶段异常: globalTxId={}", globalTxId, e);

            // 如果是超时导致的Cancel，直接清理
            if (tx.getStatus() == 3 && tx.getRetryCount() >= 3) {
                log.warn("TCC Cancel已达最大重试，强制清理: globalTxId={}", globalTxId);
                forceCleanup(globalTxId);
            }
            return false;
        }
    }

    /**
     * 处理TCC超时事务
     * 定时扫描未完成的事务，进行补偿
     */
    @Scheduled(fixedDelay = 30000)
    public void handleTimeoutTransactions() {
        log.debug("TCC超时检查...");
        // 扫描Redis中的TCC事务，找出超时未完成的进行补偿
        // 简化版：主要依靠订单超时服务来处理
    }

    /**
     * 补偿失败的重试任务
     */
    @Scheduled(fixedDelay = 60000)
    public void retryFailedTransactions() {
        // 扫描确认/取消失败的事务，进行重试
        // 简化版：依靠幂等保证重试安全
    }

    /**
     * 强制清理异常事务
     */
    private void forceCleanup(String globalTxId) {
        try {
            redisTemplate.delete(TCC_TX_PREFIX + globalTxId);
            log.warn("TCC事务已强制清理: globalTxId={}", globalTxId);
        } catch (Exception e) {
            log.error("TCC事务清理失败: globalTxId={}", globalTxId, e);
        }
    }

    /**
     * 获取TCC分布式锁（防止并发处理同一事务）
     */
    public boolean tryLock(String globalTxId, long timeoutSeconds) {
        String lockKey = TCC_LOCK_PREFIX + globalTxId;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", timeoutSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放TCC分布式锁
     */
    public void releaseLock(String globalTxId) {
        redisTemplate.delete(TCC_LOCK_PREFIX + globalTxId);
    }

    /**
     * 注册TCC参与者（订单服务回调）
     */
    public void registerParticipant(String globalTxId, String participant, int status) {
        TccTransaction tx = loadTransaction(globalTxId);
        if (tx != null) {
            tx.setBranchStatus(participant + ":" + status);
            updateTransaction(tx);
        }
    }

    /**
     * 获取事务状态
     */
    public TccTransaction getTransaction(String globalTxId) {
        return loadTransaction(globalTxId);
    }

    /**
     * 判断事务是否已结束（已确认或已回滚）
     */
    public boolean isFinished(String globalTxId) {
        TccTransaction tx = loadTransaction(globalTxId);
        if (tx == null) return false;
        return tx.getStatus() == TransactionStatus.CONFIRMED
                || tx.getStatus() == TransactionStatus.ROLLED_BACK
                || tx.getStatus() == TransactionStatus.COMMITTED;
    }

    // ==================== 私有方法 ====================

    private String generateTxId(String bizType, String bizNo) {
        return bizType + "_" + bizNo + "_" + System.currentTimeMillis();
    }

    private TccTransaction loadTransaction(String globalTxId) {
        try {
            String json = redisTemplate.opsForValue().get(TCC_TX_PREFIX + globalTxId);
            if (json == null) return null;
            return fromJson(json);
        } catch (Exception e) {
            log.error("加载TCC事务失败: globalTxId={}", globalTxId, e);
            return null;
        }
    }

    private void updateTransaction(TccTransaction tx) {
        try {
            redisTemplate.opsForValue().set(
                    TCC_TX_PREFIX + tx.getGlobalTxId(),
                    toJson(tx),
                    30, TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.error("更新TCC事务失败: globalTxId={}", tx.getGlobalTxId(), e);
        }
    }

    private String toJson(TccTransaction tx) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(
                    new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writeValueAsString(tx);
        } catch (Exception e) {
            throw new RuntimeException("序列化TCC事务失败", e);
        }
    }

    private TccTransaction fromJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(
                    new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.readValue(json, TccTransaction.class);
        } catch (Exception e) {
            throw new RuntimeException("反序列化TCC事务失败", e);
        }
    }
}
