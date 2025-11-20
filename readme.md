# HTTPProject 说明文档

## 项目简介

HTTPProject 是一个基于 Java 实现的简易 HTTP 服务器与客户端程序，支持基本的 HTTP 协议交互，包括 GET/POST
请求处理、静态资源访问、用户注册登录、重定向以及缓存机制等功能。

## 项目结构

```
HTTPProject/
├── src/
│   ├── server/           # HTTP 服务器相关类
│   │   ├── HttpServer.java       # 服务器主类，负责监听端口和接收连接
│   │   ├── RequestHandler.java   # 处理客户端请求的核心类
│   │   ├── UserService.java      # 用户注册登录服务（内存存储）
│   │   ├── MimeUtils.java        # MIME 类型工具类
│   ├── client/           # HTTP 客户端相关类
│   │   ├── HttpClient.java       # 客户端主类，支持发送GET/POST请求
│   ├── common/           # 公共模块（预留）
├── static/               # 静态资源目录
│   ├── index.html        # 测试页面
│   ├── njuLOGO.png       # 测试png格式
│   ├── new.html          # 301 重定向目标页面
│   ├── temp-new.html     # 302 重定向目标页面
│   ├── login.html        # 登录页面
│   ├── register.html     # 注册页面
├── .gitignore            # Git 忽略文件配置
├── .idea/                # IDEA 项目配置
└── readme.md             # 项目说明文档
```

## 功能说明

### 服务器功能

1. **基础 HTTP 协议支持**
    - 处理 GET 和 POST 请求方法
    - 支持 HTTP/1.1 协议
    - 实现长连接机制

2. **路由与接口**
    - 静态资源访问（HTML、TXT、图片等）
    - 用户注册接口：`POST /register`（参数：username、password）
    - 用户登录接口：`POST /login`（参数：username、password）
    - 重定向示例：`GET /old`（301 永久重定向到 /new.html）、`GET /temp`（302 临时重定向到 /temp-new.html）

3. **缓存机制**
    - 基于 `Last-Modified` 和 `If-Modified-Since` 实现 304 缓存功能
    - 静态资源自动添加缓存头

4. **错误处理**
    - 404 Not Found（资源不存在）
    - 400 Bad Request（请求参数错误）
    - 401 Unauthorized（登录失败）
    - 405 Method Not Allowed（不支持的请求方法）
    - 409 Conflict（用户名已存在）
    - 500 Internal Server Error（服务器内部错误）

### 客户端功能

1. 支持发送 GET 和 POST 请求
2. 自动处理 301/302 重定向（最大重定向次数：3次）
3. 实现缓存机制（记录资源的 Last-Modified 时间）
4. 命令行交互界面，方便输入请求参数

## 使用方法

### 启动服务器

1. 运行 `server.HttpServer` 类的 `main` 方法
2. 服务器默认在 8080 端口启动，控制台会输出 "HTTP Server started on port: 8080"

### 使用客户端

1. 运行 `client.HttpClient` 类的 `main` 方法
2. 按照命令行提示输入：
    - 请求方法（GET/POST，输入 q 退出）
    - 请求 URL（例如：http://localhost:8080/index.html）
    - 若为 POST 请求，还需输入请求参数（格式：username=xxx&password=xxx）

### 示例操作

1. **访问静态资源（200/304）**
   ```
   请输入请求方法 (GET/POST): GET
   请输入URL (例如: http://localhost:8080/index.html): http://localhost:8080/index.html
   ```
   客户端预期输出：
   ```
   发送请求到: http://localhost:8080/test.txt

   === 响应结果 ===
   状态码: 200 OK
   响应头: {Connection=keep-alive, Last-Modified=1763360580370, Content-Type=text/plain; charset=UTF-8}
   响应体:
   MAN WHAT CAN I SAY MAMBA OUT
   ```
   再次发送相同请求，触发304，预期输出：
   ```
   发送请求到: http://localhost:8080/test.txt

   === 响应结果 ===
   状态码: 304 Not Modified
   响应头: {Connection=keep-alive}
   响应体: 资源未修改，使用本地缓存
   ```
2. **用户注册**
   ```
   请输入请求方法 (GET/POST): POST
   请输入URL (例如: http://localhost:8080/index.html): http://localhost:8080/register
   请输入POST参数 (格式: username=xxx&password=xxx): username=test&password=123456
   ```
   客户端预期输出：
   ```
   发送请求到: http://localhost:8080/register

   === 响应结果 ===
   状态码: 200 OK
   响应头: {Connection=keep-alive, Content-Length=22, Content-Type=text/plain}
   响应体:
   Register Success: test
   ```
3. **用户登录**
   ```
   请输入请求方法 (GET/POST): POST
   请输入URL (例如: http://localhost:8080/index.html): http://localhost:8080/login
   请输入POST参数 (格式: username=xxx&password=xxx): username=test&password=123456
   ```
   客户端预期输出：
   ```
   发送请求到: http://localhost:8080/login

   === 响应结果 ===
   状态码: 200 OK
   响应头: {Connection=keep-alive, Content-Length=27, Content-Type=text/plain}
   响应体:
   Login Success: Welcome test
   ```

4. **测试重定向（301/302）**

   ```
   //301
   请输入请求方法 (GET/POST): GET
   请输入 URL (例如: http://localhost:8080/index.html): http://localhost:8080/old
   ```
   预期输出：
   ```
   发送请求到: http://localhost:8080/old

   === 响应结果 ===
   状态码: 301 Moved Permanently
   响应头: {Connection=keep-alive, Location=/new.html}
   重定向到: /new.html

   发送请求到: http://localhost:8080/new.html

   === 响应结果 ===
   状态码: 200 OK
   响应头: {Connection=keep-alive, Last-Modified=1763359661810, Content-Type=text/html; charset=UTF-8}
   响应体:...
   ```
   ```
   //302
   请输入请求方法 (GET/POST): GET
   请输入 URL (例如: http://localhost:8080/index.html): http://localhost:8080/temp
   ```
   预期输出：
   ```
   发送请求到: http://localhost:8080/temp

   === 响应结果 ===
   状态码: 302 Found
   响应头: {Connection=keep-alive, Location=/temp-new.html}
   重定向到: /temp-new.html

   发送请求到: http://localhost:8080/temp-new.html

   === 响应结果 ===
   状态码: 200 OK
   响应头: {Connection=keep-alive, Last-Modified=1763359711484, Content-Type=text/html; charset=UTF-8}
   响应体:...
   ```
5. **访问不存在页面（404）**
   ```
   请输入请求方法 (GET/POST): GET
   请输入 URL (例如: http://localhost:8080/index.html): http://localhost:8080/nonexist.html
   ```
   客户端预期结果：
   ```
   发送请求到: http://localhost:8080/nonexist.html

   === 响应结果 ===
   状态码: 404 Not Found
   响应头: {Connection=keep-alive, Content-Length=34, Content-Type=text/plain}
   响应体:
   Resource Not Found: /nonexist.html
   ```
6. **使用不支持的方法（405）**
   ```
   请输入请求方法 (GET/POST): POST
   请输入URL (例如: http://localhost:8080/index.html): http://localhost:8080/test.txt
   请输入POST参数 (格式: username=xxx&password=xxx): username=xxx&password=xxx
   ```
   客户端预期结果：
   ```
   发送请求到: http://localhost:8080/test.txt

   === 响应结果 ===
   状态码: 405 Method Not Allowed
   响应头: {Connection=keep-alive, Content-Length=23, Content-Type=text/plain}
   响应体:
   Method POST Not Allowed
   ```
7. **服务器内部错误（500）**
   例如在handleRegister方法中直接抛出RuntimeException，并再次向/register发送请求
   ```
   //handleRegister方法:
   private void handleRegister(String body) {
        throw new RuntimeException("This is a test for 500 Internal Server Error!");
   }
   ```
   ```
   （handleRegister方法抛出RuntimeException）
   请输入请求方法 (GET/POST): POST
   请输入URL (例如: http://localhost:8080/index.html): http://localhost:8080/register
   请输入POST参数 (格式: username=xxx&password=xxx): username=test&password=123456
   ```
   预期结果：
   ```
   发送请求到: http://localhost:8080/register

   === 响应结果 ===
   状态码: 500 Internal Server Error
   响应头: {Connection=keep-alive, Content-Length=59, Content-Type=text/plain}
   响应体:
   Server Error: This is a test for 500 Internal Server Error!
   ```
## 页面说明

- **首页（index.html）**：提供功能导航，包含：
- 静态资源测试（test.txt、刷新页面、404测试）
- 用户功能测试（注册页面、登录页面）
- 重定向测试（301永久重定向、302临时重定向）
- **new.html**：301重定向目标页面，显示永久迁移提示
- **temp-new.html**：302重定向目标页面，显示临时访问提示