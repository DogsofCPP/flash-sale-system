package com.example.userservice.service;

import com.example.userservice.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ConcurrentHashMap<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(100);

    public Order createOrder(Long userId, Long productId, String productName,
                            BigDecimal productPrice, Integer quantity,
                            String orderNo, Long activityId, Long seckillProductId) {
        BigDecimal totalAmount = productPrice.multiply(BigDecimal.valueOf(quantity));

        Order order = new Order();
        order.setId(idCounter.incrementAndGet());
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setProductName(productName);
        order.setProductPrice(productPrice);
        order.setQuantity(quantity);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setCreatedAt(LocalDateTime.now());

        orders.put(order.getId(), order);
        log.info("订单创建成功: orderNo={}, userId={}, totalAmount={}", orderNo, userId, totalAmount);
        return order;
    }

    public void createOrderAsync(Long userId, Long productId, String productName,
                                BigDecimal productPrice, Integer quantity,
                                String orderNo, Long activityId, Long seckillProductId) {
        createOrder(userId, productId, productName, productPrice, quantity, orderNo, activityId, seckillProductId);
    }

    public Order getOrderById(Long id) {
        return orders.get(id);
    }

    public Order getOrderByOrderNo(String orderNo) {
        return orders.values().stream()
                .filter(o -> o.getOrderNo().equals(orderNo))
                .findFirst()
                .orElse(null);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orders.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .toList();
    }

    public boolean cancelOrder(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order == null) {
            return false;
        }

        if (order.getStatus() != 0) {
            return false;
        }

        order.setStatus(2); // 已取消
        log.info("订单已取消: orderNo={}", orderNo);
        return true;
    }
}
