#!/bin/bash
# Docker Swarm 部署脚本

echo "=========================================="
echo "  Docker Swarm 部署脚本"
echo "=========================================="
echo ""

cd "$(dirname "$0")"

# 构建镜像
echo "构建镜像..."
docker build -t seckill-service:latest -f ../Dockerfile ..

# 推送镜像到registry（如果有）
# docker push seckill-service:latest

# 部署Stack
echo "部署Stack..."
docker stack deploy -c docker-compose.swarm.yml flash-sale

echo ""
echo "=========================================="
echo "  部署完成"
echo "=========================================="
echo ""
echo "查看服务状态："
echo "  docker service ls"
echo "  docker service ps flash-sale_seckill-service"
echo ""
echo "查看日志："
echo "  docker service logs flash-sale_seckill-service"
echo ""
echo "扩容："
echo "  docker service scale flash-sale_seckill-service=5"
echo ""
echo "删除："
echo "  docker stack rm flash-sale"
echo ""
