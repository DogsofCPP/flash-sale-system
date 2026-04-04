package com.example.seckillservice.transaction;

import com.example.common.OrderMessage;
import com.example.common.SeckillRequest;
import com.example.common.SeckillResponse;
import com.example.common.id.SnowflakeIdGenerator;
import com.example.common.transaction.TccTransaction;
import com.example.common.transaction.TransactionStatus;
import com.example.seckillservice.domain.*;
import com.example.seckillservice.mapper.*;
import com.example.seckillservice.service.IdempotencyService;
import com.example.seckillservice.service.KafkaOrderProducer;
import com.example.seckillservice.service.LuaStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分布式事务秒杀服务
 *
 * 采用 TCC (Try-Confirm-Cancel) 模式 + 可靠消息 实现分布式事务一致性
 *
 * 事务流程：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  用户下单请求                                               │
 * └───────────────────────────────┬─────────────────────────────┘
 *                                 ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Step 1: TCC Try - 库存预扣（冻结Redis库存）                  │
 * │  - IdempototencyService.tryAcquire() 幂等检查                │
 * │  - LuaStockService.decreaseStock() 原子扣库存                 │
 * └───────────────────────────────┬─────────────────────────────┘
 *                                 ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Step 2: TCC Confirm - 确认扣减（异步）                       │
 * │  - 生成订单号（雪花算法+基因）                                 │
 * │  - 创建秒杀订单（状态=排队中）                                 │
 * │  - ReliableMessageService.sendTransactionalMessage()        │
 * │  - KafkaOrderProducer 发送订单消息                           │
 * └───────────────────────────────┬─────────────────────────────┘
 *                                 ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Step 3: Kafka消费 - order-service创建正式订单                │
 * │  - KafkaOrderConsumer 消费消息                              │
 * │  - OrderService 创建订单（状态=待支付）                       │
 * └───────────────────────────────┬─────────────────────────────┘
 *                                 ▼
 * ┌─────────────────────────────────────────────────────────��───┐
 * │  异常处理：                                                  │
 * │  - Redis扣库存失败 → TCC Cancel → 回滚库存                    │
 * │  - Kafka发送失败 → 补偿重试 → 最终一致                       │
 * │  - 订单超时 → 取消 → TCC Cancel → 回滚库存                   │
 * └─────────────────────────────────────────────────────────────┘
 */
@Service
public class TransactionalSeckillService {

    private static final Logger log = LoggerFactory.getLogger(TransactionalSeckillService.class);
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
    private IdempotencyService idempotencyService;
    @Autowired
    private KafkaOrderProducer kafkaOrderProducer;
    @Autowired
    private TccTransactionCoordinator tccCoordinator;
    @Autowired
    private ReliableMessageService reliableMessageService;

    /**
     * 分布式事务秒杀下单
     *
     * @param userId  用户ID
     * @param request 秒杀请求
     * @return 秒杀响应
     */
    public SeckillResponse seckillWithTransaction(Long userId, SeckillRequest request) {
        Long activityId = request.getActivityId();
        Long productId = request.getProductId();
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;

        // 1. 幂等性检查
        if (!idempotencyService.tryAcquire(userId, activityId)) {
            throw new IllegalArgumentException("请求太频繁，请稍后重试");
        }

        // 2. 验证活动状态
        SeckillActivity activity = activityMapper.findById(activityId);
        if (activity == null) {
            idempotencyService.release(userId, activityId);
            throw new IllegalArgumentException("活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            idempotencyService.release(userId, activityId);
            throw new IllegalArgumentException("活动尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            idempotencyService.release(userId, activityId);
            throw new IllegalArgumentException("活动已结束");
        }

        // 3. 检查秒杀商品
        SeckillProduct seckillProduct = productMapper.findByActivityAndProduct(activityId, productId);
        if (seckillProduct == null) {
            idempotencyService.release(userId, activityId);
            throw new IllegalArgumentException("秒杀商品不存在");
        }

        // 4. 发起TCC分布式事务
        String orderNo = ID_GENERATOR.nextSeckillOrderNo(userId, activityId);
        String globalTxId = tccCoordinator.beginTransaction(
                "SECKILL_ORDER", orderNo, userId, activityId, productId, quantity);

        try {
            // 4.1 TCC Try阶段：预扣库存
            boolean trySuccess = tccCoordinator.tryPhase(
                    globalTxId, activityId, productId, quantity, seckillProduct.getLimitPerUser());

            if (!trySuccess) {
                // Try失败，直接Cancel
                tccCoordinator.cancelPhase(globalTxId, "库存预扣失败");
                idempotencyService.release(userId, activityId);
                throw new IllegalArgumentException("库存不足或超过购买限制");
            }

            // 4.2 创建秒杀订单（Try成功后的本地操作）
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setUserId(userId);
            seckillOrder.setActivityId(activityId);
            seckillOrder.setProductId(productId);
            seckillOrder.setOrderNo(orderNo);
            seckillOrder.setStatus(0); // 排队中
            orderMapper.insert(seckillOrder);

            // 4.3 发送可靠消息（异步创建正式订单）
            OrderMessage msg = new OrderMessage(
                    userId, productId, activityId,
                    "秒杀商品[" + productId + "]",
                    seckillProduct.getSeckillPrice(),
                    quantity,
                    seckillProduct.getSeckillPrice().multiply(java.math.BigDecimal.valueOf(quantity)),
                    orderNo
            );

            boolean msgSent = reliableMessageService.sendTransactionalMessage(globalTxId, msg);
            if (!msgSent) {
                // 消息发送失败，触发TCC Cancel
                log.error("订单消息发送失败，触发回滚: globalTxId={}", globalTxId);
                tccCoordinator.cancelPhase(globalTxId, "消息发送失败");
                orderMapper.updateStatusToFailed(seckillOrder.getId(), "消息发送失败");
                idempotencyService.release(userId, activityId);
                throw new IllegalArgumentException("订单创建失败，请重试");
            }

            // 4.4 TCC Confirm阶段
            tccCoordinator.confirmPhase(globalTxId);

            // 4.5 更新秒杀订单为成功
            orderMapper.updateStatusToSuccess(seckillOrder.getId(), 1, orderNo);

            log.info("=== 分布式事务秒杀成功 === globalTxId={}, userId={}, orderNo={}",
                    globalTxId, userId, orderNo);

            return SeckillResponse.ok(orderNo, "秒杀商品[" + productId + "]",
                    quantity,
                    seckillProduct.getSeckillPrice().multiply(java.math.BigDecimal.valueOf(quantity)),
                    LocalDateTime.now().plusMinutes(15));

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("分布式事务秒杀异常: globalTxId={}, error={}", globalTxId, e.getMessage(), e);
            // 触发TCC Cancel回滚
            tccCoordinator.cancelPhase(globalTxId, e.getMessage());
            idempotencyService.release(userId, activityId);
            throw new IllegalArgumentException("秒杀服务异常: " + e.getMessage());
        }
    }

    /**
     * TCC Confirm回调（用于异步确认）
     */
    public void confirm(Long orderId) {
        orderMapper.updateStatusToSuccess(orderId, 1, null);
    }

    /**
     * TCC Cancel回调（用于异步回滚）
     */
    public void cancel(Long activityId, Long productId, int quantity, Long userId) {
        luaStockService.increaseStock(activityId, productId, quantity);
        if (userId != null && activityId != null) {
            idempotencyService.release(userId, activityId);
        }
    }

    // ======== 活动管理 ========

    public List<SeckillActivity> getActiveActivities() {
        return activityMapper.findActiveActivities();
    }

    public SeckillActivity getActivity(Long id) {
        return activityMapper.findById(id);
    }

    public List<SeckillProduct> getActivityProducts(Long activityId) {
        return productMapper.findByActivityId(activityId);
    }

    public List<SeckillOrder> getUserOrders(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    /**
     * 初始化秒杀库存到Redis
     */
    public void initSeckillStock(Long activityId, Long productId) {
        SeckillProduct sp = productMapper.findByActivityAndProduct(activityId, productId);
        if (sp != null) {
            luaStockService.initStock(activityId, productId, sp.getStock());
            log.info("初始化秒杀库存: activityId={}, productId={}, stock={}",
                    activityId, productId, sp.getStock());
        }
    }

    // ======== 定时任务 ========

    @Scheduled(fixedDelay = 60000)
    public void updateActivityStatus() {
        List<SeckillActivity> activities = activityMapper.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (SeckillActivity activity : activities) {
            if (activity.getStatus() == 0 && now.isAfter(activity.getStartTime())) {
                activityMapper.updateStatus(activity.getId(), 1);
                log.info("活动开始: id={}", activity.getId());
            } else if (activity.getStatus() == 1 && now.isAfter(activity.getEndTime())) {
                activityMapper.updateStatus(activity.getId(), 2);
                log.info("活动结束: id={}", activity.getId());
            }
        }
    }
}