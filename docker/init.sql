-- 由 docker-compose 挂载到 MySQL 容器 /docker-entrypoint-initdb.d/，首次启动时自动执行
CREATE DATABASE IF NOT EXISTS flash_sale DEFAULT CHARACTER SET utf8mb4;
USE flash_sale;

CREATE TABLE IF NOT EXISTS t_user
(
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(32)  UNIQUE,
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL
);
