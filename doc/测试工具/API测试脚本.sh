#!/bin/bash
# ==========================================
# API测试脚本 - 秒杀系统
# ==========================================

# 配置
BASE_URL="http://localhost:8080"
API_BASE="$BASE_URL/api"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 测试辅助函数
test_api() {
    local name=$1
    local url=$2
    local method=$3
    local data=$4

    echo -e "${YELLOW}[测试]${NC} $name"
    echo "URL: $url"

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$url")
    else
        response=$(curl -s -w "\n%{http_code}" -X POST "$url" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    echo "HTTP状态: $http_code"
    echo "响应内容: $body"

    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}[通过]${NC} $name\n"
    else
        echo -e "${RED}[失败]${NC} $name\n"
    fi
}

echo "=========================================="
echo "  秒杀系统 API 测试"
echo "=========================================="
echo ""

# 1. 用户接口测试
echo ">>> 用户接口测试 <<<"
test_api "用户登录" "$API_BASE/users/login" "POST" '{"username":"test","password":"123456"}'
test_api "用户注册" "$API_BASE/users/register" "POST" '{"username":"testuser","password":"123456","phone":"13800138000"}'

# 2. 商品接口测试
echo ">>> 商品接口测试 <<<"
test_api "商品列表" "$API_BASE/products" "GET"
test_api "商品搜索" "$API_BASE/products/search?keyword=iPhone" "GET"

# 3. 秒杀接口测试
echo ">>> 秒杀接口测试 <<<"
test_api "秒杀活动列表" "$API_BASE/seckill/activities" "GET"
test_api "秒杀活动详情" "$API_BASE/seckill/activities/1" "GET"
test_api "秒杀商品列表" "$API_BASE/seckill/activities/1/products" "GET"

# 4. 秒杀下单测试
echo ">>> 秒杀下单测试 <<<"
test_api "秒杀下单" "$API_BASE/seckill/seckill" "POST" '{"activityId":1,"productId":1,"quantity":1}'

# 5. 秒杀订单测试
echo ">>> 秒杀订单测试 <<<"
test_api "秒杀订单列表" "$API_BASE/seckill/orders" "GET"

# 6. 搜索接口测试
echo ">>> 搜索接口测试 <<<"
test_api "商品搜索" "$API_BASE/search?q=手机" "GET"
test_api "重建搜索索引" "$API_BASE/search/rebuild" "POST" '{}'

# 7. 数据源测试
echo ">>> 数据源测试 <<<"
test_api "测试数据源" "$API_BASE/test/datasource" "GET"
test_api "测试读写分离" "$API_BASE/test/read-write" "GET"

echo "=========================================="
echo "  测试完成"
echo "=========================================="
