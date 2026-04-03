# Kafka异步秒杀

## 1. 概述

秒杀系统使用Kafka作为消息队列，实现高并发场景下的异步订单处理。

## 2. 架构设计

```
┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│   Client   │────▶│   Kafka    │────▶│ Consumer   │────▶│   MySQL    │
│            │     │  Broker    │     │   Group    │     │            │
└────────────┘     └────────────┘     └────────────┘     └────────────┘
                         │
                   ┌─────▼─────┐
                   │ Zookeeper │
                   └───────────┘
```

## 3. Topic配置

### 3.1 Topic创建

```bash
# 创建Topic
kafka-topics.sh --create \
  --topic seckill-order \
  --bootstrap-server localhost:9092 \
  --partitions 4 \
  --replication-factor 1

# 查看Topic列表
kafka-topics.sh --list --bootstrap-server localhost:9092
```

### 3.2 分区策略

| 分区数 | 说明 |
|--------|------|
| 4 | 并行消费，提升吞吐量 |
| 1 | 单副本，简单部署 |

## 4. 生产者配置

```java
@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic seckillOrderTopic() {
        return TopicBuilder.name("seckill-order")
            .partitions(4)
            .replicas(1)
            .build();
    }
}

@Service
public class SeckillOrderProducer {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrder(Long userId, Long productId) {
        kafkaTemplate.send("seckill-order",
            String.valueOf(userId),
            new OrderMessage(userId, productId));
    }
}
```

## 5. 消费者配置

```java
@Service
public class SeckillOrderConsumer {
    @KafkaListener(topics = "seckill-order", groupId = "seckill-order-group")
    public void consume(OrderMessage message) {
        // 创建订单
        orderService.createOrder(message);
    }
}
```

## 6. Lua脚本保证原子性

### 6.1 扣库存脚本

```lua
local stock_key = KEYS[1]
local user_key = KEYS[2]
local quantity = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

-- 检查用户购买限制
local user_bought = redis.call('GET', user_key)
if user_bought and tonumber(user_bought) >= limit then
    return -1  -- 超限
end

-- 检查库存
local stock = redis.call('GET', stock_key)
if not stock then
    return -2  -- 库存不存在
end
if tonumber(stock) < quantity then
    return -3  -- 库存不足
end

-- 扣减库存
redis.call('DECRBY', stock_key, quantity)
redis.call('INCR', user_key)

return tonumber(stock) - quantity
```

## 7. 消息幂等性

通过Redis实现消息去重：

```java
public boolean checkDuplicate(Long userId, Long productId) {
    String key = "seckill:order:" + userId + ":" + productId;
    return redisTemplate.hasKey(key);
}

public void markOrdered(Long userId, Long productId, String orderNo) {
    String key = "seckill:order:" + userId + ":" + productId;
    redisTemplate.opsForValue().set(key, orderNo, 24, TimeUnit.HOURS);
}
```

## 8. 性能调优

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| batch.size | 16384 | 批量发送大小 |
| linger.ms | 5 | 等待时间 |
| buffer.memory | 33554432 | 发送缓冲 |
| acks | 1 | 确认级别 |
| retries | 3 | 重试次数 |
