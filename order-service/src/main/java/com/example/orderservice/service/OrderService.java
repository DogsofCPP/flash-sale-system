package com.example.orderservice.service;

import com.example.orderservice.domain.Order;
import com.example.orderservice.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderMapper orderMapper;

    public Order getOrderById(Long id) {
        return orderMapper.findById(id);
    }

    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    public List<Order> getOrdersByUserIdAndStatus(Long userId, Integer status) {
        return orderMapper.findByUserIdAndStatus(userId, status);
    }

    public boolean cancelOrder(String orderNo) {
        int result = orderMapper.cancelOrder(orderNo);
        if (result > 0) {
            log.info("订单已取消: orderNo={}", orderNo);
            return true;
        }
        return false;
    }

    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredOrders() {
        log.info("开始检查过期订单...");
        List<Order> expiredOrders = orderMapper.findExpiredOrders(0);
        for (Order order : expiredOrders) {
            try {
                int result = orderMapper.cancelOrder(order.getOrderNo());
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
