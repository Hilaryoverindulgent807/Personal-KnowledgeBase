# Windows Build Script for 智能情报分析平台
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path "$ScriptDir\.."

Write-Host "=========================================" -ForegroundColor Green
Write-Host "  正在启动 Windows 桌面应用构建..." -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green

# 1. 检查环境
Write-Host "Checking prerequisites..."
if (!(Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java is not installed. Need Java 17+ or 21+."
}
if (!(Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Error "Node.js/npm is not installed."
}
if (!(Get-Command cargo -ErrorAction SilentlyContinue)) {
    Write-Error "Rust/Cargo is not installed."
}

# 2. 编译 Spring Boot
Write-Host "`n1. 正在编译 Spring Boot 后端..." -ForegroundColor Cyan
Set-Location "$RootDir\backend-springboot"
& .\mvnw.cmd clean package -DskipTests

# 3. 复制 JAR 并准备 Sidecar
Write-Host "`n2. 正在准备 Sidecar 运行包..." -ForegroundColor Cyan
$BinDir = "$RootDir\src-tauri\binaries"
if (!(Test-Path $BinDir)) {
    New-Item -ItemType Directory -Path $BinDir | Out-Null
}

Copy-Item "$RootDir\backend-springboot\target\backend.jar" "$BinDir\backend.jar" -Force

# 创建 Windows x86_64 架构的 dummy sidecar .exe 占位符
# (Windows 环境打包必须提供 .exe 后缀的文件)
$DummyExePath = "$BinDir\java-backend-x86_64-pc-windows-msvc.exe"
if (!(Test-Path $DummyExePath)) {
    # 写入一个简易的占位字节或空文件
    New-Item -ItemType File -Path $DummyExePath -Force | Out-Null
}

Write-Host "`n3. 正在安装前端依赖并执行编译..." -ForegroundColor Cyan
Set-Location "$RootDir\frontend-vue"
& npm install

Write-Host "`n4. 正在执行 Tauri 桌面打包..." -ForegroundColor Cyan
Set-Location "$RootDir"
& npx --prefix frontend-vue tauri build

Write-Host "`n=========================================" -ForegroundColor Green
Write-Host "🎉 构建成功完成！" -ForegroundColor Green
Write-Host "打包输出目录: $RootDir\src-tauri\target\release\bundle\nsis\" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
