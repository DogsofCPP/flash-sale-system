package com.example.userservice.producer;

import com.example.userservice.config.SeckillRabbitMQConfig;
import com.example.userservice.event.SeckillOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单消息生产者
 * 发送秒杀订单创建消息到RabbitMQ
 */
@Component
public class SeckillOrderProducer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀订单创建事件
     *
     * @param event 秒杀订单事件
     */
    public void sendOrderEvent(SeckillOrderEvent event) {
        log.info("发送秒杀订单消息: orderNo={}, userId={}, productId={}",
                event.getOrderNo(), event.getUserId(), event.getProductId());

        rabbitTemplate.convertAndSend(
                SeckillRabbitMQConfig.SECKILL_EXCHANGE,
                SeckillRabbitMQConfig.SECKILL_ROUTING_KEY,
                event
        );

        log.info("秒杀订单消息发送成功: orderNo={}", event.getOrderNo());
    }
}
