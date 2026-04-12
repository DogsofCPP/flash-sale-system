package com.example.userservice.consumer;

import com.example.userservice.config.SeckillRabbitMQConfig;
import com.example.userservice.domain.SeckillOrder;
import com.example.userservice.event.SeckillOrderEvent;
import com.example.userservice.service.SeckillOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单消息消费者
 * 监听秒杀订单队列，异步创建订单
 */
@Component
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    @Autowired
    private SeckillOrderService seckillOrderService;

    /**
     * 监听秒杀订单队列
     *
     * @param event 秒杀订单事件
     */
    @RabbitListener(queues = SeckillRabbitMQConfig.SECKILL_QUEUE)
    public void consume(SeckillOrderEvent event) {
        log.info("收到秒杀订单消息: orderNo={}, userId={}, productId={}",
                event.getOrderNo(), event.getUserId(), event.getProductId());

        try {
            // 创建订单
            SeckillOrder order = SeckillOrder.builder()
                    .orderNo(event.getOrderNo())
                    .userId(event.getUserId())
                    .productId(event.getProductId())
                    .activityId(event.getActivityId())
                    .quantity(event.getQuantity())
                    .totalAmount(event.getTotalAmount())
                    .status(0)
                    .build();

            seckillOrderService.createOrder(order);
            log.info("秒杀订单创建成功: orderNo={}", event.getOrderNo());
        } catch (Exception e) {
            log.error("秒杀订单创建失败: orderNo={}, error={}", event.getOrderNo(), e.getMessage(), e);
            // 异常会被Spring AMQP处理
            throw e;
        }
    }
}
