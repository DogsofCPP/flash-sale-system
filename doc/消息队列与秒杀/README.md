# 消息队列与秒杀

## 1. 概述

系统使用RabbitMQ作为消息队列，实现秒杀请求与订单创建的异步解耦。

## 2. 消息流程

```
用户请求 ──→ 扣减Redis库存 ──→ 发送MQ消息 ──→ 消费者创建订单
                        │                        │
                        ▼                        ▼
                    返回成功                 订单持久化
```

## 3. 核心组件

### 3.1 交换机和队列

| 名称 | 类型 | 说明 |
|------|------|------|
| order.exchange | Direct | 订单交换机 |
| order.queue | Queue | 订单队列 |
| dlx.exchange | Direct | 死信交换机 |
| dlx.queue | Queue | 死信队列 |

### 3.2 消息发送 (OrderMessageProducer)

```java
public void sendOrderMessage(OrderMessage message) {
    rabbitTemplate.convertAndSend(
        "order.exchange",
        "order.create",
        message
    );
}
```

### 3.3 消息消费 (OrderMessageConsumer)

```java
@RabbitListener(queues = "order.queue")
public void handleOrder(OrderMessage message) {
    // 1. 创建订单
    // 2. 更新秒杀订单状态
    // 3. 记录库存流水
}
```

## 4. 消息格式

```json
{
    "orderNo": "SK1704067200000001",
    "userId": 1001,
    "activityId": 1,
    "productId": 100,
    "quantity": 1,
    "unitPrice": 99.00,
    "totalAmount": 99.00,
    "createTime": "2026-01-01T12:00:00"
}
```

## 5. 可靠性保证

### 5.1 生产者确认

```java
rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
    if (!ack) {
        log.error("消息发送失败: {}", cause);
    }
});
```

### 5.2 消费者确认

```java
channel.basicAck(tag, false);  // 成功确认
channel.basicNack(tag, false, true);  // 失败重回队列
channel.basicReject(tag, false);  // 失败丢弃
```

### 5.3 死信队列

消息处理失败后自动进入死信队列，便于排查问题。

## 6. 性能优化

- 批量消费：配置prefetch数量
- 并发消费：设置并发线程数
- 消息持久化：交换机、队列、消息均持久化
