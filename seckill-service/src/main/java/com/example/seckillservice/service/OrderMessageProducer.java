package com.example.seckillservice.service;

import com.example.common.OrderMessage;
import com.example.seckillservice.config.RabbitMQConfig;
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
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                message
        );
        log.info("订单消息发送成功: orderNo={}", message.getOrderNo());
    }
}
