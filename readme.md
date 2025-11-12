# HTTPProject 说明文档

## 项目简介
HTTPProject 是一个基于 Java 实现的简易 HTTP 服务器与客户端程序，支持基本的 HTTP 协议交互，包括 GET/POST 请求处理、静态资源访问、用户注册登录、重定向以及缓存机制等功能。

## 项目结构
```
HTTPProject/
├── src/
│   ├── server/           # HTTP 服务器相关类
│   │   ├── HttpServer.java       # 服务器主类，负责监听端口和接收连接
│   │   ├── RequestHandler.java   # 处理客户端请求的核心类
│   │   ├── UserService.java      # 用户注册登录服务（内存存储）
│   │   ├── MimeUtils.java        # MIME 类型工具类
│   │   ├── HttpResponse.java     # 响应相关（预留）
│   ├── client/           # HTTP 客户端相关类
│   │   ├── HttpClient.java       # 客户端主类，支持发送GET/POST请求
│   ├── common/           # 公共常量（预留）
│   │   ├── HttpConstants.java
├── .gitignore            # Git 忽略文件配置
├── .idea/                # IDEA 项目配置
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
1. **访问静态资源**
   ```
   请输入请求方法 (GET/POST): GET
   请输入URL (例如: http://localhost:8080/index.html): http://localhost:8080/index.html
   ```

2. **用户注册**
   ```
   请输入请求方法 (GET/POST): POST
   请输入URL (例如: http://localhost:8080/index.html): http://localhost:8080/register
   请输入POST参数 (格式: username=xxx&password=xxx): username=test&password=123456
   ```

3. **用户登录**
   ```
   请输入请求方法 (GET/POST): POST
   请输入URL (例如: http://localhost:8080/index.html): http://localhost:8080/login
   请输入POST参数 (格式: username=xxx&password=xxx): username=test&password=123456
   ```

## 注意事项
1. 静态资源需放在项目根目录下的 `static` 文件夹中
2. 用户数据存储在内存中，服务器重启后数据会丢失
3. 客户端与服务器通信默认使用短连接，服务器支持长连接
4. 支持的 MIME 类型包括：html、txt、jpg、png，其他类型默认使用 `application/octet-stream`