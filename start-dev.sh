#!/usr/bin/env bash
# 智能情报分析平台 (重构版) - 一键启动开发服务器
# 同时启动 Spring Boot 后端 (Port: 8080) 和 Vue3 前端开发服务器 (Port: 5173)

# 确保脚本发生错误或被终止时，自动清理所有后台进程
trap 'kill $(jobs -p) 2>/dev/null || true' EXIT INT TERM

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend-springboot"
FRONTEND_DIR="$SCRIPT_DIR/frontend-vue"

echo "========================================="
echo "  正在启动 智能情报分析平台 (重构版)..."
echo "========================================="

# 1. 启动 Spring Boot 后端
echo "[1/2] 正在后台启动 Spring Boot 后端..."
cd "$BACKEND_DIR"
mvn spring-boot:run > springboot.log 2>&1 &
BACKEND_PID=$!

# 等待后端端口 8080 启动
echo "等待后端服务就绪 (端口: 8080)..."
for i in {1..40}; do
    if lsof -i :8080 >/dev/null 2>&1; then
        echo "后端已成功启动！(PID: $BACKEND_PID)"
        break
    fi
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        echo "[错误] 后端服务启动失败，查看日志以获取详情: $BACKEND_DIR/springboot.log"
        exit 1
    fi
    sleep 1
done

# 2. 启动 Vue 前端
echo "[2/2] 正在启动 Vue 前端开发服务器..."
cd "$FRONTEND_DIR"

echo ""
echo "========================================="
echo "  服务启动完毕："
echo "  前端地址 (Vue):    http://localhost:5173"
echo "  后端 API (Spring): http://localhost:8080"
echo "  后端日志路径:      $BACKEND_DIR/springboot.log"
echo "========================================="
echo ""

npm run dev
