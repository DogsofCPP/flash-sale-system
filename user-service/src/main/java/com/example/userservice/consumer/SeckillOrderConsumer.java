package com.example.userservice.consumer;

import com.example.userservice.config.RabbitMQConfig;
import com.example.userservice.domain.OrderMessage;
import com.example.userservice.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    @Autowired
    private OrderMapper orderMapper;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void consume(OrderMessage message) {
        log.info("收到秒杀订单消息: orderNo={}, userId={}, productId={}",
                message.getOrderNo(), message.getUserId(), message.getProductId());

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
            log.info("秒杀订单创建成功: orderNo={}", message.getOrderNo());
        } catch (Exception e) {
            log.error("秒杀订单创建失败: orderNo={}, error={}",
                    message.getOrderNo(), e.getMessage(), e);
            throw e;
        }
    }
}
