package com.example.userservice.service;

import com.example.userservice.domain.Order;
import com.example.userservice.mapper.OrderMapper;
import com.example.userservice.mapper.SeckillProductMapper;
import com.example.userservice.mapper.SeckillOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private SeckillProductMapper seckillProductMapper;

    @Autowired
    private RedisStockService redisStockService;

    @Autowired
    private com.example.userservice.config.RedisConfig redisConfig;

    @Transactional
    public Order createOrder(Long userId, Long productId, String productName,
                            BigDecimal productPrice, Integer quantity,
                            String orderNo, Long activityId, Long seckillProductId) {
        BigDecimal totalAmount = productPrice.multiply(BigDecimal.valueOf(quantity));

        Order order = new Order(orderNo, userId, productId, productName,
                productPrice, quantity, totalAmount);

        orderMapper.insert(order);

        log.info("订单创建成功: orderNo={}, userId={}, totalAmount={}", orderNo, userId, totalAmount);
        return order;
    }

    public void createOrderAsync(Long userId, Long productId, String productName,
                                BigDecimal productPrice, Integer quantity,
                                String orderNo, Long activityId, Long seckillProductId) {
        try {
            redisConfig.asyncExecutor().execute(() -> {
                try {
                    createOrder(userId, productId, productName, productPrice,
                            quantity, orderNo, activityId, seckillProductId);
                } catch (Exception e) {
                    log.error("异步创建订单失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            log.error("提交异步任务失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
        }
    }

    public Order getOrderById(Long id) {
        return orderMapper.findById(id);
    }

    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    @Transactional
    public boolean cancelOrder(String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            return false;
        }

        if (order.getStatus() != 0) {
            return false;
        }

        int result = orderMapper.cancelOrder(order.getId());
        if (result > 0) {
            seckillProductMapper.increaseStock(
                    seckillOrderMapper.findByUserActivityProduct(order.getUserId(), null, order.getProductId()).getActivityId(),
                    order.getProductId(),
                    order.getQuantity()
            );

            log.info("订单已取消: orderNo={}, 已回滚库存", orderNo);
            return true;
        }
        return false;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        log.info("开始检查过期订单...");
        List<Order> expiredOrders = orderMapper.findExpiredOrders(0);

        for (Order order : expiredOrders) {
            try {
                int result = orderMapper.cancelOrder(order.getId());
                if (result > 0) {
                    log.info("订单已过期取消: orderNo={}", order.getOrderNo());
                }
            } catch (Exception e) {
                log.error("取消过期订单失败: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            }
        }

        if (!expiredOrders.isEmpty()) {
            log.info("本次检查过期订单数量: {}", expiredOrders.size());
        }
    }
}
