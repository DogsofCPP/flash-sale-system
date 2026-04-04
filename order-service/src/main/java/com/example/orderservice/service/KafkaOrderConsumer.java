package com.example.orderservice.service;

import com.example.common.OrderMessage;
import com.example.orderservice.config.KafkaConfig;
import com.example.orderservice.domain.Order;
import com.example.orderservice.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

/**
 * Kafka 订单消息消费者
 *
 * 从 seckill-service 消费秒杀订单消息，异步创建正式订单
 * 支持手动提交 offset 保证消息不丢失
 */
@Service
public class KafkaOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderConsumer.class);

    @Autowired
    private OrderMapper orderMapper;

    @KafkaListener(
            topics = KafkaConfig.ORDER_TOPIC,
            groupId = KafkaConfig.CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderMessage(OrderMessage msg,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {
        try {
            log.info("消费订单消息: orderNo={}, userId={}, partition={}, offset={}",
                    msg.getOrderNo(), msg.getUserId(), partition, offset);

            // 幂等检查：防止重复消费
            if (orderMapper.findByOrderNo(msg.getOrderNo()) != null) {
                log.warn("订单已存在，跳过: orderNo={}", msg.getOrderNo());
                ack.acknowledge();
                return;
            }

            // 创建订单
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
            order.setCreateTime(java.time.LocalDateTime.now());

            orderMapper.insert(order);
            log.info("订单创建成功: orderNo={}", msg.getOrderNo());

            // 手动提交 offset
            ack.acknowledge();

        } catch (Exception e) {
            log.error("订单创建失败: orderNo={}, error={}", msg.getOrderNo(), e.getMessage(), e);
            // 不ack，让消息重试
            throw e;
        }
    }
}
