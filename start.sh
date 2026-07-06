#!/bin/bash
# 智能情报分析平台 - 一键启动脚本（Spring Boot + Vue）
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================="
echo "  智能情报分析平台 v2.0"
echo "  Spring Boot + Vue 3"
echo "========================================="
echo ""

# 启动后端
echo "[1/2] 启动 Spring Boot 后端 (port 8080)..."
cd "$SCRIPT_DIR/backend-springboot"
./mvnw spring-boot:run -q &
BACKEND_PID=$!
echo "  后端 PID: $BACKEND_PID"

# 等待后端启动
echo "  等待后端就绪..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8080/api/dashboard/stats > /dev/null 2>&1; then
        echo "  后端已就绪"
        break
    fi
    sleep 1
done

# 启动前端
echo "[2/2] 启动 Vue 前端 (port 5173)..."
cd "$SCRIPT_DIR/frontend-vue"
npm run dev &
FRONTEND_PID=$!
echo "  前端 PID: $FRONTEND_PID"

echo ""
echo "========================================="
echo "  启动完成！"
echo "  前端: http://localhost:5173"
echo "  后端: http://localhost:8080"
echo "  按 Ctrl+C 停止所有服务"
echo "========================================="

# 捕获 Ctrl+C，清理子进程
trap "echo ''; echo '正在停止...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT TERM

wait
