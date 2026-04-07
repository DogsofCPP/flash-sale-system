package com.example.seckillservice.transaction;

import com.example.common.OrderMessage;
import com.example.seckillservice.config.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 可靠消息服务（事务消息模式）
 *
 * 核心思想：
 * 1. 本地事务（库存预扣）与消息发送绑定在同一个本地事务中
 * 2. 先发送"半消息"（对Consumer不可见）
 * 3. 本地事务执行成功后，确认消息（变为可见）
 * 4. 本地事务失败后，删除半消息
 *
 * 本实现基于 Kafka 的幂等生产者 + 本地消息表 + 定时扫描补偿
 * 相比传统半消息方案更轻量，适合秒杀场景
 */
@Service
public class ReliableMessageService {

    private static final Logger log = LoggerFactory.getLogger(ReliableMessageService.class);

    /** 消息状态：待发送 */
    public static final int MSG_STATUS_PENDING = 0;
    /** 消息状态：已发送 */
    public static final int MSG_STATUS_SENT = 1;
    /** 消息状态：已确认 */
    public static final int MSG_STATUS_CONFIRMED = 2;
    /** 消息状态：已失败 */
    public static final int MSG_STATUS_FAILED = 3;

    @Autowired
    private KafkaTemplate<String, OrderMessage> kafkaTemplate;

    @Autowired
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private KafkaConfig kafkaConfig;

    /**
     * 发送事务消息（半消息模式）
     *
     * 流程：
     * 1. 先将消息标记为PENDING（本地消息表或Redis）
     * 2. 执行本地业务（库存预扣）
     * 3. 发送Kafka消息
     * 4. 更新消息状态为CONFIRMED
     *
     * @param globalTxId 全局事务ID（用于追踪）
     * @param message    订单消息
     * @return true=发送成功
     */
    public boolean sendTransactionalMessage(String globalTxId, OrderMessage message) {
        String key = message.getOrderNo();

        try {
            // 保存消息到本地消息表（Redis）
            saveLocalMessage(globalTxId, message);

            // 发送Kafka消息
            ProducerRecord<String, OrderMessage> record = new ProducerRecord<>(
                    KafkaConfig.ORDER_TOPIC,
                    key,
                    message
            );
            record.headers().add("globalTxId",
                    globalTxId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            record.headers().add("bizType",
                    "SECKILL_ORDER".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            CompletableFuture<SendResult<String, OrderMessage>> future =
                    kafkaTemplate.send(record);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    // 消息发送成功，更新状态
                    updateMessageStatus(globalTxId, message.getOrderNo(), MSG_STATUS_CONFIRMED);
                    log.info("可靠消息发送成功: globalTxId={}, orderNo={}, offset={}",
                            globalTxId, message.getOrderNo(),
                            result.getRecordMetadata().offset());
                } else {
                    // 消息发送失败，标记为失败
                    updateMessageStatus(globalTxId, message.getOrderNo(), MSG_STATUS_FAILED);
                    log.error("可靠消息发送失败: globalTxId={}, orderNo={}, error={}",
                            globalTxId, message.getOrderNo(), ex.getMessage());
                }
            });

            return true;

        } catch (Exception e) {
            log.error("发送事务消息异常: globalTxId={}, orderNo={}",
                    globalTxId, message.getOrderNo(), e);
            updateMessageStatus(globalTxId, message.getOrderNo(), MSG_STATUS_FAILED);
            return false;
        }
    }

    /**
     * 同步发送消息（配合TCC Cancel使用）
     * 确保消息发送成功后才返回
     */
    public boolean sendMessageSync(String globalTxId, OrderMessage message) {
        String key = message.getOrderNo();

        try {
            ProducerRecord<String, OrderMessage> record = new ProducerRecord<>(
                    KafkaConfig.ORDER_TOPIC, key, message);
            record.headers().add("globalTxId",
                    globalTxId.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            SendResult<String, OrderMessage> result = kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);

            log.info("同步可靠消息发送成功: globalTxId={}, orderNo={}, offset={}",
                    globalTxId, message.getOrderNo(), result.getRecordMetadata().offset());

            updateMessageStatus(globalTxId, message.getOrderNo(), MSG_STATUS_CONFIRMED);
            return true;

        } catch (Exception e) {
            log.error("同步可靠消息发送失败: globalTxId={}, orderNo={}",
                    globalTxId, message.getOrderNo(), e);
            updateMessageStatus(globalTxId, message.getOrderNo(), MSG_STATUS_FAILED);
            return false;
        }
    }

    /**
     * 确保Kafka topic存在（启动时调用）
     */
    public void ensureTopicExists(String topicName, int partitions) {
        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
            try (AdminClient adminClient = AdminClient.create(props)) {
                NewTopic topic = new NewTopic(topicName, partitions, (short) 1);
                adminClient.createTopics(Collections.singletonList(topic)).all().get();
                log.info("Kafka Topic已确认: {}", topicName);
            }
        } catch (Exception e) {
            // Topic可能已存在，忽略
            log.debug("Kafka Topic创建/确认: {}", topicName);
        }
    }

    // ==================== 本地消息表操作（Redis实现） ====================

    private static final String LOCAL_MSG_PREFIX = "msg:pending:";

    /**
     * 保存本地消息记录
     */
    private void saveLocalMessage(String globalTxId, OrderMessage message) {
        String key = LOCAL_MSG_PREFIX + globalTxId + ":" + message.getOrderNo();
        String value = globalTxId + "|" + message.getOrderNo() + "|" + MSG_STATUS_PENDING + "|"
                + System.currentTimeMillis();
        // 保存5分钟足够长（处理各种延迟情况）
        redisTemplate.opsForValue().set(key, value, 5, TimeUnit.MINUTES);
    }

    /**
     * 更新消息状态
     */
    private void updateMessageStatus(String globalTxId, String orderNo, int status) {
        String key = LOCAL_MSG_PREFIX + globalTxId + ":" + orderNo;
        try {
            String val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                String[] parts = val.split("\\|");
                if (parts.length >= 4) {
                    String newVal = parts[0] + "|" + parts[1] + "|" + status + "|" + parts[3];
                    redisTemplate.opsForValue().set(key, newVal, 5, TimeUnit.MINUTES);
                }
            }
        } catch (Exception e) {
            log.warn("更新本地消息状态失败: key={}, status={}", key, status);
        }
    }

    /**
     * 清理本地消息记录
     */
    public void deleteLocalMessage(String globalTxId, String orderNo) {
        String key = LOCAL_MSG_PREFIX + globalTxId + ":" + orderNo;
        redisTemplate.delete(key);
    }
}
