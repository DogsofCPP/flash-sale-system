package com.example.seckillservice.service;

import com.example.common.OrderMessage;
import com.example.seckillservice.config.KafkaConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaOrderProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderProducer.class);

    @Autowired
    private KafkaTemplate<String, OrderMessage> kafkaTemplate;

    public void sendOrderMessage(OrderMessage message) {
        String key = message.getUserId() + "-" + message.getActivityId();

        ProducerRecord<String, OrderMessage> record = new ProducerRecord<>(
                KafkaConfig.ORDER_TOPIC,
                key,
                message
        );

        record.headers().add("orderNo", message.getOrderNo().getBytes(StandardCharsets.UTF_8));
        record.headers().add("userId", String.valueOf(message.getUserId()).getBytes(StandardCharsets.UTF_8));
        record.headers().add("activityId", String.valueOf(message.getActivityId()).getBytes(StandardCharsets.UTF_8));

        CompletableFuture<SendResult<String, OrderMessage>> future = kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka订单消息发送成功: orderNo={}, partition={}, offset={}",
                        message.getOrderNo(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Kafka订单消息发送失败: orderNo={}, error={}",
                        message.getOrderNo(), ex.getMessage(), ex);
            }
        });
    }

    public void sendOrderMessageSync(OrderMessage message) {
        String key = message.getUserId() + "-" + message.getActivityId();
        try {
            ProducerRecord<String, OrderMessage> record = new ProducerRecord<>(
                    KafkaConfig.ORDER_TOPIC, key, message);
            kafkaTemplate.send(record).get();
            log.info("Kafka订单消息同步发送成功: orderNo={}", message.getOrderNo());
        } catch (Exception e) {
            log.error("Kafka订单消息同步发送失败: orderNo={}", message.getOrderNo(), e);
            throw new RuntimeException("消息发送失败", e);
        }
    }
}
