#!/usr/bin/env bash
# Linux Build Script for 智能情报分析平台
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "========================================="
echo "  正在启动 Linux 桌面应用构建..."
echo "========================================="

# 1. 检查必备环境
echo "Checking prerequisites..."
if ! command -v java &> /dev/null; then
    echo "[Error] Java is not installed."
    exit 1
fi
if ! command -v npm &> /dev/null; then
    echo "[Error] Node.js/npm is not installed."
    exit 1
fi
if ! command -v cargo &> /dev/null; then
    echo "[Error] Rust/Cargo is not installed."
    exit 1
fi

# 2. 编译 Spring Boot 后端
echo "-----------------------------------------"
echo "1. 正在编译 Spring Boot 后端..."
echo "-----------------------------------------"
cd "$ROOT_DIR/backend-springboot"
./mvnw clean package -DskipTests

# 3. 复制 JAR 并准备 Sidecar 目录
echo "-----------------------------------------"
echo "2. 正在准备 Sidecar 运行包..."
echo "-----------------------------------------"
mkdir -p "$ROOT_DIR/src-tauri/binaries"
cp "$ROOT_DIR/backend-springboot/target/backend.jar" "$ROOT_DIR/src-tauri/binaries/backend.jar"

# 创建 Linux x86_64 架构的 dummy sidecar 占位符
cat << 'EOF' > "$ROOT_DIR/src-tauri/binaries/java-backend-x86_64-unknown-linux-gnu"
#!/bin/sh
echo "Intelligence Platform Java Sidecar Placeholder"
exit 0
EOF
chmod +x "$ROOT_DIR/src-tauri/binaries/java-backend-x86_64-unknown-linux-gnu"

# 4. 编译前端并构建 Tauri 桌面包
echo "-----------------------------------------"
echo "3. 正在安装前端依赖并执行编译..."
echo "-----------------------------------------"
cd "$ROOT_DIR/frontend-vue"
npm install

echo "-----------------------------------------"
echo "4. 正在执行 Tauri 桌面打包..."
echo "-----------------------------------------"
cd "$ROOT_DIR"
npx --prefix frontend-vue tauri build

echo "========================================="
echo "🎉 构建成功完成！"
echo "打包输出目录: $ROOT_DIR/src-tauri/target/release/bundle/"
echo "========================================="
