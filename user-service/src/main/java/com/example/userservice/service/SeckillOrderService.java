package com.example.userservice.service;

import com.example.userservice.domain.SeckillOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 秒杀订单服务
 * 内存实现，用于无数据库环境
 */
@Service
public class SeckillOrderService {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderService.class);

    private final Map<String, SeckillOrder> orders = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    /**
     * 创建秒杀订单
     *
     * @param order 订单信息
     * @return 创建的订单
     */
    public SeckillOrder createOrder(SeckillOrder order) {
        if (order.getOrderNo() == null) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (orders.containsKey(order.getOrderNo())) {
            log.warn("订单已存在: orderNo={}", order.getOrderNo());
            return orders.get(order.getOrderNo());
        }

        order.setId(idCounter.getAndIncrement());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (order.getStatus() == null) {
            order.setStatus(0);
        }

        orders.put(order.getOrderNo(), order);
        log.info("秒杀订单创建成功: orderNo={}, userId={}, productId={}",
                order.getOrderNo(), order.getUserId(), order.getProductId());

        return order;
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    public SeckillOrder getByOrderNo(String orderNo) {
        return orders.get(orderNo);
    }

    /**
     * 更新订单状态
     *
     * @param orderNo 订单号
     * @param status  新状态
     * @return 是否更新成功
     */
    public boolean updateStatus(String orderNo, String status) {
        SeckillOrder order = orders.get(orderNo);
        if (order == null) {
            return false;
        }

        if ("PENDING".equals(status)) {
            order.setStatus(0);
        } else if ("SUCCESS".equals(status) || "PAID".equals(status)) {
            order.setStatus(1);
        } else if ("CANCELLED".equals(status) || "CANCEL".equals(status)) {
            order.setStatus(2);
        }
        order.setUpdatedAt(LocalDateTime.now());
        log.info("Order status updated: orderNo={}, status={}", orderNo, status);
        return true;
    }

    /**
     * 计算订单总金额
     *
     * @param productId 商品ID
     * @param quantity  购买数量
     * @param unitPrice 单价
     * @return 总金额
     */
    public BigDecimal calculateAmount(Long productId, Integer quantity, BigDecimal unitPrice) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
