@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

:: HTTP Project 一键启动脚本 (Windows版)
:: 作者: 计网第7小组

echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║                                                              ║
echo ║              HTTP Project 一键启动脚本                        ║
echo ║                                                              ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

:: 检查Java环境
echo [1/4] 检查运行环境...
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ 错误: 未找到Java，请先安装JDK 11+
    echo   下载地址: https://adoptium.net/
    pause
    exit /b 1
)

where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ 错误: 未找到javac编译器，请安装完整JDK
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    echo ✓ Java环境检查通过
    echo   Java版本: %%g
)

:: 设置路径
set "PROJECT_DIR=%~dp0"
set "SRC_DIR=%PROJECT_DIR%src"
set "OUT_DIR=%PROJECT_DIR%out"
set "PORT=8007"

:: 编译项目
echo.
echo [2/4] 编译项目...

if exist "%OUT_DIR%" (
    rd /s /q "%OUT_DIR%"
)
mkdir "%OUT_DIR%"

:: 查找所有Java文件
set "JAVA_FILES="
for /r "%SRC_DIR%" %%f in (*.java) do (
    set "JAVA_FILES=!JAVA_FILES! "%%f""
)

if "!JAVA_FILES!"=="" (
    echo ✗ 错误: 未找到Java源文件
    pause
    exit /b 1
)

:: 编译
javac -d "%OUT_DIR%" %JAVA_FILES%
if %errorlevel% neq 0 (
    echo ✗ 编译失败
    pause
    exit /b 1
)
echo ✓ 编译成功

:: 启动服务器
echo.
echo [3/4] 启动HTTP服务器...
echo   端口: %PORT%
echo   静态资源: %PROJECT_DIR%static

:: 切换到项目根目录
cd /d "%PROJECT_DIR%"

:: 后台启动浏览器
echo.
echo [4/4] 打开浏览器...
start "" "http://localhost:%PORT%/client.html"

echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║                                                              ║
echo ║                    ✓ 启动成功！                               ║
echo ║                                                              ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.
echo 访问地址:
echo   • 首页:      http://localhost:%PORT%/
echo   • 客户端GUI: http://localhost:%PORT%/client.html
echo.
echo 功能测试:
echo   • 静态资源:  http://localhost:%PORT%/test.txt
echo   • 用户注册:  POST http://localhost:%PORT%/register
echo   • 用户登录:  POST http://localhost:%PORT%/login
echo   • 301重定向: http://localhost:%PORT%/old
echo   • 302重定向: http://localhost:%PORT%/temp
echo.
echo 按 Ctrl+C 停止服务器
echo.

:: 启动服务器
java -cp "%OUT_DIR%" server.HttpServer

:: 清理
echo.
echo 正在停止服务器...
echo ✓ 服务器已停止
pause