flash-sale-system
=================

商品库存与秒杀系统设计

## 一、系统架构设计

### 1. 整体架构说明

本系统采用微服务 + 分布式中间件的架构思路，将核心业务拆分为以下服务：

- 用户服务（user-service）：负责用户注册、登录与用户信息管理
- 商品服务（product-service）：负责商品信息、秒杀活动信息管理
- 库存服务（stock-service）：负责商品库存维护及并发扣减控制
- 订单服务（order-service）：负责订单创建、查询与订单状态流转
- 公共组件：API 网关、注册中心、配置中心、缓存、消息队列等

### 2. 文字版架构图（示意）

- 客户端 → API 网关 →
  - 用户相关请求 → 用户服务 → MySQL(user) / Redis
  - 商品浏览请求 → 商品服务 → MySQL(product) / Redis
  - 下单/秒杀请求 → 网关限流 → 秒杀入口（商品服务或独立秒杀服务）→
    - 写入消息队列（MQ）→ 订单服务异步消费创建订单
    - 订单服务调用库存服务 → 扣减库存 → MySQL(stock) / Redis 同步

## 二、各服务 RESTful API 设计

### 1. 用户服务（user-service）

- POST `/api/users/register`：用户注册
  - 请求体：`{ "username": "string", "password": "string", "phone": "string" }`
  - 响应：`{ "id": 1, "username": "string" }`

- POST `/api/users/login`：用户登录
  - 请求体：`{ "username": "string", "password": "string" }`
  - 响应（示例）：`{ "token": "jwt-token", "userId": 1, "username": "string" }`

- GET `/api/users/me`：获取当前用户信息
  - Header：`Authorization: Bearer <token>`

### 2. 商品服务（product-service）

- GET `/api/products`：分页查询商品列表（支持 `page`、`size` 参数）
- GET `/api/products/{productId}`：查询商品详情
- GET `/api/products/{productId}/seckill`：查询指定商品的秒杀信息

### 3. 库存服务（stock-service）

- GET `/api/stocks/{productId}`：查询商品库存
- POST `/api/stocks/freeze`：预冻结库存（防止超卖）
  - 请求体：
    ```json
    {
      "productId": 1,
      "userId": 1,
      "quantity": 1,
      "requestId": "uuid"
    }
    ```
- POST `/api/stocks/confirm`：确认扣减库存
  - 请求体：`{ "requestId": "uuid" }`
- POST `/api/stocks/cancel`：取消预冻结库存
  - 请求体：`{ "requestId": "uuid" }`

### 4. 订单服务（order-service）

- POST `/api/orders`：创建订单
  - 请求体：
    ```json
    {
      "userId": 1,
      "productId": 1,
      "quantity": 1,
      "addressId": 100
    }
    ```
  - 处理流程：验证用户与商品 → 调用库存服务预冻结库存 → 写入订单表（状态：待支付）→ 发送订单创建消息到 MQ

- GET `/api/orders/{orderId}`：查询订单详情
- GET `/api/orders?userId=1&page=1&size=10`：查询用户订单列表

## 三、数据库 ER 图设计（文字说明）

系统包含四张核心业务表：用户表、商品表、库存表、订单表。

### 1. 用户表 `t_user`

- `id` (BIGINT, PK)
- `username` (VARCHAR, 唯一)
- `password_hash` (VARCHAR，加密后的密码)
- `phone` (VARCHAR, 唯一)
- `status` (TINYINT，0=禁用，1=正常)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### 2. 商品表 `t_product`

- `id` (BIGINT, PK)
- `name` (VARCHAR)
- `description` (TEXT)
- `price` (DECIMAL(10,2))
- `status` (TINYINT，0=下架，1=上架)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### 3. 库存表 `t_stock`

- `id` (BIGINT, PK)
- `product_id` (BIGINT, FK → `t_product.id`)
- `total_stock` (INT，总库存)
- `available_stock` (INT，可用库存)
- `frozen_stock` (INT，冻结库存)
- `version` (INT，用于乐观锁防止并发超卖)
- `updated_at` (DATETIME)

### 4. 订单表 `t_order`

- `id` (BIGINT, PK)
- `order_no` (VARCHAR, 唯一订单号)
- `user_id` (BIGINT, FK → `t_user.id`)
- `product_id` (BIGINT, FK → `t_product.id`)
- `quantity` (INT)
- `amount` (DECIMAL(10,2))
- `status` (TINYINT，0=已取消，1=待支付, 2=已支付, 3=已完成)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### 5. ER 关系说明

- 一个用户可以拥有多笔订单：`t_user (1) —— (N) t_order`
- 一个商品对应一条库存记录：`t_product (1) —— (1) t_stock`
- 一个订单对应一个商品（简化为单商品订单）：`t_order (N) —— (1) t_product`

## 四、技术栈选型说明

- 编程语言：Java 17
- 后端框架：Spring Boot 3.x + Spring Cloud / Spring Cloud Alibaba
- 持久层：MyBatis / MyBatis-Plus
- 数据库：MySQL 8.0
- 缓存：Redis（存放热点商品、库存、会话等）
- 消息队列：RocketMQ / RabbitMQ（秒杀削峰、订单异步处理）
- 注册与配置中心：Nacos / Eureka + Nacos Config / Spring Cloud Config
- 网关：Spring Cloud Gateway 或 Nginx + Spring Cloud

## 五、环境准备与基础功能

1. 使用 Git 初始化项目代码仓库：
   - `git init`
   - `git add .`
   - `git commit -m "init flash-sale-system"`

2. 使用 Spring Initializr 创建基础 Spring Boot 工程：
   - 选择依赖：Spring Web、MyBatis、MySQL Driver、Validation、Lombok 等

3. 在数据库中根据上文 ER 设计创建 `t_user`、`t_product`、`t_stock`、`t_order` 四张表。

4. 优先实现用户服务的“注册 + 登录”接口，验证数据库及基础框架是否搭建成功。

---

## 高并发读 / 负载均衡（第二部分）

- **容器环境**：根目录提供 `docker-compose.yml`、`user-service/Dockerfile`，可一键启动 MySQL、两个后端实例（8081/8082）、Nginx（80）。MySQL 表由 `docker/init.sql` 自动初始化。
- **负载均衡**：Nginx 配置见 `nginx/nginx.conf`，默认轮询；可切换为最少连接、ip_hash、加权轮询等，详见 `docs/负载均衡与压测说明.md`。
- **压测与验证**：后端提供 `GET /api/users/info` 返回当前实例端口；使用 JMeter 对 `http://localhost/api/users/info` 压测，观察响应与两实例日志即可验证请求是否被均分。

---

## 高并发读 / 动静分离（第三部分）

- **静态页面**：位于 `front-end/index.html`，包含商品列表、秒杀按钮等前端展示（示例页面，后端 API 尚未对接）。
- **Nginx 动静分离**：`nginx/nginx.conf` 中配置：
  - `location /`：静态资源（HTML/CSS/JS/图片）直接返回，缓存一年（`expires 1y`）。
  - `location /api/`：动态请求转发到后端负载均衡组（8081/8082）。
- **容器挂载**：`docker-compose.yml` 中 Nginx 容器将 `front-end/` 目录挂载到 `/usr/share/nginx/html`。
- **验证**：访问 `http://localhost/` 直接返回静态页面（不经过后端），访问 `http://localhost/api/users/info` 才会调用后端服务。

---

## 高并发读 / 分布式缓存 Redis（第四部分）

- **Redis 依赖**：已添加到 `user-service/pom.xml`（`spring-boot-starter-data-redis`、`commons-pool2`、`spring-boot-starter-aop`）。
- **Redis 配置**：`application.yml` 中配置 Redis 连接（host/port/lettuce pool），环境变量 `REDIS_HOST`、`REDIS_PORT`。
- **缓存服务**：
  - `CacheService`：提供 `set` / `get` / `delete` / `tryLock` / `unlock` 等通用缓存操作。
  - `RedisConfig`：配置 Jackson2Json 序列化（支持 Java 对象存储）。
- **缓存应用**：
  - `UserService.login()`：登录时先查缓存（键 `user:username:{username}`），缓存未命中再查数据库并写入缓存（5 分钟 TTL）。
  - `UserService.findById()`：新增根据 ID 查询用户方法，优先读缓存（键 `user:id:{id}`）。
- **容器环境**：`docker-compose.yml` 新增 Redis 服务（端口 6379），两个 user-service 实例通过环境变量 `REDIS_HOST=redis` 连接。
- **验证**：登录后再次登录应从 Redis 缓存返回；可通过 Redis CLI 查看缓存键 `user:username:*`。

---

## 六、文档索引

完整项目文档位于 `doc/` 目录，结构如下：

```
doc/
├── README.md                              # 文档索引
├── 01-系统概述与架构设计.md                # 项目介绍、技术栈、架构图
├── 02-数据库设计.md                        # 表结构、Redis数据结构
├── 03-功能模块设计.md                      # 秒杀流程、订单状态机
├── 04-高并发解决方案.md                    # 限流、缓存、异步、分布式锁
├── 05-通讯与扩展机制.md                    # MQ消息队列、事件驱动
├── 06-API接口设计.md                       # RESTful API、错误码
├── 07-页面原型设计.md                      # 前端页面、交互流程
├── 08-Docker部署指南.md                    # Docker详细部署
├── 09-Docker快速开始.md                    # 5分钟快速启动
├── 10-Kubernetes集群部署.md               # K8s集群部署
├── 11-Docker-Swarm集群部署.md              # Swarm集群部署
├── 12-集群部署方案对比.md                  # 部署方案选型
├── 读写分离/                               # MySQL主从复制
│   └── README.md
├── 消息队列与秒杀/                         # RabbitMQ/Kafka异步秒杀
│   ├── README.md
│   ├── 01-Kafka异步秒杀.md
│   └── 02-消息队列对比.md
├── 容器与负载均衡部署/                      # Docker、Nginx、性能测试
│   ├── README.md
│   ├── 01-容器环境部署.md
│   ├── 02-负载均衡配置.md
│   ├── 03-动静分离配置.md
│   ├── 04-分布式缓存.md
│   └── 05-JMeter压力测试.md
├── 测试工具/                               # API测试脚本
│   ├── API测试脚本.sh
│   ├── 密码生成脚本.sh
│   ├── 编译检查脚本.sh
│   └── Shiro接口测试集合.postman_collection.json
└── 问题修复/                               # 常见问题解决
    ├── 01-编译问题修复.md
    ├── 02-Docker镜像拉取失败.md
    └── 03-所有问题修复总结.md
```

### 阅读路径建议

**快速开始（30分钟）**
1. [doc/09-Docker快速开始.md](doc/09-Docker快速开始.md) → 5分钟启动系统
2. [doc/06-API接口设计.md](doc/06-API接口设计.md) → 了解API调用

**深入学习（3-5小时）**
1. [doc/01-系统概述与架构设计.md](doc/01-系统概述与架构设计.md) → 系统全貌
2. [doc/02-数据库设计.md](doc/02-数据库设计.md) → 数据模型
3. [doc/04-高并发解决方案.md](doc/04-高并发解决方案.md) → 并发处理核心
4. [doc/03-功能模块设计.md](doc/03-功能模块设计.md) → 业务逻辑

**生产部署**
1. [doc/12-集群部署方案对比.md](doc/12-集群部署方案对比.md) → 选择方案
2. [doc/10-Kubernetes集群部署.md](doc/10-Kubernetes集群部署.md) 或 [doc/11-Docker-Swarm集群部署.md](doc/11-Docker-Swarm集群部署.md) → 执行部署

---

## 七、项目结构总览

```
flash-sale-system/
├── doc/                                    # 完整文档（31个文件）
├── docker/                                 # 数据库初始化脚本
│   ├── init.sql                           # 主数据库初始化
│   ├── mysql/
│   │   ├── master.cnf                     # MySQL Master配置
│   │   ├── slave.cnf                      # MySQL Slave配置
│   │   └── init-slave.sql                 # 从库复制初始化
├── nginx/
│   └── nginx.conf                         # Nginx配置
├── front-end/
│   └── sale.html                          # 秒杀前端页面
├── k8s/                                    # Kubernetes配置
│   ├── namespace.yaml                      # 命名空间
│   ├── app-deployment.yaml                 # 应用Deployment
│   ├── mysql-statefulset.yaml              # MySQL StatefulSet
│   ├── redis-deployment.yaml              # Redis Deployment
│   ├── hpa.yaml                           # 自动扩缩容
│   ├── ingress.yaml                       # Ingress
│   └── deploy.sh                          # 部署脚本
├── swarm/                                  # Docker Swarm配置
│   ├── docker-compose.swarm.yml           # Swarm编排文件
│   ├── swarm-init.sh                      # 集群初始化
│   └── swarm-deploy.sh                    # 部署脚本
├── jmeter/
│   └── seckill-test.jmx                  # JMeter压测脚本
├── user-service/                           # 用户服务（主服务）
│   ├── Dockerfile                          # 多阶段Docker构建
│   ├── pom.xml                            # Maven依赖
│   └── src/main/
│       ├── java/.../
│       │   ├── config/                    # 数据源、Redis、MQ、限流配置
│       │   ├── datasource/                # 读写分离（AOP+DynamicDS）
│       │   ├── service/                   # 业务逻辑（秒杀、库存、搜索）
│       │   ├── web/                       # REST控制器
│       │   └── mapper/                   # MyBatis Mapper
│       └── resources/
│           ├── application.yml           # Spring Boot配置
│           ├── lua/                       # Redis Lua脚本
│           └── static/register.html      # 登录页面
├── docker-compose.yml                      # Docker Compose编排
├── docker-start.sh                         # 一键启动脚本
├── Dockerfile.simple                       # 简化版Dockerfile
└── README.md                              # 本文件
```