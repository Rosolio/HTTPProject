package server;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RequestHandler implements Runnable {
    private final Socket clientSocket;
    private final UserService userService;
    private final int connectionId;
    private OutputStream dataOut;

    public RequestHandler(Socket socket, UserService userService, int connectionId) {
        this.clientSocket = socket;
        this.userService = userService;
        this.connectionId = connectionId;
    }

    @Override
    public void run() {
        // 1. 将 headers 定义在 try 外部，并初始化默认值
        Map<String, String> headers = new HashMap<>();

        try {
            // 按字节解析请求，避免 Content-Length/字符集不一致导致 POST/中文乱码
            InputStream input = new BufferedInputStream(clientSocket.getInputStream());
            dataOut = clientSocket.getOutputStream();

            // 解析HTTP请求行（第一行：Method Path Protocol）
            String requestLine = readLine(input);
            if (requestLine == null) return;
            String[] reqParts = requestLine.split(" ");
            String method = reqParts[0]; // GET/POST
            String path = reqParts.length > 1 ? reqParts[1] : "/";   // 请求路径（如/、/login、/register）
            String protocol = reqParts.length > 2 ? reqParts[2] : "HTTP/1.1"; // HTTP/1.1

            // 解析请求头（提取Connection判断长连接）
            // 注意：此处复用外部定义的 headers，不再重新声明
            String headerLine;
            while ((headerLine = readLine(input)) != null && !headerLine.isEmpty()) {
                int idx = headerLine.indexOf(':');
                if (idx > 0) {
                    String name = headerLine.substring(0, idx).trim().toLowerCase(Locale.ROOT);
                    String value = headerLine.substring(idx + 1).trim();
                    headers.put(name, value);
                }
            }

            // 处理POST请求体（注册/登录需获取表单数据）
            String requestBody = "";
            if ("POST".equals(method) && headers.containsKey("content-length")) {
                long contentLength = Long.parseLong(headers.get("content-length"));
                byte[] bodyBytes = readFixedBytes(input, contentLength);
                requestBody = new String(bodyBytes, StandardCharsets.UTF_8);
            }

            // 路由请求（接口/静态资源）
            handleRoute(method, path, protocol, headers, requestBody);

        } catch (Exception e) {
            // 500 服务器内部错误
            System.err.println("Connection #" + connectionId + " error: " + e.getMessage());
            sendResponse("HTTP/1.1", 500, "Internal Server Error", "text/plain", "Server Error: " + e.getMessage(), -1);
        } finally {
            try {
                // 长连接判断：此时 headers 已在外部初始化，可安全访问
                if (!"keep-alive".equalsIgnoreCase(headers.getOrDefault("connection", "close"))) {
                    clientSocket.close();
                    System.out.println("Connection #" + connectionId + " closed");
                }
            } catch (IOException e) {
                System.err.println("Error closing connection #" + connectionId + ": " + e.getMessage());
            }
            try {
                if (dataOut != null) dataOut.close();
            } catch (IOException e) {
                System.err.println("Error closing output stream for connection #" + connectionId + ": " + e.getMessage());
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
        
        // 用户更新接口：PUT /user
        if ("PUT".equals(method) && "/user".equals(path)) {
            handleUpdateUser(body);
            return;
        }
        
        // 用户删除接口：DELETE /user
        if ("DELETE".equals(method) && "/user".equals(path)) {
            handleDeleteUser(body);
            return;
        }
        
        // 用户列表接口：GET /users
        if ("GET".equals(method) && "/users".equals(path)) {
            handleGetUsers();
            return;
        }
        
        // 静态资源请求（如GET /index.html、/image.jpg）
        handleStaticResource(method, path, protocol, headers);
    }

    // 处理注册：解析表单（username=xxx&password=xxx），内存存储用户
    private void handleRegister(String body) {
        //test code 500
        //throw new RuntimeException("This is a test for 500 Internal Server Error!");
        Map<String, String> params = parseFormParams(body);
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            sendResponse("HTTP/1.1", 400, "Bad Request", "text/plain", "Username and Password are required", -1);
            return;
        }
        if (userService.register(username, password)) {
            sendResponse("HTTP/1.1", 200, "OK", "text/plain", "Register Success: " + username, -1);
        } else {
            sendResponse("HTTP/1.1", 409, "Conflict", "text/plain", "Username already exists: " + username, -1);
        }
    }

    // 处理登录：验证用户名密码
    private void handleLogin(String body) {
        Map<String, String> params = parseFormParams(body);
        String username = params.get("username");
        String password = params.get("password");

        if (userService.login(username, password)) {
            sendResponse("HTTP/1.1", 200, "OK", "text/plain", "Login Success: Welcome " + username, -1);
        } else {
            sendResponse("HTTP/1.1", 401, "Unauthorized", "text/plain", "Login Failed: Invalid username or password", -1);
        }
    }
    
    // 处理用户更新：PUT /user
    private void handleUpdateUser(String body) {
        Map<String, String> params = parseFormParams(body);
        String username = params.get("username");
        String newPassword = params.get("newPassword");
        
        if (username == null || newPassword == null || username.isEmpty() || newPassword.isEmpty()) {
            sendResponse("HTTP/1.1", 400, "Bad Request", "text/plain", "Username and newPassword are required", -1);
            return;
        }
        
        if (userService.updateUser(username, newPassword)) {
            sendResponse("HTTP/1.1", 200, "OK", "text/plain", "User updated successfully: " + username, -1);
        } else {
            sendResponse("HTTP/1.1", 404, "Not Found", "text/plain", "User not found: " + username, -1);
        }
    }
    
    // 处理用户删除：DELETE /user
    private void handleDeleteUser(String body) {
        Map<String, String> params = parseFormParams(body);
        String username = params.get("username");
        
        if (username == null || username.isEmpty()) {
            sendResponse("HTTP/1.1", 400, "Bad Request", "text/plain", "Username is required", -1);
            return;
        }
        
        if (userService.deleteUser(username)) {
            sendResponse("HTTP/1.1", 200, "OK", "text/plain", "User deleted successfully: " + username, -1);
        } else {
            sendResponse("HTTP/1.1", 404, "Not Found", "text/plain", "User not found: " + username, -1);
        }
    }
    
    // 处理获取用户列表：GET /users
    private void handleGetUsers() {
        String userList = userService.getUserList();
        sendResponse("HTTP/1.1", 200, "OK", "text/plain", userList, -1);
    }

    // 处理静态资源（支持304缓存、404不存在、MIME类型）
    private void handleStaticResource(String method, String path, String protocol, Map<String, String> headers) throws IOException {
        // 支持GET和HEAD请求获取静态资源，其他方法返回405
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            sendResponse(protocol, 405, "Method Not Allowed", "text/plain", "Method " + method + " Not Allowed", -1);
            return;
        }

        // 根路径默认指向index.html
        if ("/".equals(path)) path = "/index.html";
        // 静态资源存放路径（项目根目录下的static文件夹）
        // 额外做一次路径规范化，避免 ../ 路径穿越读取静态目录外文件
        File staticRoot = new File("static").getCanonicalFile();
        File requested = new File(staticRoot, path.startsWith("/") ? path.substring(1) : path).getCanonicalFile();
        if (!requested.getPath().startsWith(staticRoot.getPath())) {
            sendResponse(protocol, 403, "Forbidden", "text/plain", "Forbidden", -1);
            return;
        }
        File resourceFile = requested;

        // 404：资源不存在
        if (!resourceFile.exists() || !resourceFile.isFile()) {
            sendResponse(protocol, 404, "Not Found", "text/plain", "Resource Not Found: " + path, -1);
            return;
        }

        // 304：资源未修改（根据If-Modified-Since判断）
        String ifModifiedSince = headers.get("if-modified-since");
        long lastModified = resourceFile.lastModified();
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            try {
                long clientLastModified = Long.parseLong(ifModifiedSince); // 直接转毫秒
                if (lastModified <= clientLastModified) {
                    sendResponse(protocol, 304, "Not Modified", null, null, -1);
                    return;
                }
            } catch (NumberFormatException e) {
                // 客户端传的不是数字，忽略缓存逻辑
                System.err.println("Invalid If-Modified-Since (not a number): " + ifModifiedSince);
            }
        }


        // 200：返回资源（带MIME类型、Last-Modified头）
        String mimeType = MimeUtils.getMimeType(path);
        sendStaticFileHeaders(protocol, 200, "OK", mimeType, resourceFile.length(), lastModified);
        
        // 如果是HEAD方法，只返回头部不返回内容
        if ("HEAD".equals(method)) {
            return;
        }
        
        // 写入资源文件内容（按字节流式输出）
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
    private void sendResponse(String protocol, int statusCode, String statusMsg, String mimeType, String body, long lastModified) {
        byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);

        // 1. 响应状态行
        StringBuilder response = new StringBuilder();
        response.append(protocol).append(" ").append(statusCode).append(" ").append(statusMsg).append("\r\n");

        // 2. 响应头（长连接、MIME类型、内容长度等）
        response.append("Connection: keep-alive\r\n"); // 支持长连接
        if (mimeType != null) {
            response.append("Content-Type: ").append(mimeType).append("\r\n");
        }
        // 始终带 Content-Length，方便客户端按字节读取（包含 body=null 的情况）
        response.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        // 静态资源添加Last-Modified头（用于304判断）
        if (lastModified != -1) {
            response.append("Last-Modified: ").append(lastModified).append("\r\n"); // 纯数字毫秒
        }


        // 3. 空行（分隔头与体）
        response.append("\r\n");

        // 4. 响应体（如存在）
        byte[] headerBytes = response.toString().getBytes(StandardCharsets.UTF_8);
        try {
            dataOut.write(headerBytes);
            if (bodyBytes.length > 0) {
                dataOut.write(bodyBytes);
            }
            dataOut.flush();
        } catch (IOException e) {
            // 如果发送失败，这里只记录并让线程结束
            e.printStackTrace();
        }
    }

    private void sendStaticFileHeaders(String protocol, int statusCode, String statusMsg,
                                        String mimeType, long contentLength, long lastModified) throws IOException {
        StringBuilder response = new StringBuilder();
        response.append(protocol).append(" ").append(statusCode).append(" ").append(statusMsg).append("\r\n");
        response.append("Connection: keep-alive\r\n");
        if (mimeType != null) {
            response.append("Content-Type: ").append(mimeType).append("\r\n");
        }
        response.append("Content-Length: ").append(contentLength).append("\r\n");
        if (lastModified != -1) {
            response.append("Last-Modified: ").append(lastModified).append("\r\n");
        }
        response.append("\r\n");
        dataOut.write(response.toString().getBytes(StandardCharsets.UTF_8));
        dataOut.flush();
    }

    // 解析POST表单参数（username=xxx&password=xxx → Map）
    private Map<String, String> parseFormParams(String body) {
        Map<String, String> params = new HashMap<>();
        if (body.isEmpty()) return params;
        String[] pairs = body.split("&", -1);
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] keyVal = pair.split("=", 2);
            String rawKey = keyVal.length >= 1 ? keyVal[0] : "";
            String rawVal = keyVal.length == 2 ? keyVal[1] : "";
            String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
            String val = URLDecoder.decode(rawVal, StandardCharsets.UTF_8);
            params.put(key, val);
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
        response.append("Content-Length: 0\r\n");
        response.append("\r\n"); // 空行结束头部分

        try {
            dataOut.write(response.toString().getBytes(StandardCharsets.UTF_8));
            dataOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 读取一行（以 CRLF 结束），并按 ISO-8859-1 解码为字符串
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(128);
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b == '\r') continue; // 过滤掉 CR
            buffer.write(b);
        }
        if (b == -1 && buffer.size() == 0) return null;
        return buffer.toString(StandardCharsets.ISO_8859_1);
    }

    private static byte[] readFixedBytes(InputStream in, long length) throws IOException {
        if (length <= 0) return new byte[0];
        if (length > Integer.MAX_VALUE) {
            throw new IOException("Content-Length too large: " + length);
        }
        int remaining = (int) length;
        byte[] data = new byte[remaining];
        int offset = 0;
        while (offset < remaining) {
            int read = in.read(data, offset, remaining - offset);
            if (read == -1) throw new EOFException("Unexpected EOF while reading request body");
            offset += read;
        }
        return data;
    }
}