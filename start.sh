#!/bin/bash

# HTTP Project 一键启动脚本
# 作者: 计网第7小组

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目路径
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
OUT_DIR="$PROJECT_DIR/out"
STATIC_DIR="$PROJECT_DIR/static"

# 默认端口
PORT=8007

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   HTTP Project 一键启动脚本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 检查Java环境
check_java() {
    if ! command -v java &> /dev/null; then
        echo -e "${RED}错误: 未找到Java，请先安装JDK${NC}"
        exit 1
    fi
    
    if ! command -v javac &> /dev/null; then
        echo -e "${RED}错误: 未找到javac，请先安装JDK${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Java环境检查通过${NC}"
}

# 清理并编译
compile() {
    echo -e "${YELLOW}正在编译项目...${NC}"
    
    # 清理out目录
    rm -rf "$OUT_DIR"
    mkdir -p "$OUT_DIR"
    
    # 编译所有Java文件
    javac -d "$OUT_DIR" "$SRC_DIR"/server/*.java "$SRC_DIR"/client/*.java
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 编译成功${NC}"
    else
        echo -e "${RED}✗ 编译失败${NC}"
        exit 1
    fi
}

# 启动服务器
start_server() {
    echo ""
    echo -e "${YELLOW}正在启动HTTP服务器...${NC}"
    echo -e "${BLUE}端口: $PORT${NC}"
    echo -e "${BLUE}静态资源目录: $STATIC_DIR${NC}"
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}   服务器已启动！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "访问地址:"
    echo -e "  ${BLUE}首页: http://localhost:$PORT/${NC}"
    echo -e "  ${BLUE}客户端GUI: http://localhost:$PORT/client.html${NC}"
    echo ""
    echo -e "按 ${RED}Ctrl+C${NC} 停止服务器"
    echo ""
    
    # 切换到项目根目录（静态资源相对路径）
    cd "$PROJECT_DIR"
    
    # 启动服务器
    java -cp "$OUT_DIR" server.HttpServer
}

# 打开浏览器
open_browser() {
    sleep 2
    
    # 检测操作系统并打开浏览器
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        open "http://localhost:$PORT/client.html"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        xdg-open "http://localhost:$PORT/client.html" 2>/dev/null || true
    fi
}

# 主流程
main() {
    check_java
    compile
    
    # 在后台打开浏览器
    open_browser &
    
    # 启动服务器（前台运行）
    start_server
}

# 捕获中断信号
trap 'echo -e "\n${YELLOW}正在停止服务器...${NC}; exit 0' INT TERM

# 运行主流程
main