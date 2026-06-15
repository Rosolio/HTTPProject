# HTTP Project 启动指南

## 一键启动

本项目提供三种一键启动脚本，请根据您的操作系统选择：

### macOS / Linux

```bash
# 方法1: 使用Shell脚本（推荐）
./start.sh

# 方法2: 使用Python脚本
python3 start.py
```

### Windows

```cmd
# 方法1: 双击运行
start.bat

# 方法2: 命令行运行
start.bat
```

## 手动启动

如果一键启动脚本无法正常工作，可以手动启动：

### 1. 编译项目

```bash
# 创建输出目录
mkdir -p out

# 编译所有Java文件
javac -d out src/server/*.java src/client/*.java
```

### 2. 启动服务器

```bash
# 启动HTTP服务器
java -cp out server.HttpServer
```

### 3. 访问应用

打开浏览器访问：
- 首页: http://localhost:8007/
- 客户端GUI: http://localhost:8007/client.html

## 启动脚本说明

| 脚本文件 | 适用平台 | 说明 |
|---------|---------|------|
| `start.sh` | macOS / Linux | Shell脚本，功能完整 |
| `start.py` | 全平台 | Python脚本，跨平台支持 |
| `start.bat` | Windows | 批处理脚本，Windows专用 |

## 启动流程

所有启动脚本执行以下流程：

1. **检查环境** - 验证Java和javac是否可用
2. **编译项目** - 编译src目录下所有Java文件到out目录
3. **启动服务器** - 运行HttpServer，监听8007端口
4. **打开浏览器** - 自动打开客户端GUI页面

## 停止服务器

在终端中按 `Ctrl + C` 停止服务器。

## 常见问题

### Q: 提示"未找到Java"怎么办？

A: 请安装JDK 11或更高版本：
- 下载地址: https://adoptium.net/
- 安装后确保 `java` 和 `javac` 命令可用

### Q: 端口8007被占用怎么办？

A: 修改`HttpServer.java`中的端口号：
```java
private static final int DEFAULT_PORT = 8080; // 改为其他端口
```

### Q: 浏览器没有自动打开？

A: 手动访问以下地址：
- http://localhost:8007/client.html

### Q: 权限不足无法执行脚本？

A: 在终端中运行：
```bash
chmod +x start.sh
```

## 功能特性

启动后可以使用以下功能：

- **静态资源访问**: HTML、TXT、PNG等文件
- **用户管理**: 注册、登录、更新、删除
- **HTTP方法**: GET、POST、PUT、DELETE、HEAD
- **重定向**: 301永久重定向、302临时重定向
- **缓存**: 304 Not Modified缓存机制
- **GUI界面**: 可视化HTTP请求测试工具