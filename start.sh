#!/bin/bash

set -e

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd "$SCRIPT_DIR"

echo "========================================"
echo "企业园区接待室影音设备管理系统"
echo "========================================"

if [ ! -f ".env" ]; then
    echo "错误: 未找到 .env 文件"
    exit 1
fi

source .env

echo ""
echo "端口配置:"
echo "  前端: $FRONTEND_PORT"
echo "  后端: $BACKEND_PORT"
echo "  MySQL: $MYSQL_PORT"
echo "  Redis: $REDIS_PORT"
echo ""

echo "检查端口占用情况..."
for port in $FRONTEND_PORT $BACKEND_PORT $MYSQL_PORT $REDIS_PORT; do
    if nc -z localhost "$port" 2>/dev/null; then
        echo "错误: 端口 $port 已被占用"
        lsof -i ":$port" 2>/dev/null | grep LISTEN | head -1
        exit 1
    fi
done

echo "端口检查通过"
echo ""

echo "启动 Docker Compose 服务..."
docker compose up -d --build

echo ""
echo "等待服务启动..."
sleep 10

echo ""
echo "服务状态:"
docker compose ps

echo ""
echo "========================================"
echo "启动完成！"
echo ""
echo "前端访问地址: http://localhost:$FRONTEND_PORT"
echo "后端 API: http://localhost:$BACKEND_PORT/api"
echo "========================================"
