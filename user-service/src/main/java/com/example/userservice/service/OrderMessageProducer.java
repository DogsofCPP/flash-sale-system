package com.example.userservice.service;

import com.example.userservice.config.RabbitMQConfig;
import com.example.userservice.domain.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendOrderMessage(OrderMessage message) {
        try {
            CorrelationData correlationData = new CorrelationData(message.getMessageId());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_ROUTING_KEY,
                    message,
                    correlationData
            );

            log.info("订单消息已发送: messageId={}, orderNo={}", message.getMessageId(), message.getOrderNo());
        } catch (Exception e) {
            log.error("发送订单消息失败: messageId={}, error={}", message.getMessageId(), e.getMessage(), e);
            throw new RuntimeException("消息发送失败: " + e.getMessage());
        }
    }
}
