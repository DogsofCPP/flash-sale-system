package com.example.userservice.service;

import com.example.userservice.config.RabbitMQConfig;
import com.example.userservice.domain.OrderMessage;
import com.example.userservice.domain.SeckillOrder;
import com.example.userservice.mapper.OrderMapper;
import com.example.userservice.mapper.SeckillOrderMapper;
import com.example.userservice.mapper.SeckillProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageConsumer.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private SeckillProductMapper seckillProductMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    @Transactional
    public void handleOrderMessage(OrderMessage message) {
        log.info("收到订单消息: messageId={}, orderNo={}", message.getMessageId(), message.getOrderNo());

        try {
            com.example.userservice.domain.Order order = new com.example.userservice.domain.Order();
            order.setOrderNo(message.getOrderNo());
            order.setUserId(message.getUserId());
            order.setProductId(message.getProductId());
            order.setProductName(message.getProductName());
            order.setProductPrice(message.getProductPrice());
            order.setQuantity(message.getQuantity());
            order.setTotalAmount(message.getTotalAmount());
            order.setStatus(0);
            order.setCreatedAt(message.getCreatedAt());
            order.setExpireTime(message.getCreatedAt().plusMinutes(15));

            orderMapper.insert(order);

            SeckillOrder seckillOrder = seckillOrderMapper.findByUserActivityProduct(
                    message.getUserId(), message.getActivityId(), message.getProductId());
            if (seckillOrder != null) {
                seckillOrderMapper.updateStatus(seckillOrder.getId(), 1);
            }

            log.info("订单创建成功: orderNo={}", message.getOrderNo());

        } catch (Exception e) {
            log.error("创建订单失败: messageId={}, orderNo={}, error={}",
                    message.getMessageId(), message.getOrderNo(), e.getMessage(), e);
            throw e;
        }
    }
}
