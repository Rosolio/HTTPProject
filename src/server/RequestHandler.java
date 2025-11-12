package server;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class RequestHandler implements Runnable {
    private final Socket clientSocket;
    private final UserService userService;
    private BufferedReader in;
    private PrintWriter out;
    private OutputStream dataOut;

    public RequestHandler(Socket socket, UserService userService) {
        this.clientSocket = socket;
        this.userService = userService;
    }

    @Override
    public void run() {
        // 1. 将 headers 定义在 try 外部，并初始化默认值
        Map<String, String> headers = new HashMap<>();
        BufferedReader in = null;
        PrintWriter out = null;
        OutputStream dataOut = null;

        try {
            // 初始化输入输出流
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            dataOut = clientSocket.getOutputStream();

            // 解析HTTP请求行（第一行：Method Path Protocol）
            String requestLine = in.readLine();
            if (requestLine == null) return;
            String[] reqParts = requestLine.split(" ");
            String method = reqParts[0]; // GET/POST
            String path = reqParts[1];   // 请求路径（如/、/login、/register）
            String protocol = reqParts[2]; // HTTP/1.1

            // 解析请求头（提取Connection判断长连接）
            // 注意：此处复用外部定义的 headers，不再重新声明
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                String[] headerParts = headerLine.split(": ", 2);
                if (headerParts.length == 2) {
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            // 处理POST请求体（注册/登录需获取表单数据）
            String requestBody = "";
            if ("POST".equals(method) && headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars);
                requestBody = new String(bodyChars);
            }

            // 路由请求（接口/静态资源）
            handleRoute(method, path, protocol, headers, requestBody);

        } catch (Exception e) {
            // 500 服务器内部错误
            sendResponse("HTTP/1.1", 500, "Internal Server Error", "text/plain", "Server Error: " + e.getMessage());
        } finally {
            try {
                // 长连接判断：此时 headers 已在外部初始化，可安全访问
                if (!"keep-alive".equalsIgnoreCase(headers.getOrDefault("Connection", "close"))) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 关闭流（避免资源泄漏）
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (out != null) out.close();
            try {
                if (dataOut != null) dataOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 路由处理：区分注册、登录接口与静态资源
    private void handleRoute(String method, String path, String protocol, Map<String, String> headers, String body) throws IOException {
        // 新增：301永久重定向示例（/old → /new）
        if ("GET".equals(method) && "/old".equals(path)) {
            sendRedirect(protocol, 301, "/new.html"); // 永久重定向到/new.html
            return;
        }

        // 新增：302临时重定向示例（/temp → /temp-new）
        if ("GET".equals(method) && "/temp".equals(path)) {
            sendRedirect(protocol, 302, "/temp-new.html"); // 临时重定向到/temp-new.html
            return;
        }
        // 注册接口：POST /register
        if ("POST".equals(method) && "/register".equals(path)) {
            handleRegister(body);
            return;
        }
        // 登录接口：POST /login
        if ("POST".equals(method) && "/login".equals(path)) {
            handleLogin(body);
            return;
        }
        // 静态资源请求（如GET /index.html、/image.jpg）
        handleStaticResource(method, path, protocol, headers);
    }

    // 处理注册：解析表单（username=xxx&password=xxx），内存存储用户
    private void handleRegister(String body) {
        Map<String, String> params = parseFormParams(body);
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            sendResponse("HTTP/1.1", 400, "Bad Request", "text/plain", "Username and Password are required");
            return;
        }
        if (userService.register(username, password)) {
            sendResponse("HTTP/1.1", 200, "OK", "text/plain", "Register Success: " + username);
        } else {
            sendResponse("HTTP/1.1", 409, "Conflict", "text/plain", "Username already exists: " + username);
        }
    }

    // 处理登录：验证用户名密码
    private void handleLogin(String body) {
        Map<String, String> params = parseFormParams(body);
        String username = params.get("username");
        String password = params.get("password");

        if (userService.login(username, password)) {
            sendResponse("HTTP/1.1", 200, "OK", "text/plain", "Login Success: Welcome " + username);
        } else {
            sendResponse("HTTP/1.1", 401, "Unauthorized", "text/plain", "Login Failed: Invalid username or password");
        }
    }

    // 处理静态资源（支持304缓存、404不存在、MIME类型）
    private void handleStaticResource(String method, String path, String protocol, Map<String, String> headers) throws IOException {
        // 只支持GET请求获取静态资源，其他方法返回405
        if (!"GET".equals(method)) {
            sendResponse(protocol, 405, "Method Not Allowed", "text/plain", "Method " + method + " Not Allowed");
            return;
        }

        // 根路径默认指向index.html
        if ("/".equals(path)) path = "/index.html";
        // 静态资源存放路径（项目根目录下的static文件夹）
        String resourcePath = "static" + path;
        File resourceFile = new File(resourcePath);

        // 404：资源不存在
        if (!resourceFile.exists() || !resourceFile.isFile()) {
            sendResponse(protocol, 404, "Not Found", "text/plain", "Resource Not Found: " + path);
            return;
        }

        // 304：资源未修改（根据If-Modified-Since判断）
        String ifModifiedSince = headers.get("If-Modified-Since");
        long lastModified = resourceFile.lastModified();
        if (ifModifiedSince != null && lastModified <= Long.parseLong(ifModifiedSince)) {
            sendResponse(protocol, 304, "Not Modified", null, null);
            return;
        }

        // 200：返回资源（带MIME类型、Last-Modified头）
        String mimeType = MimeUtils.getMimeType(path);
        sendResponse(protocol, 200, "OK", mimeType, null);
        // 写入资源文件内容
        try (FileInputStream fis = new FileInputStream(resourceFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                dataOut.write(buffer, 0, len);
            }
            dataOut.flush();
        }
    }

    // 构建并发送HTTP响应
    private void sendResponse(String protocol, int statusCode, String statusMsg, String mimeType, String body) {
        // 1. 响应状态行
        StringBuilder response = new StringBuilder();
        response.append(protocol).append(" ").append(statusCode).append(" ").append(statusMsg).append("\r\n");

        // 2. 响应头（长连接、MIME类型、内容长度等）
        response.append("Connection: keep-alive\r\n"); // 支持长连接
        if (mimeType != null) {
            response.append("Content-Type: ").append(mimeType).append("\r\n");
        }
        if (body != null) {
            response.append("Content-Length: ").append(body.getBytes().length).append("\r\n");
        }
        // 静态资源添加Last-Modified头（用于304判断）
        if (statusCode == 200 && mimeType != null && !mimeType.startsWith("text/plain")) {
            response.append("Last-Modified: ").append(System.currentTimeMillis()).append("\r\n");
        }

        // 3. 空行（分隔头与体）
        response.append("\r\n");

        // 4. 响应体（如存在）
        if (body != null) {
            response.append(body);
        }

        // 发送响应
        out.print(response.toString());
        out.flush();
    }

    // 解析POST表单参数（username=xxx&password=xxx → Map）
    private Map<String, String> parseFormParams(String body) {
        Map<String, String> params = new HashMap<>();
        if (body.isEmpty()) return params;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] keyVal = pair.split("=", 2);
            if (keyVal.length == 2) {
                params.put(keyVal[0], keyVal[1]);
            }
        }
        return params;
    }

    /**
     * 发送重定向响应
     *
     * @param protocol   HTTP协议版本（如HTTP/1.1）
     * @param statusCode 状态码（301或302）
     * @param location   重定向目标URL
     */
    private void sendRedirect(String protocol, int statusCode, String location) {
        // 验证状态码合法性
        if (statusCode != 301 && statusCode != 302) {
            throw new IllegalArgumentException("Invalid redirect status code: " + statusCode);
        }

        // 构建响应行和响应头
        StringBuilder response = new StringBuilder();
        response.append(protocol).append(" ").append(statusCode).append(" ")
                .append(statusCode == 301 ? "Moved Permanently" : "Found").append("\r\n");
        response.append("Location: ").append(location).append("\r\n"); // 重定向目标URL
        response.append("Connection: keep-alive\r\n"); // 保持长连接支持
        response.append("\r\n"); // 空行结束头部分

        // 发送响应
        out.print(response.toString());
        out.flush();
    }
}