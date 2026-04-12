# Flash Sale System

A distributed flash sale (seckill) e-commerce system built with microservices architecture.

## System Architecture

### Architecture Overview

The system follows a microservices + distributed middleware architecture, with core business logic split into the following services:

- **User Service** (`user-service`): User registration, login, and user profile management
- **Product Service**: Product information and flash sale activity management
- **Stock Service**: Inventory management and concurrent stock deduction control
- **Order Service**: Order creation, queries, and order status transitions
- **Shared Components**: API Gateway, Service Registry, Config Center, Cache, Message Queue, etc.

### Architecture Diagram

```
Client → API Gateway →
  - User requests → User Service → MySQL(user) / Redis
  - Product browse requests → Product Service → MySQL(product) / Redis
  - Order/flash-sale requests → Gateway Rate Limiting → Flash Sale Entry →
    - Write to Message Queue (MQ) → Order Service async consumption creates order
    - Order Service calls Stock Service → Deduct stock → MySQL(stock) / Redis sync
```

## RESTful API Design

### User Service

- `POST /api/users/register`: User registration
  - Request body: `{ "username": "string", "password": "string", "phone": "string" }`
  - Response: `{ "id": 1, "username": "string" }`

- `POST /api/users/login`: User login
  - Request body: `{ "username": "string", "password": "string" }`
  - Response: `{ "token": "jwt-token", "userId": 1, "username": "string" }`

- `GET /api/users/me`: Get current user info
  - Header: `Authorization: Bearer <token>`

### Product Service

- `GET /api/products`: Paginated product list (supports `page`, `size` params)
- `GET /api/products/{productId}`: Get product details
- `GET /api/products/{productId}/seckill`: Get flash sale info for a product

### Stock Service

- `GET /api/stocks/{productId}`: Get product stock
- `POST /api/stocks/freeze`: Pre-freeze stock (prevent overselling)
  - Request body:
    ```json
    {
      "productId": 1,
      "userId": 1,
      "quantity": 1,
      "requestId": "uuid"
    }
    ```
- `POST /api/stocks/confirm`: Confirm stock deduction
  - Request body: `{ "requestId": "uuid" }`
- `POST /api/stocks/cancel`: Cancel pre-frozen stock
  - Request body: `{ "requestId": "uuid" }`

### Order Service

- `POST /api/orders`: Create order
  - Request body:
    ```json
    {
      "userId": 1,
      "productId": 1,
      "quantity": 1,
      "addressId": 100
    }
    ```
  - Flow: Validate user & product → Call stock service to pre-freeze → Write to order table (status: pending payment) → Send order creation message to MQ

- `GET /api/orders/{orderId}`: Get order details
- `GET /api/orders?userId=1&page=1&size=10`: Get user's order list

### Flash Sale (Seckill) API

- `GET /api/seckill/activities`: List all flash sale activities
- `GET /api/seckill/activities/active`: List active activities
- `GET /api/seckill/activities/{id}`: Get activity details
- `GET /api/seckill/activities/{activityId}/products`: Get products in an activity
- `POST /api/seckill/activities/{activityId}/products/{productId}/init-stock`: Initialize stock for a product in an activity
- `POST /api/seckill/activities/init-all-stock`: Initialize stock for all active activities
- `POST /api/seckill/order`: Submit flash sale order
  - Params: `userId`, `activityId`, `productId`
- `GET /api/seckill/order/{orderNo}`: Get flash sale order details
- `GET /api/seckill/orders/user/{userId}`: Get user's flash sale orders
- `POST /api/seckill/order/{orderNo}/cancel`: Cancel a flash sale order

## Database Schema

### 1. User Table `t_user`

- `id` (BIGINT, PK)
- `username` (VARCHAR, unique)
- `password_hash` (VARCHAR)
- `phone` (VARCHAR, unique)
- `status` (TINYINT, 0=disabled, 1=active)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### 2. Product Table `t_product`

- `id` (BIGINT, PK)
- `name` (VARCHAR)
- `description` (TEXT)
- `price` (DECIMAL(10,2))
- `status` (TINYINT, 0=offline, 1=online)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### 3. Stock Table `t_stock`

- `id` (BIGINT, PK)
- `product_id` (BIGINT, FK → `t_product.id`)
- `total_stock` (INT)
- `available_stock` (INT)
- `frozen_stock` (INT)
- `version` (INT, optimistic locking)
- `updated_at` (DATETIME)

### 4. Order Table `t_order`

- `id` (BIGINT, PK)
- `order_no` (VARCHAR, unique)
- `user_id` (BIGINT, FK → `t_user.id`)
- `product_id` (BIGINT, FK → `t_product.id`)
- `quantity` (INT)
- `amount` (DECIMAL(10,2))
- `status` (TINYINT, 0=cancelled, 1=pending payment, 2=paid, 3=completed)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### 5. Flash Sale Activity Table `t_seckill_activity`

- `id` (BIGINT, PK)
- `name` (VARCHAR)
- `start_time` (DATETIME)
- `end_time` (DATETIME)
- `status` (TINYINT, 0=disabled, 1=enabled)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### 6. Flash Sale Product Table `t_seckill_product`

- `id` (BIGINT, PK)
- `activity_id` (BIGINT, FK → `t_seckill_activity.id`)
- `product_id` (BIGINT, FK → `t_product.id`)
- `seckill_price` (DECIMAL(10,2))
- `stock` (INT)
- `limit_per_user` (INT, max per user)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### 7. Flash Sale Order Table `t_seckill_order`

- `id` (BIGINT, PK)
- `user_id` (BIGINT, FK → `t_user.id`)
- `activity_id` (BIGINT, FK → `t_seckill_activity.id`)
- `product_id` (BIGINT, FK → `t_product.id`)
- `order_no` (VARCHAR, unique)
- `status` (TINYINT, 0=pending, 1=success, 2=failed)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### ER Relationships

- One user can have multiple orders: `t_user (1) —— (N) t_order`
- One product has one stock record: `t_product (1) —— (1) t_stock`
- One order references one product (single-product orders): `t_order (N) —— (1) t_product`
- One activity has multiple flash sale products: `t_seckill_activity (1) —— (N) t_seckill_product`

## Technology Stack

- **Language**: Java 17
- **Backend Framework**: Spring Boot 3.x + Spring Cloud / Spring Cloud Alibaba
- **Persistence**: MyBatis / MyBatis-Plus
- **Database**: MySQL 8.0
- **Cache**: Redis (hot products, stock, sessions)
- **Message Queue**: RabbitMQ (flash sale削峰, async order processing)
- **Registry & Config**: Nacos / Eureka + Nacos Config / Spring Cloud Config
- **Gateway**: Spring Cloud Gateway or Nginx + Spring Cloud

## Environment Setup

1. Initialize the project with Git:
   ```bash
   git init
   git add .
   git commit -m "init flash-sale-system"
   ```

2. Create a Spring Boot project using Spring Initializr:
   - Dependencies: Spring Web, MyBatis, MySQL Driver, Validation, Lombok, etc.

3. Create the tables in the database according to the schema above.

4. Start with the user service "register + login" APIs to verify the database and framework.

---

## High Concurrency Read / Load Balancing (Part 2)

- **Container Environment**: The root directory provides `docker-compose.yml` and `user-service/Dockerfile` to start MySQL, two backend instances (8081/8082), and Nginx (80) with one command. Database tables are auto-initialized by `docker/init.sql`.
- **Load Balancing**: Nginx config at `nginx/nginx.conf`, default round-robin; can switch to least connections, ip_hash, weighted round-robin, etc. See `docs/负载均衡与压测说明.md`.
- **Load Testing & Verification**: Backend provides `GET /api/users/info` returning the current instance port; use JMeter to stress test `http://localhost/api/users/info`, observe responses and instance logs to verify request distribution.

---

## High Concurrency Read / Dynamic/Static Separation (Part 3)

- **Static Pages**: Located at `front-end/index.html`, includes product list, flash sale buttons, etc. (example page, backend API not yet connected).
- **Nginx Dynamic/Static Separation**: In `nginx/nginx.conf`:
  - `location /`: Static resources (HTML/CSS/JS/images) served directly, cached for 1 year (`expires 1y`).
  - `location /api/`: Dynamic requests forwarded to the backend load balancer group (8081/8082).
- **Container Mount**: Nginx container in `docker-compose.yml` mounts the `front-end/` directory to `/usr/share/nginx/html`.
- **Verification**: Access `http://localhost/` returns static page directly (no backend); access `http://localhost/api/users/info` to call backend service.

---

## High Concurrency Read / Distributed Cache Redis (Part 4)

- **Redis Dependency**: Added to `user-service/pom.xml` (`spring-boot-starter-data-redis`, `commons-pool2`, `spring-boot-starter-aop`).
- **Redis Config**: `application.yml` configures Redis connection (host/port/lettuce pool), env vars `REDIS_HOST`, `REDIS_PORT`.
- **Cache Services**:
  - `CacheService`: Provides `set` / `get` / `delete` / `tryLock` / `unlock` generic cache operations.
  - `RedisConfig`: Configures Jackson2Json serialization (supports Java object storage).
- **Cache Applications**:
  - `UserService.login()`: On login, check cache first (key `user:username:{username}`), fallback to DB and write to cache (5 min TTL).
  - `UserService.findById()`: Added ID-based query, reads cache first (key `user:id:{id}`).
- **Container Environment**: `docker-compose.yml` adds Redis service (port 6379), two user-service instances connect via `REDIS_HOST=redis`.
- **Verification**: After login, subsequent logins return from Redis cache; check cache keys via Redis CLI `user:username:*`.

---

## Flash Sale (Seckill) High Concurrency Solution

### Overall Flow

1. **Rate Limiting**: Use Redis + Lua script for atomic rate limiting at the gateway/controller level
2. **Stock Pre-warming**: Before flash sale starts, load stock into Redis
3. **Order Deduplication**: Redis SETNX ensures one order per user per product
4. **Atomic Stock Deduction**: Redis Lua script guarantees atomic stock deduction (no overselling)
5. **Async Order Creation**: RabbitMQ decouples stock deduction from order creation
6. **Stock Sync**: Redis → MySQL async sync via scheduled tasks or message queue

### Key Components

- `LuaStockService`: Executes Lua scripts for atomic stock operations
- `SeckillService`: Core flash sale business logic
- `SeckillOrderProducer`: Sends order messages to RabbitMQ
- `SeckillOrderConsumer`: Consumes order messages and creates orders
- `RateLimiterConfig`: Redis-based rate limiting with Guava

### Lua Scripts

Located in `user-service/src/main/resources/lua/`:

- `decrease_stock.lua`: Atomic stock deduction with duplicate check
- `increase_stock.lua`: Atomic stock increase (for rollback)

---

## Project Structure

```
flash-sale-system/
├── doc/                                    # Complete documentation (31 files)
├── docker/                                 # Database initialization scripts
│   ├── init.sql                           # Main database initialization
│   ├── mysql/
│   │   ├── master.cnf                     # MySQL Master config
│   │   ├── slave.cnf                      # MySQL Slave config
│   │   └── init-slave.sql                 # Slave replication initialization
├── nginx/
│   └── nginx.conf                         # Nginx configuration
├── front-end/
│   ├── index.html                         # Product list page
│   ├── sale.html                          # Flash sale page
│   ├── login.html                         # Login page
│   ├── register.html                      # Registration page
│   ├── orders.html                        # Order list page
│   └── admin.html                         # Admin page
├── user-service/                           # Main backend service
│   ├── src/main/
│   │   ├── java/.../
│   │   │   ├── config/                    # DataSource, Redis, MQ, RateLimiting configs
│   │   │   ├── consumer/                  # MQ consumers (flash sale order consumer)
│   │   │   ├── controller/               # REST controllers
│   │   │   ├── domain/                   # Entity classes
│   │   │   ├── dto/                      # Data transfer objects
│   │   │   ├── event/                    # Event classes
│   │   │   ├── mapper/                   # MyBatis mappers
│   │   │   ├── producer/                 # MQ producers
│   │   │   ├── service/                  # Business logic
│   │   │   └── web/                      # REST controllers
│   │   └── resources/
│   │       ├── application.yml           # Spring Boot configuration
│   │       └── lua/                       # Redis Lua scripts
│   └── Dockerfile
├── docker-compose.yml                      # Docker Compose orchestration
├── docker-start.sh                         # One-click startup script
├── Dockerfile.simple                       # Simplified Dockerfile
└── README.md                              # This file
```

---

## Documentation Index

Complete project documentation is in the `doc/` directory:

```
doc/
├── README.md                              # Documentation index
├── 01-System Overview and Architecture.md
├── 02-Database Design.md
├── 03-Functional Module Design.md
├── 04-High Concurrency Solutions.md
├── 05-Communication and Extension Mechanisms.md
├── 06-API Interface Design.md
├── 07-Page Prototype Design.md
├── 08-Docker Deployment Guide.md
├── 09-Docker Quick Start.md
├── 10-Kubernetes Cluster Deployment.md
├── 11-Docker-Swarm Cluster Deployment.md
├── 12-Cluster Deployment Comparison.md
├── 读写分离/                              # MySQL master-slave replication
│   └── README.md
├── 消息队列与秒杀/                         # RabbitMQ/Kafka async flash sales
│   ├── README.md
│   ├── 01-Kafka Async Flash Sale.md
│   └── 02-Message Queue Comparison.md
├── 容器与负载均衡部署/                      # Docker, Nginx, load testing
│   ├── README.md
│   ├── 01-Container Environment Deployment.md
│   ├── 02-Load Balancing Config.md
│   ├── 03-Dynamic Static Separation Config.md
│   ├── 04-Distributed Cache.md
│   └── 05-JMeter Load Testing.md
├── 测试工具/                               # API test scripts
│   ├── API Test Script.sh
│   ├── Password Generation Script.sh
│   ├── Build Check Script.sh
│   └── Shiro API Test Collection.postman_collection.json
└── 问题修复/                               # Troubleshooting
    ├── 01-Build Issue Fixes.md
    ├── 02-Docker Image Pull Failures.md
    └── 03-All Issue Fixes Summary.md
```

### Reading Path Suggestions

**Quick Start (30 minutes)**
1. [doc/09-Docker Quick Start.md](doc/09-Docker快速开始.md) → Start system in 5 minutes
2. [doc/06-API Interface Design.md](doc/06-API接口设计.md) → Understand API calls

**Deep Learning (3-5 hours)**
1. [doc/01-System Overview and Architecture.md](doc/01-系统概述与架构设计.md) → System overview
2. [doc/02-Database Design.md](doc/02-数据库设计.md) → Data model
3. [doc/04-High Concurrency Solutions.md](doc/04-高并发解决方案.md) → Concurrency core
4. [doc/03-Functional Module Design.md](doc/03-功能模块设计.md) → Business logic

**Production Deployment**
1. [doc/12-Cluster Deployment Comparison.md](doc/12-集群部署方案对比.md) → Choose approach
2. [doc/10-Kubernetes Cluster Deployment.md](doc/10-Kubernetes集群部署.md) or [doc/11-Docker-Swarm Cluster Deployment.md](doc/11-Docker-Swarm集群部署.md) → Execute deployment
