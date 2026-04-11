# 分布式事务实现文档

## 目录

1. [概述](#1-概述)
2. [技术方案对比](#2-技术方案对比)
3. [TCC分布式事务实现](#3-tcc分布式事务实现)
4. [可靠消息服务](#4-可靠消息服务)
5. [订单支付与状态一致性](#5-订单支付与状态一致性)
6. [架构流程图](#6-架构流程图)
7. [关键代码说明](#7-关键代码说明)
8. [API接口](#8-api接口)

---

## 1. 概述

### 1.1 需求背景

在分布式架构下，秒杀下单涉及多个微服务：

- **秒杀服务（seckill-service）**：管理活动、商品、库存预扣
- **订单服务（order-service）**：管理订单创建、支付、取消

这两个服务各自拥有独立的数据库，秒杀下单需要同时保证：
1. 库存扣减与订单创建的数据一致性
2. 订单支付与订单状态更新的最终一致性

### 1.2 一致性目标

| 场景 | 一致性类型 | 实现方案 |
|------|-----------|---------|
| 秒杀下单（库存+订单） | 强一致性 | TCC + 可靠消息 |
| 订单支付（支付+状态） | 最终一致性 | 本地事务 + MQ通知 |
| 超时订单（取消+库存回滚） | 最终一致性 | 定时任务 + MQ补偿 |

---

## 2. 技术方案对比

### 2.1 常见分布式事务方案

| 方案 | 原理 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|---------|
| **2PC** | 两阶段提交，协调者阻塞 | 实现简单 | 锁定资源时间长，单点故障 | 不推荐 |
| **TCC** | Try-Confirm-Cancel，自主补偿 | 不锁定资源，性能高 | 业务侵入性强 | 高并发场景 |
| **可靠消息** | 本地消息表+MQ | 低耦合 | 可能短暂不一致 | 异步场景 |
| **Saga** | 链式调用+补偿 | 无锁定 | 无回滚，需正向补偿 | 长流程 |

### 2.2 本项目选型

本项目采用 **TCC + 可靠消息** 混合方案：

- **TCC**：保障秒杀下单时"库存预扣 + 订单创建"的一致性
- **可靠消息**：Kafka异步解耦，实现削峰填谷
- **本地事务 + MQ**：保障"订单支付 + 状态更新"的一致性

---

## 3. TCC分布式事务实现

### 3.1 TCC核心概念

TCC 将分布式事务分为三个阶段：

```
┌─────────────────────────────────────────────┐
│              Try（预留资源）                  │
│  锁定库存、冻结资源、检查业务前置条件           │
│  失败 → Cancel（释放）                      │
│  成功 → Confirm（确认）                     │
└────────────────────┬────────────────────────┘
                     │ 成功
┌────────────────────▼────────────────────────┐
│           Confirm（确认执行）               │
│  真正扣减库存、创建订单、发送消息             │
│  失败 → 重试3次 → 告警人工介入               │
└─────────────────────────────────────────────┘
```

### 3.2 秒杀场景的TCC设计

| 阶段 | seckill-service（库存参与者） | order-service（订单参与者） |
|------|------------------------------|------------------------------|
| **Try** | Redis预扣库存（Lua原子操作） | 创建订单记录（状态=待支付） |
| **Confirm** | 确认扣减，更新DB已售数量 | 更新订单状态为已创建 |
| **Cancel** | 回滚Redis库存（+1） | 删除/标记订单为已取消 |

### 3.3 TCC注解定义

```java
@TccTry        // 标注Try阶段方法
@TccConfirm    // 标注Confirm阶段方法
@TccCancel     // 标注Cancel阶段方法
@TccTransaction // 标注参与TCC事务的方法
```

### 3.4 TCC协调器

`TccTransactionCoordinator` 负责：

1. 创建全局事务（生成 `globalTxId`）
2. 协调各参与者执行 Try/Confirm/Cancel
3. 管理事务状态（Redis持久化）
4. 处理超时和异常补偿

---

## 4. 可靠消息服务

### 4.1 可靠消息原理

```
┌──────────────┐     1.保存半消息     ┌──────────────┐
│ 本地业务      │ ─────────────────> │ 本地消息表     │
│ (库存预扣)    │                    │ (Redis/MySQL) │
└──────┬───────┘                    └──────────────┘
       │
       │ 2.执行业务
       ▼
┌──────────────┐     3.发送MQ消息    ┌──────────────┐
│ 事务成功      │ ─────────────────> │ Kafka Cluster │
└──────┬───────┘                    └──────────────┘
       │
       │ 4.更新消息状态
       ▼
┌──────────────┐
│ 确认消息      │
└──────────────┘
```

### 4.2 消息幂等性保证

消费端（order-service）通过以下方式保证幂等：

```java
// 消费消息前先检查订单是否已存在
if (orderMapper.findByOrderNo(orderNo) != null) {
    ack.acknowledge(); // 已处理过，直接ACK
    return;
}
// 创建订单...
```

### 4.3 Kafka消息格式

```json
{
  "globalTxId": "SECKILL_ORDER_SK20260101120000001...",
  "bizType": "SECKILL_ORDER",
  "orderNo": "SK20260101120000001...",
  "userId": 12345,
  "activityId": 1,
  "productId": 100,
  "quantity": 1,
  "totalAmount": 5999.00,
  "status": 0
}
```

---

## 5. 订单支付与状态一致性

### 5.1 订单状态机

```
                    ┌─────────────┐
                    │  待支付(0)   │────超时扫描────→┌─────────────┐
                    └──────┬──────┘                 │  已超时(4)  │
                           │支付成功                └─────────────┘
                           ▼
                    ┌─────────────┐
                    │  已支付(1)   │
                    └──────┬──────┘
                           │退款
                           ▼
                    ┌─────────────┐
                    │  已退款(3)   │
                    └─────────────┘

          ┌─────────────┐
          │  已取消(2)   │←──用户主动取消
          └─────────────┘
```

### 5.2 支付防重

使用 Redis 分布式锁防止重复支付：

```java
String lockKey = "payment:lock:" + orderNo;
Boolean lock = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "1", 1, TimeUnit.MINUTES);
if (!Boolean.TRUE.equals(lock)) {
    throw new Exception("订单正在支付中，禁止重复支付");
}
```

### 5.3 超时订单补偿

定时任务每60秒扫描超时应支付订单：

1. 获取分布式锁（防止并发处理）
2. 更新订单状态为"已超时"
3. 发送MQ消息通知秒杀服务回滚Redis库存
4. 释放分布式锁

---

## 6. 架构流程图

### 6.1 秒杀下单分布式事务流程

```
用户请求 POST /api/seckill/seckill/transactional
         │
         ▼
┌────────────────────────────────────────────┐
│ 1. 幂等性检查（Redis SETNX 5分钟锁）          │
└────────────────────┬───────────────────────┘
                     │ 通过
                     ▼
┌────────────────────────────────────────────┐
│ 2. TCC事务开始                              │
│    globalTxId = "SECKILL_ORDER_" + orderNo │
│    保存到Redis（TTL=30分钟）                 │
└────────────────────┬───────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────┐
│ 3. TCC Try阶段：库存预扣                     │
│    Lua脚本：原子扣Redis库存 + 检查限购        │
│    失败 → TCC Cancel → 返回错误              │
└────────────────────┬───────────────────────┘
                     │ 成功（remaining >= 0）
                     ▼
┌────────────────────────────────────────────┐
│ 4. 创建秒杀订单（状态=排队中）               │
└────────────────────┬───────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────┐
│ 5. 可靠消息发送                             │
│    - 保存半消息到Redis                      │
│    - 异步发送Kafka消息                     │
│    失败 → TCC Cancel → 回滚库存             │
└────────────────────┬───────────────────────┘
                     │ 消息发送成功
                     ▼
┌────────────────────────────────────────────┐
│ 6. TCC Confirm阶段（更新秒杀订单为成功）     │
└────────────────────┬───────────────────────┘
                     │
                     ▼
              返回秒杀成功响应
                     │
                     ▼
         ┌────────────────────────┐
         │ Kafka消费（异步）       │
         │ order-service          │
         │ 1.幂等检查             │
         │ 2.创建正式订单（待支付） │
         │ 3.手动ACK              │
         └────────────────────────┘
```

### 6.2 订单支付流程

```
用户请求 POST /api/orders/{orderNo}/pay
         │
         ▼
┌────────────────────────────────────────────┐
│ 1. 获取Redis分布式锁（防止重复支付）          │
└────────────────────┬───────────────────────┘
                     │ 获取锁成功
                     ▼
┌────────────────────────────────────────────┐
│ 2. 检查订单状态（必须为"待支付"）             │
└────────────────────┬───────────────────────┘
                     │ 状态正确
                     ▼
┌────────────────────────────────────────────┐
│ 3. 检查订单是否超时（超过15分钟）             │
│    超时 → 标记为"已超时" → 释放锁           │
└────────────────────┬───────────────────────┘
                     │ 未超时
                     ▼
┌────────────────────────────────────────────┐
│ 4. 调用支付渠道（模拟）                      │
│    成功 → 更新订单为"已支付"                 │
└────────────────────┬───────────────────────┘
                     │
                     ▼
              释放Redis锁
              返回支付结果
```

---

## 7. 关键代码说明

### 7.1 TCC事务协调器

路径：`seckill-service/.../transaction/TccTransactionCoordinator.java`

核心方法：

```java
// 发起事务
String globalTxId = beginTransaction(bizType, bizNo, userId, activityId, productId, quantity);

// Try阶段：预扣库存
boolean trySuccess = tryPhase(globalTxId, activityId, productId, quantity, limitPerUser);

// Confirm阶段：确认
confirmPhase(globalTxId);

// Cancel阶段：回滚
cancelPhase(globalTxId, "库存不足");
```

### 7.2 分布式事务秒杀服务

路径：`seckill-service/.../transaction/TransactionalSeckillService.java`

完整流程：

```java
public SeckillResponse seckillWithTransaction(Long userId, SeckillRequest request) {
    // 1. 幂等性检查
    idempotencyService.tryAcquire(userId, activityId);

    // 2. 验证活动、商品状态
    SeckillActivity activity = activityMapper.findById(activityId);

    // 3. 发起TCC事务
    String globalTxId = tccCoordinator.beginTransaction("SECKILL_ORDER", orderNo, ...);

    // 4. TCC Try：预扣库存
    boolean trySuccess = tccCoordinator.tryPhase(globalTxId, activityId, productId, quantity, limit);

    // 5. 创建秒杀订单（本地）
    orderMapper.insert(seckillOrder);

    // 6. 可靠消息发送
    reliableMessageService.sendTransactionalMessage(globalTxId, orderMessage);

    // 7. TCC Confirm
    tccCoordinator.confirmPhase(globalTxId);

    // 8. 更新订单状态
    orderMapper.updateStatusToSuccess(seckillOrder.getId(), 1, orderNo);
}
```

### 7.3 Redis Lua原子扣库存

路径：`seckill-service/.../resources/lua/decrease_stock.lua`

```lua
-- 检查用户购买限制
local user_bought = redis.call('GET', user_key)
if user_bought and tonumber(user_bought) >= limit then
    return -1  -- 超限
end

-- 检查库存
local stock = redis.call('GET', stock_key)
if not stock then return -2 end
if tonumber(stock) < quantity then
    return -3  -- 库存不足
end

-- 扣减库存
redis.call('DECRBY', stock_key, quantity)
redis.call('INCR', user_key)

return tonumber(stock) - quantity
```

---

## 8. API接口

### 8.1 分布式事务秒杀下单

```
POST /api/seckill/seckill/transactional
```

**请求参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID（Query） |
| request | Body | 秒杀请求 |

**请求体：**

```json
{
  "activityId": 1,
  "productId": 1,
  "quantity": 1
}
```

**响应：**

```json
{
  "code": 200,
  "message": "秒杀成功（分布式事务）",
  "data": {
    "orderNo": "SK202601011500001200345678",
    "productName": "秒杀商品[1]",
    "quantity": 1,
    "totalAmount": 5999.00,
    "payExpireTime": "2026-01-01T15:15:00"
  }
}
```

### 8.2 TCC事务查询

```
GET /api/seckill/transaction/{globalTxId}
```

### 8.3 TCC事务手动回滚

```
POST /api/seckill/transaction/{globalTxId}/cancel
```

### 8.4 订单支付

```
POST /api/orders/{orderNo}/pay?payToken=xxx
```

### 8.5 订单取消

```
POST /api/orders/{orderNo}/cancel?userId=xxx
```

### 8.6 订单退款

```
POST /api/orders/{orderNo}/refund?reason=xxx
```

### 8.7 订单状态查询

```
GET /api/orders/{orderNo}/status
```
