#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
HTTP Project 一键启动脚本
作者: 计网第7小组
"""

import os
import sys
import subprocess
import platform
import time
import webbrowser
import signal
import shutil

# 颜色定义
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    PURPLE = '\033[0;35m'
    CYAN = '\033[0;36m'
    NC = '\033[0m'  # No Color

# 项目配置
PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_DIR = os.path.join(PROJECT_DIR, 'src')
OUT_DIR = os.path.join(PROJECT_DIR, 'out')
STATIC_DIR = os.path.join(PROJECT_DIR, 'static')
PORT = 8007
SERVER_URL = f"http://localhost:{PORT}"
CLIENT_URL = f"http://localhost:{PORT}/client.html"

def print_banner():
    """打印启动横幅"""
    banner = f"""
{Colors.CYAN}╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║              HTTP Project 一键启动脚本                        ║
║                                                              ║
║              计算机网络 第7小组                                ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝{Colors.NC}
"""
    print(banner)

def check_environment():
    """检查Java环境"""
    print(f"{Colors.YELLOW}[1/4] 检查运行环境...{Colors.NC}")
    
    # 检查java命令
    java_path = shutil.which('java')
    if not java_path:
        print(f"{Colors.RED}✗ 错误: 未找到Java，请先安装JDK 11+${Colors.NC}")
        print(f"  下载地址: https://adoptium.net/")
        sys.exit(1)
    
    # 检查javac命令
    javac_path = shutil.which('javac')
    if not javac_path:
        print(f"{Colors.RED}✗ 错误: 未找到javac编译器，请安装完整JDK${Colors.NC}")
        sys.exit(1)
    
    # 获取Java版本
    try:
        result = subprocess.run(['java', '-version'], capture_output=True, text=True)
        version_line = result.stderr.split('\n')[0]
        print(f"{Colors.GREEN}✓ Java环境检查通过{Colors.NC}")
        print(f"  {version_line}")
    except Exception as e:
        print(f"{Colors.YELLOW}⚠ 无法获取Java版本: {e}{Colors.NC}")

def compile_project():
    """编译项目"""
    print(f"\n{Colors.YELLOW}[2/4] 编译项目...{Colors.NC}")
    
    # 清理out目录
    if os.path.exists(OUT_DIR):
        shutil.rmtree(OUT_DIR)
    os.makedirs(OUT_DIR)
    
    # 收集所有Java文件
    java_files = []
    for root, dirs, files in os.walk(SRC_DIR):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    
    if not java_files:
        print(f"{Colors.RED}✗ 错误: 未找到Java源文件${Colors.NC}")
        sys.exit(1)
    
    print(f"  找到 {len(java_files)} 个Java文件")
    
    # 编译
    try:
        result = subprocess.run(
            ['javac', '-d', OUT_DIR] + java_files,
            capture_output=True,
            text=True,
            cwd=PROJECT_DIR
        )
        
        if result.returncode == 0:
            print(f"{Colors.GREEN}✓ 编译成功${Colors.NC}")
        else:
            print(f"{Colors.RED}✗ 编译失败:${Colors.NC}")
            print(result.stderr)
            sys.exit(1)
    except Exception as e:
        print(f"{Colors.RED}✗ 编译异常: {e}{Colors.NC}")
        sys.exit(1)

def start_server():
    """启动服务器"""
    print(f"\n{Colors.YELLOW}[3/4] 启动HTTP服务器...{Colors.NC}")
    print(f"  端口: {PORT}")
    print(f"  静态资源: {STATIC_DIR}")
    
    # 切换到项目根目录
    os.chdir(PROJECT_DIR)
    
    # 启动服务器进程
    try:
        server_process = subprocess.Popen(
            ['java', '-cp', OUT_DIR, 'server.HttpServer'],
            cwd=PROJECT_DIR,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            universal_newlines=True,
            bufsize=1
        )
        
        # 等待服务器启动
        time.sleep(1)
        
        if server_process.poll() is not None:
            print(f"{Colors.RED}✗ 服务器启动失败${Colors.NC}")
            sys.exit(1)
        
        print(f"{Colors.GREEN}✓ 服务器已启动 (PID: {server_process.pid})${Colors.NC}")
        return server_process
        
    except Exception as e:
        print(f"{Colors.RED}✗ 启动服务器异常: {e}{Colors.NC}")
        sys.exit(1)

def open_browser():
    """打开浏览器"""
    print(f"\n{Colors.YELLOW}[4/4] 打开浏览器...{Colors.NC}")
    
    time.sleep(1)
    
    try:
        webbrowser.open(CLIENT_URL)
        print(f"{Colors.GREEN}✓ 已打开浏览器${Colors.NC}")
    except Exception as e:
        print(f"{Colors.YELLOW}⚠ 无法自动打开浏览器: {e}{Colors.NC}")
        print(f"  请手动访问: {CLIENT_URL}")

def print_success():
    """打印成功信息"""
    success_msg = f"""
{Colors.GREEN}╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║                    ✓ 启动成功！                               ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝{Colors.NC}

{Colors.CYAN}访问地址:{Colors.NC}
  • 首页:      {Colors.BLUE}{SERVER_URL}/{Colors.NC}
  • 客户端GUI: {Colors.BLUE}{CLIENT_URL}{Colors.NC}

{Colors.CYAN}功能测试:{Colors.NC}
  • 静态资源:  {SERVER_URL}/test.txt
  • 用户注册:  POST {SERVER_URL}/register
  • 用户登录:  POST {SERVER_URL}/login
  • 301重定向: {SERVER_URL}/old
  • 302重定向: {SERVER_URL}/temp

{Colors.YELLOW}按 Ctrl+C 停止服务器{Colors.NC}
"""
    print(success_msg)

def monitor_server(server_process):
    """监控服务器输出"""
    try:
        for line in server_process.stdout:
            print(f"  {line.rstrip()}")
    except KeyboardInterrupt:
        pass

def main():
    """主函数"""
    print_banner()
    
    # 处理Ctrl+C信号
    def signal_handler(sig, frame):
        print(f"\n{Colors.YELLOW}正在停止服务器...{Colors.NC}")
        if 'server_process' in locals():
            server_process.terminate()
            server_process.wait(timeout=5)
        print(f"{Colors.GREEN}✓ 服务器已停止{Colors.NC}")
        sys.exit(0)
    
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # 执行启动流程
    check_environment()
    compile_project()
    server_process = start_server()
    open_browser()
    print_success()
    
    # 监控服务器输出
    try:
        monitor_server(server_process)
    except KeyboardInterrupt:
        pass
    finally:
        print(f"\n{Colors.YELLOW}正在停止服务器...{Colors.NC}")
        server_process.terminate()
        try:
            server_process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            server_process.kill()
        print(f"{Colors.GREEN}✓ 服务器已停止{Colors.NC}")

if __name__ == '__main__':
    main()