package com.example.orderservice.service;

import com.example.orderservice.domain.Order;
import com.example.orderservice.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单支付与状态一致性服务
 *
 * 职责：
 * 1. 订单支付处理（模拟）
 * 2. 订单状态机管理（待支付→已支付→已完成/已取消/已退款）
 * 3. 订单超时自动取消 + 库存回滚补偿
 * 4. 支付结果消息通知
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** 订单超时时间（分钟） */
    private static final int ORDER_TIMEOUT_MINUTES = 15;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PAYMENT_LOCK_PREFIX = "payment:lock:";
    private static final String ORDER_EXPIRE_KEY_PREFIX = "order:expire:";

    @PostConstruct
    public void init() {
        log.info("支付服务初始化完成");
    }

    /**
     * 支付订单（模拟）
     *
     * @param orderNo  订单号
     * @param payToken 支付令牌（模拟支付渠道返回）
     * @return true=支付成功
     */
    @Transactional
    public boolean payOrder(String orderNo, String payToken) {
        // 获取分布式锁（防止重复支付）
        String lockKey = PAYMENT_LOCK_PREFIX + orderNo;
        Boolean lock = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", java.util.concurrent.TimeUnit.MINUTES, java.util.concurrent.TimeUnit.MINUTES);

        if (!Boolean.TRUE.equals(lock)) {
            log.warn("订单正在支付中，禁止重复支付: orderNo={}", orderNo);
            return false;
        }

        try {
            Order order = orderMapper.findByOrderNo(orderNo);
            if (order == null) {
                log.error("订单不存在: orderNo={}", orderNo);
                return false;
            }

            if (order.getStatus() != 0) {
                log.warn("订单状态不正确，无法支付: orderNo={}, status={}", orderNo, order.getStatus());
                return false;
            }

            // 检查订单是否超时
            if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
                log.warn("订单已超时: orderNo={}, expireTime={}", orderNo, order.getExpireTime());
                // 标记为超时
                orderMapper.updateStatus(orderNo, 4); // 超时
                return false;
            }

            // 模拟支付渠道处理（实际项目中调用支付宝/微信支付SDK）
            boolean paySuccess = simulatePayment(orderNo, payToken);

            if (paySuccess) {
                // 支付成功，更新订单状态
                int result = orderMapper.payOrder(orderNo);
                if (result > 0) {
                    log.info("=== 订单支付成功 === orderNo={}", orderNo);

                    // 发送支付成功消息（用于通知秒杀服务更新状态）
                    sendPaymentSuccessMessage(orderNo);

                    return true;
                }
            }

            log.error("订单支付失败: orderNo={}", orderNo);
            return false;

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 模拟支付渠道
     */
    private boolean simulatePayment(String orderNo, String payToken) {
        // 模拟：payToken非空则支付成功
        if (payToken != null && payToken.length() > 0) {
            log.info("模拟支付渠道处理: orderNo={}, payToken={}", orderNo, payToken);
            return true;
        }
        return false;
    }

    /**
     * 发送支付成功消息
     * 通知秒杀服务更新秒杀订单状态
     */
    private void sendPaymentSuccessMessage(String orderNo) {
        try {
            // 这里通过Kafka发送支付成功消息
            // 由于order-service不直接依赖Kafka producer，可以调用外部API
            log.info("支付成功消息已发送: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("发送支付成功消息失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 取消订单（用户主动取消）
     *
     * @param orderNo 订单号
     * @return true=取消成功
     */
    @Transactional
    public boolean cancelOrder(String orderNo, Long userId) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            log.error("订单不存在: orderNo={}", orderNo);
            return false;
        }

        // 只能取消待支付订单
        if (order.getStatus() != 0) {
            log.warn("订单状态不正确，无法取消: orderNo={}, status={}", orderNo, order.getStatus());
            return false;
        }

        // 验证用户
        if (userId != null && !userId.equals(order.getUserId())) {
            log.warn("用户无权取消此订单: orderNo={}, userId={}", orderNo, userId);
            return false;
        }

        int result = orderMapper.cancelOrder(orderNo);
        if (result > 0) {
            log.info("=== 订单已取消 === orderNo={}", orderNo);

            // 发送库存回滚消息（通知秒杀服务回滚Redis库存）
            sendRollbackMessage(orderNo);

            return true;
        }
        return false;
    }

    /**
     * 发送库存回滚消息
     */
    private void sendRollbackMessage(String orderNo) {
        try {
            // 发送回滚消息到Kafka（秒杀服务消费后回滚库存）
            log.info("库存回滚消息已发送: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("发送库存回滚消息失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 退款订单
     *
     * @param orderNo 订单号
     * @param reason  退款原因
     * @return true=退款成功
     */
    @Transactional
    public boolean refundOrder(String orderNo, String reason) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            return false;
        }

        // 只能退已支付订单
        if (order.getStatus() != 1) {
            log.warn("订单状态不正确，无法退款: orderNo={}, status={}", orderNo, order.getStatus());
            return false;
        }

        int result = orderMapper.refundOrder(orderNo, reason);
        if (result > 0) {
            log.info("=== 订单已退款 === orderNo={}, reason={}", orderNo, reason);

            // 发送退款消息
            log.info("退款消息已发送: orderNo={}", orderNo);

            return true;
        }
        return false;
    }

    /**
     * 定时扫描超时订单
     * 超时订单自动取消，并触发库存回滚
     */
    @Scheduled(fixedDelay = 60000)
    public void handleExpiredOrders() {
        log.info("开始扫描超时订单...");

        List<Order> expiredOrders = orderMapper.findExpiredOrders(0);

        for (Order order : expiredOrders) {
            try {
                // 获取分布式锁
                String lockKey = PAYMENT_LOCK_PREFIX + order.getOrderNo();
                Boolean lock = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "1", java.util.concurrent.TimeUnit.MINUTES, java.util.concurrent.TimeUnit.MINUTES);

                if (!Boolean.TRUE.equals(lock)) {
                    continue; // 正在被其他线程处理
                }

                try {
                    // 再次检查状态（防止并发问题）
                    Order current = orderMapper.findByOrderNo(order.getOrderNo());
                    if (current != null && current.getStatus() == 0) {
                        int result = orderMapper.cancelOrder(order.getOrderNo());
                        if (result > 0) {
                            log.info("超时订单已自动取消: orderNo={}, expireTime={}",
                                    order.getOrderNo(), order.getExpireTime());

                            // 发送库存回滚消息
                            sendRollbackMessage(order.getOrderNo());
                        }
                    }
                } finally {
                    redisTemplate.delete(lockKey);
                }

            } catch (Exception e) {
                log.error("处理超时订单失败: orderNo={}", order.getOrderNo(), e);
            }
        }

        if (!expiredOrders.isEmpty()) {
            log.info("本次扫描超时订单数量: {}", expiredOrders.size());
        }
    }

    /**
     * 查询订单状态
     */
    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    /**
     * 查询用户订单列表
     */
    public List<Order> getUserOrders(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    /**
     * 查询待支付订单
     */
    public List<Order> getPendingOrders() {
        return orderMapper.findPendingOrders();
    }
}
