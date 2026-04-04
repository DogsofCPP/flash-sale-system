# API 接口设计

## 1. 接口规范

### 1.1 基础信息

- **统一网关入口**：`http://localhost:8080`（经 Gateway 路由）
- **微服务直连**：`http://localhost:端口/api`
- **数据格式**：JSON
- **字符编码**：UTF-8

### 1.2 统一响应封装

所有接口统一返回 `Result<T>` 结构：

```json
{
    "code": 200,
    "message": "success",
    "data": { ... }
}
```

```java
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

### 1.3 业务错误码

| 错误码 | 含义 | HTTP 状态码 |
|--------|------|------------|
| 200 | 操作成功 | 200 |
| 400 | 参数错误 | 400 |
| 401 | 未认证 | 401 |
| 403 | 无权限 | 403 |
| 404 | 资源不存在 | 404 |
| 429 | 请求过于频繁 | 429 |
| 500 | 服务器内部错误 | 500 |
| 1001 | 秒杀活动不存在 | 200 |
| 1002 | 秒杀活动尚未开始 | 200 |
| 1003 | 秒杀活动已结束 | 200 |
| 1004 | 库存不足 | 200 |
| 1005 | 购买数量超出限制 | 200 |
| 1006 | 已购买过该商品 | 200 |
| 1007 | 订单创建失败 | 200 |
| 1008 | 订单不存在 | 200 |
| 1009 | 订单已超时 | 200 |
| 1010 | 订单状态不允许此操作 | 200 |

## 2. 用户接口（user-service:8081）

### 2.1 用户注册

```
POST /api/users/register
```

**请求参数：**

```json
{
    "username": "flashuser",
    "password": "Aa123456",
    "phone": "13900001111"
}
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "注册成功",
    "data": {
        "id": 1,
        "username": "flashuser",
        "phone": "13900001111",
        "status": 1
    }
}
```

### 2.2 用户登录

```
POST /api/users/login
```

**请求参数：**

```json
{
    "username": "flashuser",
    "password": "Aa123456"
}
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "id": 1,
        "username": "flashuser",
        "phone": "13900001111",
        "status": 1
    }
}
```

### 2.3 获取用户详情

```
GET /api/users/{id}
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "username": "flashuser",
        "phone": "13900001111",
        "status": 1,
        "createdAt": "2024-01-01T12:00:00"
    }
}
```

### 2.4 查询用户列表

```
GET /api/users?page=1&size=20
```

### 2.5 健康检查

```
GET /api/users/info
```

## 3. 商品接口（product-service:8083）

### 3.1 商品列表

```
GET /api/products
GET /api/products?page=1&size=10&status=1
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "name": "iPhone 15 Pro",
            "description": "苹果旗舰手机",
            "price": 5999.00,
            "stock": 100,
            "status": 1
        }
    ]
}
```

### 3.2 商品详情

```
GET /api/products/{id}
```

### 3.3 商品搜索（MySQL LIKE）

```
GET /api/products/search?keyword=手机&page=1&size=10
```

### 3.4 商品搜索（Elasticsearch）

```
GET /api/search?q=手机&page=0&size=10
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "total": 25,
        "items": [
            {
                "id": 1,
                "name": "iPhone 15 Pro",
                "price": 5999.00,
                "score": 0.95
            }
        ]
    }
}
```

## 4. 秒杀接口（seckill-service:8086）

### 4.1 获取秒杀活动列表

```
GET /api/seckill/activities
GET /api/seckill/activities?status=1
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "name": "新年特惠秒杀",
            "startTime": "2024-01-01T10:00:00",
            "endTime": "2024-01-01T12:00:00",
            "status": 1,
            "products": [
                {
                    "productId": 100,
                    "productName": "iPhone 15 Pro",
                    "seckillPrice": 99.00,
                    "originalPrice": 5999.00,
                    "stock": 50,
                    "soldCount": 20,
                    "limitPerUser": 1
                }
            ]
        }
    ]
}
```

### 4.2 获取进行中的活动

```
GET /api/seckill/activities/active
```

### 4.3 获取活动详情

```
GET /api/seckill/activities/{activityId}
```

### 4.4 获取活动商品列表

```
GET /api/seckill/activities/{activityId}/products
```

### 4.5 秒杀下单（核心接口）

```
POST /api/seckill/seckill
Header: X-User-Id: {userId}
```

**请求参数：**

```json
{
    "activityId": 1,
    "productId": 100,
    "quantity": 1
}
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "秒杀成功，订单已创建",
    "data": {
        "orderNo": "1720000000001ABC12345",
        "productName": "iPhone 15 Pro",
        "seckillPrice": 99.00,
        "quantity": 1,
        "totalAmount": 99.00,
        "expireTime": "2024-01-01T12:15:00",
        "status": 0
    }
}
```

**失败响应示例：**

```json
// 库存不足
{
    "code": 1004,
    "message": "库存不足",
    "data": null
}

// 已达限购上限
{
    "code": 1005,
    "message": "该商品每人限购1件",
    "data": null
}

// 活动未开始
{
    "code": 1002,
    "message": "秒杀活动尚未开始",
    "data": null
}
```

### 4.6 预热库存（初始化 Redis）

```
POST /api/seckill/activities/{activityId}/products/{productId}/init-stock
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "库存已初始化到 Redis",
    "data": {
        "activityId": 1,
        "productId": 100,
        "stock": 50
    }
}
```

### 4.7 批量预热所有活动库存

```
POST /api/seckill/activities/init-all-stock
```

### 4.8 查询用户秒杀订单

```
GET /api/seckill/orders/user/{userId}
GET /api/seckill/orders/user/{userId}?status=0
```

## 5. 订单接口（order-service:8085）

### 5.1 订单详情

```
GET /api/orders/{orderNo}
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "orderNo": "1720000000001ABC12345",
        "userId": 1,
        "productId": 100,
        "productName": "iPhone 15 Pro",
        "productPrice": 99.00,
        "quantity": 1,
        "totalAmount": 99.00,
        "status": 0,
        "statusName": "待支付",
        "createdAt": "2024-01-01T12:00:00",
        "expireTime": "2024-01-01T12:15:00"
    }
}
```

### 5.2 用户订单列表

```
GET /api/orders?userId=1
GET /api/orders?userId=1&status=0
GET /api/orders?userId=1&page=1&size=10
```

### 5.3 取消订单

```
POST /api/orders/{orderNo}/cancel
```

**成功响应（200）：**

```json
{
    "code": 200,
    "message": "订单已取消",
    "data": null
}
```

**失败响应（订单不存在或状态不允许）：**

```json
{
    "code": 1008,
    "message": "订单不存在",
    "data": null
}
```

## 6. 数据源路由测试接口（user-service:8081）

### 6.1 测试数据源连接

```
GET /api/test/datasource
```

### 6.2 强制使用主库

```
GET /api/test/master
```

### 6.3 强制使用从库

```
GET /api/test/slave
```

### 6.4 验证读写分离效果

```
GET /api/test/read-write
```

返回结果中包含主从库查询的耗时对比，验证路由是否正确。

## 7. 接口性能要求

| 接口 | 目标 P99 延迟 | 说明 |
|------|-------------|------|
| 秒杀下单 POST /seckill/seckill | < 50ms | Redis Lua 路径 |
| 秒杀活动列表 GET /activities | < 100ms | Redis 缓存 |
| 订单详情 GET /orders/{orderNo} | < 50ms | Redis 缓存 |
| 商品搜索 GET /search | < 200ms | Elasticsearch |
