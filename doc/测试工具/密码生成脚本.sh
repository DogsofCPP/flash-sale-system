#!/bin/bash
# ==========================================
# 密码生成脚本
# 使用BCrypt加密密码
# ==========================================

echo "=========================================="
echo "  密码生成工具"
echo "=========================================="

# 读取密码
read -p "请输入密码（默认: 123456）: " password
password=${password:-123456}

echo ""
echo "输入的密码: $password"
echo ""
echo "BCrypt加密结果（Spring Security格式）:"

# 方法1: 使用htpasswd (Apache)
if command -v htpasswd &> /dev/null; then
    echo "使用 htpasswd (Apache):"
    htpasswd -nbBC 10 "" "$password" | tr -d ':\n' | sed 's/^\$2y\$/\$2a\$/' | sed 's/\$2y\$/\$2a\$/'
    echo ""
fi

# 方法2: 使用Python生成BCrypt
if command -v python3 &> /dev/null; then
    echo "使用 Python3:"
    python3 -c "
import hashlib, bcrypt
password = '$password'.encode('utf-8')
hashed = bcrypt.hashpw(password, bcrypt.gensalt(rounds=10))
print(hashed.decode('utf-8'))
"
    echo ""
fi

# 方法3: 使用Java生成（需要Maven）
if [ -f "pom.xml" ]; then
    echo "提示: 可使用Java BCrypt库生成"
    echo "在Spring Security中会自动处理BCrypt格式密码"
fi

echo "=========================================="
echo "  测试账号（BCrypt加密后的密码）"
echo "=========================================="
echo ""
echo "用户: test      密码: 123456"
echo "用户: admin     密码: admin123"
echo ""
echo "BCrypt加密后的密码可以直接存入数据库"
echo "系统启动时会自动验证BCrypt格式密码"
echo ""
