package com.example.orderservice.service;

import com.example.common.OrderMessage;
import com.example.orderservice.domain.Order;
import com.example.orderservice.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;

@Service
public class OrderMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageConsumer.class);

    @Autowired
    private OrderMapper orderMapper;

    @RabbitListener(queues = "order.queue")
    public void handleOrderMessage(OrderMessage msg,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            log.info("消费订单消息: orderNo={}, userId={}", msg.getOrderNo(), msg.getUserId());

            Order order = new Order();
            order.setOrderNo(msg.getOrderNo());
            order.setUserId(msg.getUserId());
            order.setProductId(msg.getProductId());
            order.setActivityId(msg.getActivityId());
            order.setProductName(msg.getProductName());
            order.setProductPrice(msg.getProductPrice());
            order.setQuantity(msg.getQuantity());
            order.setTotalAmount(msg.getTotalAmount());
            order.setStatus(0); // 待支付
            order.setExpireTime(java.time.LocalDateTime.now().plusMinutes(15));

            orderMapper.insert(order);
            log.info("订单创建成功: orderNo={}", msg.getOrderNo());

            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("订单创建失败: orderNo={}, error={}", msg.getOrderNo(), e.getMessage(), e);
            try {
                channel.basicNack(tag, false, true);
            } catch (java.io.IOException ex) {
                log.error("消息确认失败: error={}", ex.getMessage());
            }
            throw new RuntimeException("订单处理失败", e);
        }
    }
}
