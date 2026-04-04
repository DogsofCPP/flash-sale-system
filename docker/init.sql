-- ======================================================
-- 分布式秒杀系统数据库初始化脚本
-- MySQL 主库首次启动时自动执行（在 mysql-master 容器内运行）
-- ======================================================

-- 创建用户数据库
CREATE DATABASE IF NOT EXISTS flash_sale_user DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_product DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_order DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4;

-- ======================================================
-- flash_sale_user (用户服务)
-- ======================================================
USE flash_sale_user;

CREATE TABLE IF NOT EXISTS t_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    phone         VARCHAR(32)  UNIQUE COMMENT '手机号',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1-正常 0-禁用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 测试用户（密码均为 123456）
INSERT INTO t_user (username, password_hash, phone, status) VALUES
('test001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138001', 1),
('test002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138002', 1);

-- ======================================================
-- flash_sale_product (商品服务)
-- ======================================================
USE flash_sale_product;

CREATE TABLE IF NOT EXISTS t_product (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128)  NOT NULL COMMENT '商品名称',
    description  VARCHAR(512)  COMMENT '商品描述',
    price        DECIMAL(10,2) NOT NULL COMMENT '原价',
    stock        INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '1-上架 0-下架',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 测试商品
INSERT INTO t_product (name, description, price, stock, status) VALUES
('iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB 深空黑', 7999.00, 100, 1),
('MacBook Pro 14', 'MacBook Pro 14-inch M3芯片 16+512GB', 14999.00, 50, 1),
('AirPods Pro 2', 'AirPods Pro 2代 支持主动降噪', 1899.00, 200, 1),
('iPad Air 5', 'iPad Air 5 256GB WiFi 紫色', 5499.00, 80, 1),
('Apple Watch S9', 'Apple Watch Series 9 45mm 蜂窝版', 2999.00, 150, 1);

-- 创建 ShardingSphere 分库分表数据库
CREATE DATABASE IF NOT EXISTS flash_sale_order_0 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_order_1 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_order_2 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_order_3 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_0 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_1 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_2 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS flash_sale_3 DEFAULT CHARACTER SET utf8mb4;

-- ======================================================
-- flash_sale_order (订单服务 - 单库单表，演示用)
-- ======================================================
USE flash_sale_order;

CREATE TABLE IF NOT EXISTS t_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(64)   NOT NULL UNIQUE COMMENT '订单编号',
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    product_id      BIGINT        NOT NULL COMMENT '商品ID',
    activity_id     BIGINT        COMMENT '秒杀活动ID',
    product_name    VARCHAR(128)  NOT NULL COMMENT '商品名称',
    product_price   DECIMAL(10,2) NOT NULL COMMENT '购买单价',
    quantity        INT           NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_amount    DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已支付 2-已取消 3-已退款 4-已超时',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    pay_time        DATETIME COMMENT '支付时间',
    expire_time     DATETIME COMMENT '订单过期时间',
    cancel_time     DATETIME COMMENT '取消时间',
    INDEX idx_user (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_expire (expire_time),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ======================================================
-- flash_sale (秒杀服务)
-- ======================================================
USE flash_sale;

-- 秒杀活动表
CREATE TABLE IF NOT EXISTS seckill_activity (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL COMMENT '活动名称',
    start_time  DATETIME     NOT NULL COMMENT '开始时间',
    end_time    DATETIME     NOT NULL COMMENT '结束时间',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0-未开始 1-进行中 2-已结束',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_time (start_time, end_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

-- 秒杀商品关联表
CREATE TABLE IF NOT EXISTS seckill_product (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    activity_id     BIGINT        NOT NULL COMMENT '秒杀活动ID',
    product_id      BIGINT        NOT NULL COMMENT '商品ID',
    seckill_price   DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    stock           INT           NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    sold_count      INT           NOT NULL DEFAULT 0 COMMENT '已售数量',
    limit_per_user  INT           NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activity (activity_id),
    INDEX idx_product (product_id),
    UNIQUE KEY uk_activity_product (activity_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品关联表';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS seckill_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    activity_id     BIGINT        NOT NULL COMMENT '秒杀活动ID',
    product_id      BIGINT        NOT NULL COMMENT '商品ID',
    order_no        VARCHAR(64)   COMMENT '关联订单号',
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '0-排队中 1-成功 2-失败 3-超时',
    fail_reason     VARCHAR(255)  COMMENT '失败原因',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_activity (user_id, activity_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    UNIQUE KEY uk_user_activity_product (user_id, activity_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- 库存流水表
CREATE TABLE IF NOT EXISTS stock_flow (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    activity_id     BIGINT        NOT NULL COMMENT '秒杀活动ID',
    product_id      BIGINT        NOT NULL COMMENT '商品ID',
    order_no        VARCHAR(64)   COMMENT '关联订单号',
    type            TINYINT       NOT NULL COMMENT '1-扣减 2-回滚 3-补偿',
    quantity        INT           NOT NULL COMMENT '数量',
    before_stock    INT           NOT NULL COMMENT '变动前库存',
    after_stock     INT           NOT NULL COMMENT '变动后库存',
    source          VARCHAR(32)   COMMENT '来源：redis/mq/manual',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_product (activity_id, product_id),
    INDEX idx_order_no (order_no),
    INDEX idx_type (type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- 测试秒杀活动
INSERT INTO seckill_activity (name, start_time, end_time, status) VALUES
('iPhone 15 Pro 限时秒杀', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1),
('MacBook Pro 专场特惠', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 2 HOUR, 0),
('Apple 全家桶狂欢节', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY) + INTERVAL 24 HOUR, 0);

-- 测试秒杀商品（关联商品服务中的商品）
INSERT INTO seckill_product (activity_id, product_id, seckill_price, stock, sold_count, limit_per_user) VALUES
(1, 1, 5999.00, 10, 0, 1),
(1, 3, 1499.00, 50, 0, 2),
(2, 2, 12999.00, 10, 0, 1);

-- ======================================================
-- 主从复制配置
-- ======================================================
CREATE USER IF NOT EXISTS 'replica'@'%' IDENTIFIED WITH mysql_native_password BY 'replica123';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'replica'@'%';
FLUSH PRIVILEGES;
