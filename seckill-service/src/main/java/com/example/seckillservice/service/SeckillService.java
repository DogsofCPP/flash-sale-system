package com.example.seckillservice.service;

import com.example.common.OrderMessage;
import com.example.common.SeckillRequest;
import com.example.common.SeckillResponse;
import com.example.common.id.SnowflakeIdGenerator;
import com.example.seckillservice.config.KafkaConfig;
import com.example.seckillservice.domain.*;
import com.example.seckillservice.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    /** 雪花算法生成器（机器ID=1，可配置） */
    private static final SnowflakeIdGenerator ID_GENERATOR = new SnowflakeIdGenerator(1);

    @Autowired
    private SeckillActivityMapper activityMapper;

    @Autowired
    private SeckillProductMapper productMapper;

    @Autowired
    private SeckillOrderMapper orderMapper;

    @Autowired
    private LuaStockService luaStockService;

    @Autowired
    private KafkaOrderProducer kafkaOrderProducer;

    @Autowired
    private IdempotencyService idempotencyService;

    // ======== 秒杀活动 ========

    public List<SeckillActivity> getAllActivities() {
        return activityMapper.findAll();
    }

    public List<SeckillActivity> getActiveActivities() {
        return activityMapper.findActiveActivities();
    }

    public SeckillActivity getActivity(Long id) {
        return activityMapper.findById(id);
    }

    public List<SeckillProduct> getActivityProducts(Long activityId) {
        return productMapper.findByActivityId(activityId);
    }

    /** 初始化单个活动库存到Redis */
    public void initSeckillStock(Long activityId, Long productId) {
        SeckillProduct sp = productMapper.findByActivityAndProduct(activityId, productId);
        if (sp != null) {
            luaStockService.initStock(activityId, productId, sp.getStock());
            log.info("初始化秒杀库存: activityId={}, productId={}, stock={}",
                    activityId, productId, sp.getStock());
        }
    }

    /** 初始化所有活动库存 */
    public void initAllSeckillStock() {
        List<SeckillActivity> activities = activityMapper.findActiveActivities();
        for (SeckillActivity activity : activities) {
            List<SeckillProduct> products = productMapper.findByActivityId(activity.getId());
            for (SeckillProduct product : products) {
                initSeckillStock(activity.getId(), product.getProductId());
            }
        }
    }

    // ======== 秒杀下单 ========

    public SeckillResponse seckill(Long userId, SeckillRequest request) {
        Long activityId = request.getActivityId();
        Long productId = request.getProductId();
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;

        // 1. Redis 幂等性检查（防止重复提交）
        if (!idempotencyService.tryAcquire(userId, activityId)) {
            throw new IllegalArgumentException("请求太频繁，请稍后重试");
        }

        try {
            // 2. 验证活动
            SeckillActivity activity = activityMapper.findById(activityId);
            if (activity == null) {
                throw new IllegalArgumentException("活动不存在");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(activity.getStartTime())) {
                throw new IllegalArgumentException("活动尚未开始");
            }
            if (now.isAfter(activity.getEndTime())) {
                throw new IllegalArgumentException("活动已结束");
            }

            // 3. 检查商品
            SeckillProduct seckillProduct = productMapper.findByActivityAndProduct(activityId, productId);
            if (seckillProduct == null) {
                throw new IllegalArgumentException("秒杀商品不存在");
            }

            // 4. Lua原子扣库存（含用户购买限制检查）
            long result = luaStockService.decreaseStock(activityId, productId, quantity,
                    seckillProduct.getLimitPerUser());

            if (result < 0) {
                if (result == -1) throw new IllegalArgumentException("您已超过购买限制");
                if (result == -3) throw new IllegalArgumentException("商品已抢光");
                throw new IllegalArgumentException("库存未初始化，请联系管理员");
            }

            // 5. 雪花算法生成订单号（嵌入活动基因+用户基因）
            String orderNo = ID_GENERATOR.nextSeckillOrderNo(userId, activityId);

            // 6. 创建秒杀订单（排队中）
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setUserId(userId);
            seckillOrder.setActivityId(activityId);
            seckillOrder.setProductId(productId);
            seckillOrder.setOrderNo(orderNo);
            seckillOrder.setStatus(0); // 排队中
            orderMapper.insert(seckillOrder);

            // 7. Kafka异步发送订单消息到order-service（削峰填谷）
            try {
                OrderMessage msg = new OrderMessage(
                        userId, productId, activityId,
                        "秒杀商品[" + productId + "]",
                        seckillProduct.getSeckillPrice(),
                        quantity,
                        seckillProduct.getSeckillPrice().multiply(java.math.BigDecimal.valueOf(quantity)),
                        orderNo
                );
                kafkaOrderProducer.sendOrderMessage(msg);

                // 8. 更新秒杀订单为成功
                orderMapper.updateStatusToSuccess(seckillOrder.getId(), 1, orderNo);

                log.info("秒杀成功: userId={}, orderNo={}, remainingStock={}",
                        userId, orderNo, result);

                return SeckillResponse.ok(orderNo, "秒杀商品[" + productId + "]",
                        quantity,
                        seckillProduct.getSeckillPrice().multiply(java.math.BigDecimal.valueOf(quantity)),
                        LocalDateTime.now().plusMinutes(15));

            } catch (Exception e) {
                // MQ发送失败，回滚库存
                log.error("创建订单失败，回滚库存: userId={}, productId={}", userId, productId, e);
                luaStockService.increaseStock(activityId, productId, quantity);
                orderMapper.updateStatusToFailed(seckillOrder.getId(), e.getMessage());
                // 释放幂等锁，允许重试
                idempotencyService.release(userId, activityId);
                throw new IllegalArgumentException("订单创建失败: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            // 业务异常，释放幂等锁
            idempotencyService.release(userId, activityId);
            throw e;
        } catch (Exception e) {
            // 其他异常，释放幂等锁
            idempotencyService.release(userId, activityId);
            log.error("秒杀异常: userId={}, activityId={}", userId, activityId, e);
            throw new RuntimeException("秒杀服务异常，请稍后重试", e);
        }
    }

    public List<SeckillOrder> getUserOrders(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    // ======== 定时任务：更新活动状态 ========

    @Scheduled(fixedDelay = 60000)
    public void updateActivityStatus() {
        List<SeckillActivity> activities = activityMapper.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (SeckillActivity activity : activities) {
            if (activity.getStatus() == 0 && now.isAfter(activity.getStartTime())) {
                activityMapper.updateStatus(activity.getId(), 1); // 未开始 → 进行中
                log.info("活动开始: id={}", activity.getId());
            } else if (activity.getStatus() == 1 && now.isAfter(activity.getEndTime())) {
                activityMapper.updateStatus(activity.getId(), 2); // 进行中 → 已结束
                log.info("活动结束: id={}", activity.getId());
            }
        }
    }
}
