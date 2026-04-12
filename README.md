flash-sale-system
=================

A high-performance flash sale (seckill) e-commerce system based on microservices architecture.

## Architecture Overview

### Microservices

The system consists of the following microservices:

| Service | Description |
|---------|-------------|
| `user-service` | User registration, login, and profile management |
| `product-service` | Product catalog, product details, and inventory management |
| `order-service` | Order creation, order queries, and order state transitions |
| `seckill-service` | Flash sale activities, inventory pre-freeze, and seckill logic |
| `gateway` | API Gateway for routing and authentication |
| `common` | Shared library containing common models, utilities, and configurations |

### Architecture Diagram

```
Client -> Gateway
              |
              +-- user-service   -> MySQL(user) / Redis
              +-- product-service -> MySQL(product) / Redis
              +-- order-service   -> MySQL(order) / Redis
              +-- seckill-service -> MQ -> order-service -> stock-service
```

### Infrastructure

- **Database**: MySQL 8.0
- **Cache**: Redis (hot product cache, session store, inventory cache)
- **Message Queue**: RabbitMQ (order async processing, traffic peak shaving)
- **Gateway**: Spring Cloud Gateway
- **Registry**: Nacos (service discovery and configuration)

## Tech Stack

- Java 17
- Spring Boot 3.x + Spring Cloud Alibaba
- MyBatis / MyBatis-Plus
- MySQL 8.0
- Redis
- RabbitMQ
- Docker & Docker Compose
- Nginx (static resources and load balancing)
- Nacos

## Project Structure

```
flash-sale-system/
├── common/                          # Shared library (common models, utils)
├── doc/                             # Documentation
├── docker/                          # Database initialization scripts
│   └── init.sql                     # MySQL schema initialization
├── front-end/                       # Static frontend pages
│   ├── index.html                   # Product listing page
│   ├── sale.html                    # Flash sale page
│   ├── login.html                   # Login page
│   ├── register.html                # Registration page
│   ├── orders.html                  # Order list page
│   └── admin.html                   # Admin page
├── gateway/                         # API Gateway service
├── jmeter/                          # JMeter performance test scripts
├── k8s/                             # Kubernetes deployment configs
│   ├── namespace.yaml
│   ├── app-deployment.yaml
│   ├── mysql-statefulset.yaml
│   ├── redis-deployment.yaml
│   ├── hpa.yaml
│   ├── ingress.yaml
│   └── deploy.sh
├── nginx/
│   └── nginx.conf                   # Nginx load balancing config
├── order-service/                   # Order microservice
├── product-service/                 # Product microservice
├── seckill-service/                 # Flash sale microservice
├── swarm/                           # Docker Swarm configs
│   ├── docker-compose.swarm.yml
│   ├── swarm-init.sh
│   └── swarm-deploy.sh
├── user-service/                    # User microservice
│   ├── src/main/java/.../
│   │   ├── config/                  # DataSource, Redis, MQ, RateLimiter configs
│   │   ├── datasource/              # Read-write splitting (AOP + DynamicDataSource)
│   │   ├── domain/                  # Domain entities
│   │   ├── mapper/                  # MyBatis Mapper interfaces
│   │   ├── service/                 # Business logic (seckill, cache, stock)
│   │   ├── repository/             # Elasticsearch repositories
│   │   └── web/                     # REST controllers
│   └── src/main/resources/
│       ├── application.yml
│       └── lua/                     # Redis Lua scripts
├── docker-compose.yml               # Docker Compose orchestration
├── docker-start.sh                  # One-click startup script
├── Dockerfile.simple                 # Simplified Dockerfile
└── README.md
```

## Database Schema

### t_user

- `id` (BIGINT, PK)
- `username` (VARCHAR, UNIQUE)
- `password_hash` (VARCHAR)
- `phone` (VARCHAR, UNIQUE)
- `status` (TINYINT: 0=disabled, 1=active)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### t_product

- `id` (BIGINT, PK)
- `name` (VARCHAR)
- `description` (TEXT)
- `price` (DECIMAL(10,2))
- `status` (TINYINT: 0=offline, 1=online)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

### t_stock

- `id` (BIGINT, PK)
- `product_id` (BIGINT, FK -> t_product.id)
- `total_stock` (INT)
- `available_stock` (INT)
- `frozen_stock` (INT)
- `version` (INT, optimistic locking)
- `updated_at` (DATETIME)

### t_order

- `id` (BIGINT, PK)
- `order_no` (VARCHAR, UNIQUE)
- `user_id` (BIGINT, FK -> t_user.id)
- `product_id` (BIGINT, FK -> t_product.id)
- `quantity` (INT)
- `amount` (DECIMAL(10,2))
- `status` (TINYINT: 0=cancelled, 1=pending payment, 2=paid, 3=completed)
- `created_at` (DATETIME)
- `updated_at` (DATETIME)

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### Start with Docker Compose

```bash
docker-compose up -d
```

This starts all services including MySQL, Redis, RabbitMQ, and all microservices.

### Start Backend Services Manually

```bash
# Build all modules
mvn clean package -DskipTests

# Start each service (requires MySQL, Redis, RabbitMQ running)
java -jar user-service/target/user-service.jar
java -jar product-service/target/product-service.jar
java -jar order-service/target/order-service.jar
java -jar seckill-service/target/seckill-service.jar
java -jar gateway/target/gateway.jar
```

## Key Features

### High Concurrency Read (Load Balancing)

- Multiple backend instances behind Nginx (round-robin load balancing)
- Static and dynamic resource separation via Nginx
- Redis caching for hot product data
- JMeter test scripts included in `jmeter/`

### High Concurrency Write (Seckill)

- Redis Lua scripts for atomic inventory deduction
- Message queue for order async processing
- Pre-freeze inventory mechanism to prevent overselling
- Optimistic locking with version field

### Data Consistency

- Read-write splitting via DynamicDataSource + AOP
- Redis-to-DB sync service for eventual consistency
- RabbitMQ message-driven order processing

## API Overview

### User Service

- `POST /api/users/register` - User registration
- `POST /api/users/login` - User login
- `GET /api/users/me` - Get current user info

### Product Service

- `GET /api/products` - List products (pagination)
- `GET /api/products/{productId}` - Get product details
- `GET /api/products/{productId}/seckill` - Get seckill info

### Stock Service

- `GET /api/stocks/{productId}` - Get stock info
- `POST /api/stocks/freeze` - Pre-freeze inventory
- `POST /api/stocks/confirm` - Confirm stock deduction
- `POST /api/stocks/cancel` - Cancel pre-freeze

### Order Service

- `POST /api/orders` - Create order
- `GET /api/orders/{orderId}` - Get order details
- `GET /api/orders` - List user orders (pagination)
