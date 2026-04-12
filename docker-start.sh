#!/bin/bash
# ==========================================
# 秒杀系统 Docker 一键启动脚本
# ==========================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "=========================================="
echo "  分布式秒杀系统 Docker 启动脚本"
echo "=========================================="
echo ""

# 1. 检查Docker
echo -e "${YELLOW}[1/7]${NC} 验证Docker环境..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}[错误]${NC} Docker 未安装"
    exit 1
fi
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}[错误]${NC} Docker Compose 未安装"
    exit 1
fi
echo -e "${GREEN}[OK]${NC} Docker: $(docker --version)"
echo -e "${GREEN}[OK]${NC} Docker Compose: $(docker-compose --version)"
echo ""

# 2. 检查端口
echo -e "${YELLOW}[2/7]${NC} 检查关键端口..."
check_port() {
    local port=$1
    local name=$2
    if netstat -an 2>/dev/null | grep -q ":$port.*LISTEN" || ss -tlnp 2>/dev/null | grep -q ":$port"; then
        echo -e "${RED}[警告]${NC} 端口 $port ($name) 已被占用"
    else
        echo -e "${GREEN}[OK]${NC} 端口 $port ($name) 可用"
    fi
}
check_port 80 "Nginx"
check_port 3307 "MySQL Master"
check_port 3308 "MySQL Slave"
check_port 6379 "Redis"
check_port 8080 "Gateway"
check_port 8083 "Product Service"
check_port 8085 "Order Service"
check_port 8086 "Seckill Service"
check_port 9200 "Elasticsearch"
check_port 5672 "RabbitMQ"
check_port 8848 "Nacos"
echo ""

# 3. 清理旧容器
echo -e "${YELLOW}[3/7]${NC} 清理旧容器和镜像..."
docker-compose down --remove-orphans 2>/dev/null || true
echo -e "${GREEN}[完成]${NC} 清理完成"
echo ""

# 4. 构建镜像
echo -e "${YELLOW}[4/7]${NC} 构建应用镜像（首次约5-10分钟）..."
docker-compose build --no-cache
echo -e "${GREEN}[完成]${NC} 镜像构建完成"
echo ""

# 5. 启动基础设施
echo -e "${YELLOW}[5/7]${NC} 启动基础设施（MySQL、Redis、RabbitMQ、ES、Nacos、Kafka）..."
docker-compose up -d mysql-master mysql-slave redis rabbitmq elasticsearch kibana nacos kafka
echo ""

# 6. 等待MySQL就绪
echo -e "${YELLOW}[6/7]${NC} 等待MySQL Master就绪（约30-60秒）..."
for i in {1..30}; do
    if docker exec flash-sale-mysql-master mysqladmin ping -h localhost -uroot -p123456 &>/dev/null; then
        echo -e "${GREEN}[OK]${NC} MySQL Master 已就绪"
        break
    fi
    echo "  等待中 ($i/30)..."
    sleep 2
done
echo ""

# 7. 启动应用服务
echo -e "${YELLOW}[7/7]${NC} 启动应用服务和网关..."
docker-compose up -d gateway user-service-1 user-service-2 product-service-1 order-service-1 seckill-service-1 seckill-service-2 nginx
echo ""

# 等待所有服务启动
echo -e "${YELLOW}[完成]${NC} 等待服务启动（约1分钟）..."
sleep 60

echo ""
echo "=========================================="
echo "  启动完成！"
echo "=========================================="
echo ""
echo -e "${GREEN}访问地址：${NC}"
echo "  前端页面:   http://localhost"
echo "  API网关:    http://localhost:8080"
echo "  Nacos:      http://localhost:8848/nacos (nacos/nacos)"
echo "  RabbitMQ:   http://localhost:15672 (guest/guest)"
echo "  Kibana:     http://localhost:5601"
echo "  ES:         http://localhost:9200"
echo "  Kafka:      localhost:9092"
echo ""
echo -e "${GREEN}微服务端口：${NC}"
echo "  Gateway:      8080"
echo "  User Service:   8081 (实例1) / 8082 (实例2)"
echo "  Product Service: 8083"
echo "  Order Service:   8085"
echo "  Seckill Service: 8086 (实例1) / 实例2"
echo ""
echo -e "${GREEN}常用命令：${NC}"
echo "  查看状态:    docker-compose ps"
echo "  查看日志:    docker-compose logs -f seckill-service-1"
echo "  停止:        docker-compose down"
echo "  重启:        docker-compose restart"
echo ""
echo -e "${YELLOW}测试接口：${NC}"
echo "  curl http://localhost/api/users/info"
echo "  curl http://localhost/api/products"
echo "  curl http://localhost/api/seckill/activities"
echo ""
