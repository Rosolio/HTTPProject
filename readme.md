# 计网第7小组大作业 HTTPProject 说明文档
## 项目简介
HTTPProject 是完全基于 **Java Socket API** 开发的简易 HTTP 服务器与客户端程序（不依赖 Netty 等第三方框架），支持 BIO 模型。核心实现 HTTP 协议基础交互能力，涵盖 GET/POST 请求处理、静态资源访问、用户注册登录、重定向、缓存机制等功能。

## 项目结构
```
HTTPProject/
├── src/
│   ├── server/           # HTTP 服务器相关类
│   │   ├── HttpServer.java       # 服务器主类，负责监听端口和接收连接
│   │   ├── RequestHandler.java   # 处理客户端请求的核心类
│   │   ├── UserService.java      # 用户注册登录服务（内存存储，不持久化）
│   │   ├── MimeUtils.java        # MIME 类型工具类
│   ├── client/           # HTTP 客户端相关类
│   │   ├── HttpClient.java       # 客户端主类，支持发送GET/POST请求
│   ├── common/           # 公共模块（预留）
├── static/               # 静态资源目录
│   ├── index.html        # 测试页面
│   ├── njuLOGO.png       # 测试png格式（非文本MIME类型）
│   ├── new.html          # 301 重定向目标页面
│   ├── temp-new.html     # 302 重定向目标页面
│   ├── login.html        # 登录页面
│   ├── register.html     # 注册页面
├── .gitignore            # Git 忽略文件配置
├── .idea/                # IDEA 项目配置
└── readme.md             # 项目说明文档
```

## 功能说明
### 一、服务器功能
#### 1. 基础开发规范与协议支持
- **无框架依赖**：完全基于 Java Socket API 编写，不使用 Netty 等第三方框架，符合作业底层开发要求。
- **HTTP 协议兼容**：支持 HTTP/1.1 协议，核心实现 **长连接机制**（通过 `Connection: keep-alive` 头维持连接），减少连接建立与关闭的性能开销。
- **请求方法支持**：兼容 GET、POST 两种核心请求方法，可正确解析 GET 路径参数、POST 表单参数（格式：`username=xxx&password=xxx`）。

#### 2. 状态码完整实现（覆盖作业要求的7种）
| 状态码 | 状态描述                | 触发场景与处理逻辑                                                                 |
|--------|-------------------------|-----------------------------------------------------------------------------------|
| 200    | OK（成功）              | 请求正常处理（如静态资源读取成功、注册/登录验证通过），返回响应体与对应 MIME 类型头 |
| 301    | Moved Permanently（永久重定向） | 访问 `GET /old` 时触发，响应头携带 `Location: /new.html`，指引客户端访问新地址     |
| 302    | Found（临时重定向）     | 访问 `GET /temp` 时触发，响应头携带 `Location: /temp-new.html`，临时指引新地址     |
| 304    | Not Modified（未修改）  | 客户端携带 `If-Modified-Since` 请求头，且资源未更新时返回，不返回响应体           |
| 404    | Not Found（资源不存在） | 请求路径对应的静态资源或接口不存在（如 `GET /nonexist.html`）                     |
| 405    | Method Not Allowed（方法不允许） | 对仅支持 GET 的资源发送 POST 请求（如 `POST /test.txt`）                           |
| 500    | Internal Server Error（服务器内部错误） | 服务器处理逻辑异常（如接口代码抛出异常），返回错误信息响应体                       |

#### 3. MIME 类型支持（满足“至少3种+1种非文本”要求）
- 支持类型及对应场景：
   - `text/plain`：文本文件（如 `test.txt`）
   - `text/html`：HTML 页面（如 `index.html`、`new.html`）
   - `image/png`：图片文件（如 `njuLOGO.png`，非文本类型）
- 实现方式：通过 `MimeUtils` 工具类映射文件后缀与 MIME 类型，可灵活扩展更多类型。

#### 4. 业务功能（注册登录接口）
- **数据存储规则**：用户数据仅存储在内存（`UserService` 类的集合中），无需持久化到数据库，符合作业要求。
- **注册接口**：
   - 路径：`POST /register`
   - 参数：`username`（用户名）、`password`（密码）
   - 逻辑：判断用户名是否已存在，存在返回 409 状态码，不存在则添加到内存并返回 200 成功响应。
- **登录接口**：
   - 路径：`POST /login`
   - 参数：`username`（用户名）、`password`（密码）
   - 逻辑：验证用户名与密码是否匹配，匹配返回 200 欢迎信息，不匹配返回 401 未授权响应。

#### 5. 缓存机制（配合304状态码）
- 实现原理：基于 HTTP 协议的 `Last-Modified`（服务器响应头）与 `If-Modified-Since`（客户端请求头）机制。
- 流程：
   1. 客户端首次请求静态资源时，服务器返回 200 状态码，并携带 `Last-Modified`（资源最后修改时间）；
   2. 客户端缓存该时间戳，再次请求时在请求头添加 `If-Modified-Since`；
   3. 服务器对比资源当前修改时间与请求头时间，若未修改则返回 304，客户端直接使用本地缓存。


### 二、客户端功能
#### 1. 基础请求与响应处理
- 支持构建标准 HTTP 请求报文（包含请求行、请求头、请求体），可发送 GET/POST 请求。
- 响应呈现：通过命令行输出完整响应内容，包括状态码、响应头、响应体，满足“呈现响应报文”要求。

#### 2. 专项状态码处理（301/302/304）
- **301/302 重定向处理**：
   1. 检测到响应状态码为 301/302 时，提取响应头 `Location` 字段；
   2. 若 `Location` 为相对路径（如 `/new.html`），自动基于当前请求的主机和端口补全为完整 URL（如 `http://localhost:8007/new.html`）；
   3. 重新向新 URL 发送请求，限制最大重定向次数为 3 次（避免无限循环）。
- **304 未修改处理**：
   - 检测到 304 状态码时，直接输出“资源未修改，使用本地缓存”，终止后续请求流程，不重新获取响应体。

#### 3. 缓存支持
- 本地缓存：通过键值对存储已请求资源的 URL 与对应 `Last-Modified` 时间戳。
- 自动携带请求头：再次请求已缓存资源时，自动在请求头添加 `If-Modified-Since`，触发服务器 304 逻辑。

#### 4. 交互体验
- 命令行引导：运行客户端后，依次提示输入“请求方法（GET/POST，输入 q 退出）”“请求 URL”“POST 参数（若为 POST 请求）”，操作简洁直观。


## 使用方法
### 一、启动服务器
1. 运行 `server.HttpServer` 类的 `main` 方法；
2. 服务器默认在 8007 端口启动，控制台输出 “HTTP Server started on port: 8007” 表示启动成功。

### 二、使用客户端
1. 运行 `client.HttpClient` 类的 `main` 方法；
2. 按命令行提示输入信息：
   - 示例1（GET 请求静态资源）：
     ```
     请输入请求方法 (GET/POST): GET
     请输入URL (例如: http://localhost:8007/index.html): http://localhost:8007/index.html
     ```
   - 示例2（POST 请求注册接口）：
     ```
     请输入请求方法 (GET/POST): POST
     请输入URL (例如: http://localhost:8007/index.html): http://localhost:8007/register
     请输入POST参数 (格式: username=xxx&password=xxx): username=test&password=123456
     ```


## 示例操作（验证核心功能）
### 1. 访问静态资源（200 成功 / 304 缓存）
- **首次请求**（返回 200）：
  ```
  发送请求到: http://localhost:8007/test.txt
  === 响应结果 ===
  状态码: 200 OK
  响应头: {Connection=keep-alive, Last-Modified=1763360580370, Content-Type=text/plain; charset=UTF-8}
  响应体:
  MAN WHAT CAN I SAY MAMBA OUT
  ```
- **再次请求**（返回 304）：
  ```
  发送请求到: http://localhost:8007/test.txt
  === 响应结果 ===
  状态码: 304 Not Modified
  响应头: {Connection=keep-alive}
  响应体: 资源未修改，使用本地缓存
  ```

### 2. 用户注册与登录
- **注册成功**：
  ```
  发送请求到: http://localhost:8007/register
  === 响应结果 ===
  状态码: 200 OK
  响应头: {Connection=keep-alive, Content-Length=22, Content-Type=text/plain}
  响应体:
  Register Success: test
  ```
- **登录成功**：
  ```
  发送请求到: http://localhost:8007/login
  === 响应结果 ===
  状态码: 200 OK
  响应头: {Connection=keep-alive, Content-Length=27, Content-Type=text/plain}
  响应体:
  Login Success: Welcome test
  ```

### 3. 重定向测试（301 / 302）
- **301 永久重定向**：
  ```
  发送请求到: http://localhost:8007/old
  === 响应结果 ===
  状态码: 301 Moved Permanently
  响应头: {Connection=keep-alive, Location=/new.html}
  重定向到: /new.html
  发送请求到: http://localhost:8007/new.html
  === 响应结果 ===
  状态码: 200 OK
  响应头: {Connection=keep-alive, Last-Modified=1763359661810, Content-Type=text/html; charset=UTF-8}
  响应体:...（new.html 页面内容）
  ```
- **302 临时重定向**：
  ```
  发送请求到: http://localhost:8007/temp
  === 响应结果 ===
  状态码: 302 Found
  响应头: {Connection=keep-alive, Location=/temp-new.html}
  重定向到: /temp-new.html
  发送请求到: http://localhost:8007/temp-new.html
  === 响应结果 ===
  状态码: 200 OK
  响应体:...（temp-new.html 页面内容）
  ```

### 4. 错误状态码测试（404 / 405 / 500）
- **404 资源不存在**：
  ```
  发送请求到: http://localhost:8007/nonexist.html
  === 响应结果 ===
  状态码: 404 Not Found
  响应体: Resource Not Found: /nonexist.html
  ```
- **405 方法不允许**：
  ```
  发送请求到: http://localhost:8007/test.txt
  === 响应结果 ===
  状态码: 405 Method Not Allowed
  响应体: Method POST Not Allowed
  ```
- **500 服务器内部错误**（模拟接口抛异常）：
  ```
  发送请求到: http://localhost:8007/register
  === 响应结果 ===
  状态码: 500 Internal Server Error
  响应体: Server Error: This is a test for 500 Internal Server Error!
  ```


## 页面说明
| 页面路径          | 功能描述                                                                 |
|-------------------|--------------------------------------------------------------------------|
| `index.html`      | 首页，提供功能导航（静态资源测试、注册登录入口、重定向测试链接）           |
| `new.html`        | 301 永久重定向目标页面，显示“资源已永久迁移至此页面”提示                  |
| `temp-new.html`   | 302 临时重定向目标页面，显示“资源临时访问此页面”提示                      |
| `login.html`      | 登录页面，提供表单输入框（可配合客户端或 Postman 发送 POST 请求）         |
| `register.html`   | 注册页面，提供表单输入框（可配合客户端或 Postman 发送 POST 请求）         |