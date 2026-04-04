-- MySQL 从库初始化脚本
-- 此脚本在 MySQL 初始化阶段执行，用于启动基本复制
-- 注意：gtid-mode 在临时服务器阶段可能不生效，采用传统方式

-- 配置主库连接信息（使用主机名，等主库启动后再启动复制）
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST='mysql-master',
    SOURCE_PORT=3306,
    SOURCE_USER='replica',
    SOURCE_PASSWORD='replica123',
    SOURCE_CONNECT_RETRY=60,
    GET_SOURCE_PUBLIC_KEY=1;

-- 启动复制线程（主库未就绪时会等待）
START REPLICA;
