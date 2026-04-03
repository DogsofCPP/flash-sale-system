#!/bin/bash
# ==========================================
# 秒杀系统 Docker 一键启动脚本
# ==========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "=========================================="
echo "  秒杀系统 Docker 启动脚本"
echo "=========================================="
echo ""

# 1. 检查Docker
echo -e "${YELLOW}[检查]${NC} 验证Docker环境..."

if ! command -v docker &> /dev/null; then
    echo -e "${RED}[错误]${NC} Docker 未安装"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}[错误]${NC} Docker Compose 未安装"
    exit 1
fi

docker_version=$(docker --version)
compose_version=$(docker-compose --version)

echo -e "${GREEN}[OK]${NC} Docker: $docker_version"
echo -e "${GREEN}[OK]${NC} Docker Compose: $compose_version"
echo ""

# 2. 检查端口占用
echo -e "${YELLOW}[检查]${NC} 检查端口占用..."
check_port() {
    local port=$1
    local name=$2
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -an | grep -q ":$port.*LISTEN"; then
        echo -e "${RED}[警告]${NC} 端口 $port ($name) 已被占用"
        return 1
    else
        echo -e "${GREEN}[OK]${NC} 端口 $port ($name) 可用"
        return 0
    fi
}

# 需要检查的端口
check_port 80 "Nginx" || true
check_port 3306 "MySQL" || true
check_port 6379 "Redis" || true
check_port 8080 "Gateway" || true
echo ""

# 3. 检查磁盘空间
echo -e "${YELLOW}[检查]${NC} 检查磁盘空间..."
df -h . | awk 'NR==2 {if ($4+0 < 10) print "'"${RED}[警告]${NC} 磁盘空间不足: "$4" 可用"; else print "'"${GREEN}[OK]${NC} 磁盘空间: "$4" 可用"}'
echo ""

# 4. 构建镜像
echo -e "${YELLOW}[构建]${NC} 构建应用镜像..."
docker-compose build --no-cache
echo -e "${GREEN}[完成]${NC} 镜像构建完成"
echo ""

# 5. 启动基础设施服务
echo -e "${YELLOW}[启动]${NC} 启动基础设施服务..."
docker-compose up -d mysql-user mysql-product mysql-stock mysql-order redis
echo ""

# 6. 等待MySQL就绪
echo -e "${YELLOW}[等待]${NC} 等待MySQL服务就绪..."
echo "这可能需要30-60秒..."
for i in {1..30}; do
    if docker exec flash-sale-mysql-user mysqladmin ping -h localhost -uroot -p123456 &>/dev/null; then
        echo -e "${GREEN}[OK]${NC} MySQL服务已就绪"
        break
    fi
    echo "  等待中... ($i/30)"
    sleep 2
done
echo ""

# 7. 启动所有服务
echo -e "${YELLOW}[启动]${NC} 启动所有服务..."
docker-compose up -d
echo ""

# 8. 等待服务就绪
echo -e "${YELLOW}[等待]${NC} 等待所有服务启动..."
echo "这可能需要2-3分钟..."
sleep 30
echo ""

# 9. 检查服务状态
echo -e "${YELLOW}[检查]${NC} 检查服务状态..."
docker-compose ps
echo ""

# 10. 显示访问信息
echo "=========================================="
echo "  启动完成！"
echo "=========================================="
echo ""
echo -e "${GREEN}访问地址：${NC}"
echo "  前端页面:  http://localhost"
echo "  API网关:   http://localhost:8080"
echo "  Nacos:     http://localhost:8848/nacos (nacos/nacos)"
echo "  RabbitMQ:  http://localhost:15672 (guest/guest)"
echo "  Kibana:    http://localhost:5601"
echo "  ES:        http://localhost:9200"
echo ""
echo -e "${GREEN}常用命令：${NC}"
echo "  查看日志: docker-compose logs -f user-service-8081"
echo "  停止服务: docker-compose down"
echo "  重启服务: docker-compose restart"
echo ""
echo -e "${YELLOW}查看详细日志：${NC}"
echo "  docker-compose logs -f"
echo ""
