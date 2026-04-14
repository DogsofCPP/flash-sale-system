package com.example.userservice.service;

import com.example.userservice.config.RabbitMQConfig;
import com.example.userservice.domain.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendOrderMessage(OrderMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_EXCHANGE,
                    RabbitMQConfig.SECKILL_ROUTING_KEY,
                    message
            );
            log.info("订单消息已发送: orderNo={}, exchange={}, routingKey={}",
                    message.getOrderNo(), RabbitMQConfig.SECKILL_EXCHANGE, RabbitMQConfig.SECKILL_ROUTING_KEY);
        } catch (Exception e) {
            log.error("发送订单消息失败: orderNo={}, error={}", message.getOrderNo(), e.getMessage(), e);
            throw new RuntimeException("消息发送失败: " + e.getMessage());
        }
    }
}
