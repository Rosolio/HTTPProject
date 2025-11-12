package client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HttpClient {
    // 缓存资源的Last-Modified时间（用于304判断）
    private static final Map<String, Long> resourceCache = new HashMap<>();
    private static final Scanner scanner = new Scanner(System.in);
    // 最大重定向次数（防止循环重定向）
    private static final int MAX_REDIRECT = 3;

    public static void main(String[] args) {
        System.out.println("=== HTTP Client (支持GET/POST) ===");
        System.out.println("输入 'q' 退出程序");

        while (true) {
            // 读取用户输入的请求方法
            System.out.print("\n请输入请求方法 (GET/POST): ");
            String method = scanner.nextLine().trim().toUpperCase();
            if ("Q".equals(method)) {
                System.out.println("程序退出");
                break;
            }
            if (!"GET".equals(method) && !"POST".equals(method)) {
                System.out.println("不支持的方法，请使用GET或POST");
                continue;
            }

            // 读取用户输入的URL
            System.out.print("请输入URL (例如: http://localhost:8080/index.html): ");
            String url = scanner.nextLine().trim();
            if (url.isEmpty()) {
                System.out.println("URL不能为空");
                continue;
            }

            // 解析URL获取主机、端口、路径
            UrlInfo urlInfo = parseUrl(url);
            if (urlInfo == null) {
                System.out.println("URL格式错误，请重新输入");
                continue;
            }

            // 处理POST请求体
            String requestBody = "";
            if ("POST".equals(method)) {
                System.out.print("请输入POST参数 (格式: username=xxx&password=xxx): ");
                requestBody = scanner.nextLine().trim();
            }

            // 发送请求并处理响应
            try {
                sendRequest(method, urlInfo, requestBody);
            } catch (Exception e) {
                System.out.println("请求失败: " + e.getMessage());
            }
        }
        scanner.close();
    }

    // 解析URL为 主机名、端口、路径
    private static UrlInfo parseUrl(String url) {
        if (!url.startsWith("http://")) {
            return null;
        }

        String urlWithoutProtocol = url.substring(7); // 去掉"http://"
        String host;
        int port = 80; // 默认端口
        String path = "/"; // 默认路径

        // 分割主机和路径（包含端口的情况）
        int colonIndex = urlWithoutProtocol.indexOf(':');
        int slashIndex = urlWithoutProtocol.indexOf('/');

        if (colonIndex != -1 && (slashIndex == -1 || colonIndex < slashIndex)) {
            // 包含端口（如 localhost:8080/path 或 localhost:8080）
            host = urlWithoutProtocol.substring(0, colonIndex);
            String portAndPath = urlWithoutProtocol.substring(colonIndex + 1);
            if (portAndPath.contains("/")) {
                int portEnd = portAndPath.indexOf('/');
                try {
                    port = Integer.parseInt(portAndPath.substring(0, portEnd));
                } catch (NumberFormatException e) {
                    return null;
                }
                path = portAndPath.substring(portEnd);
            } else {
                try {
                    port = Integer.parseInt(portAndPath);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else if (slashIndex != -1) {
            // 不包含端口但包含路径（如 localhost/path）
            host = urlWithoutProtocol.substring(0, slashIndex);
            path = urlWithoutProtocol.substring(slashIndex);
        } else {
            // 仅主机名（如 localhost）
            host = urlWithoutProtocol;
        }

        return new UrlInfo(host, port, path);
    }

    // 发送HTTP请求并处理响应（包含重定向逻辑）
    private static void sendRequest(String method, UrlInfo urlInfo, String body) throws IOException {
        int redirectCount = 0;
        UrlInfo currentUrl = urlInfo;

        while (redirectCount < MAX_REDIRECT) {
            System.out.println("\n发送请求到: " + currentUrl);
            try (Socket socket = new Socket(currentUrl.host, currentUrl.port)) {
                // 构建请求
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                // 1. 发送请求行
                out.println(method + " " + currentUrl.path + " HTTP/1.1");

                // 2. 发送请求头
                out.println("Host: " + currentUrl.host + ":" + currentUrl.port);
                out.println("Connection: close"); // 客户端使用短连接
                if ("POST".equals(method)) {
                    // POST请求需指定内容类型和长度
                    out.println("Content-Type: application/x-www-form-urlencoded");
                    out.println("Content-Length: " + body.getBytes().length);
                }
                // 对GET请求添加缓存验证头（If-Modified-Since）
                if ("GET".equals(method)) {
                    String cacheKey = currentUrl.toString();
                    if (resourceCache.containsKey(cacheKey)) {
                        out.println("If-Modified-Since: " + resourceCache.get(cacheKey));
                    }
                }
                out.println(); // 空行分隔头和体

                // 3. 发送POST请求体
                if ("POST".equals(method) && !body.isEmpty()) {
                    out.print(body);
                    out.flush();
                }

                // 4. 接收并解析响应
                ResponseInfo response = parseResponse(in);
                System.out.println("\n=== 响应结果 ===");
                System.out.println("状态码: " + response.statusCode + " " + response.statusMsg);
                System.out.println("响应头: " + response.headers);

                // 处理304（未修改）
                if (response.statusCode == 304) {
                    System.out.println("响应体: 资源未修改，使用本地缓存");
                    return;
                }

                // 处理200（成功）
                if (response.statusCode == 200) {
                    System.out.println("响应体:\n" + response.body);
                    // 缓存Last-Modified（用于后续304判断）
                    if (response.headers.containsKey("Last-Modified")) {
                        try {
                            long lastModified = Long.parseLong(response.headers.get("Last-Modified"));
                            resourceCache.put(currentUrl.toString(), lastModified);
                        } catch (NumberFormatException e) {
                            // 忽略格式错误的Last-Modified
                        }
                    }
                    return;
                }

                // 处理301/302（重定向）
                if (response.statusCode == 301 || response.statusCode == 302) {
                    String location = response.headers.get("Location");
                    if (location == null || location.isEmpty()) {
                        System.out.println("重定向错误: 未找到Location头");
                        return;
                    }
                    System.out.println("重定向到: " + location);
                    UrlInfo newUrl = parseUrl(location);
                    if (newUrl == null) {
                        System.out.println("重定向URL格式错误: " + location);
                        return;
                    }
                    currentUrl = newUrl;
                    redirectCount++;
                    continue; // 继续处理重定向
                }

                // 其他状态码（如404、500等）
                System.out.println("响应体:\n" + response.body);
                return;

            } catch (UnknownHostException e) {
                throw new IOException("未知主机: " + currentUrl.host);
            } catch (IOException e) {
                throw new IOException("连接失败: " + e.getMessage());
            }
        }

        // 超过最大重定向次数
        throw new IOException("超过最大重定向次数 (" + MAX_REDIRECT + ")");
    }

    // 解析服务器响应（状态行、响应头、响应体）
    private static ResponseInfo parseResponse(BufferedReader in) throws IOException {
        ResponseInfo response = new ResponseInfo();

        // 1. 解析状态行（如 HTTP/1.1 200 OK）
        String statusLine = in.readLine();
        if (statusLine == null) {
            throw new IOException("无效的响应");
        }
        String[] statusParts = statusLine.split(" ", 3);
        if (statusParts.length >= 2) {
            try {
                response.statusCode = Integer.parseInt(statusParts[1]);
                response.statusMsg = (statusParts.length == 3) ? statusParts[2] : "";
            } catch (NumberFormatException e) {
                throw new IOException("无效的状态码: " + statusParts[1]);
            }
        }

        // 2. 解析响应头
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            String[] headerParts = headerLine.split(": ", 2);
            if (headerParts.length == 2) {
                response.headers.put(headerParts[0], headerParts[1]);
            }
        }

        // 3. 解析响应体（根据Content-Length读取）
        if (response.headers.containsKey("Content-Length")) {
            try {
                int contentLength = Integer.parseInt(response.headers.get("Content-Length"));
                char[] bodyChars = new char[contentLength];
                int bytesRead = in.read(bodyChars, 0, contentLength);
                if (bytesRead > 0) {
                    response.body = new String(bodyChars, 0, bytesRead);
                }
            } catch (NumberFormatException e) {
                // 忽略格式错误的Content-Length，尝试读取所有内容
                response.body = readRemaining(in);
            }
        } else {
            // 无Content-Length时读取到流结束
            response.body = readRemaining(in);
        }

        return response;
    }

    // 读取输入流剩余内容（用于无Content-Length的情况）
    private static String readRemaining(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    // 内部类：封装URL解析结果
    private static class UrlInfo {
        String host;
        int port;
        String path;

        UrlInfo(String host, int port, String path) {
            this.host = host;
            this.port = port;
            this.path = path;
        }

        @Override
        public String toString() {
            return "http://" + host + ":" + port + path;
        }
    }

    // 内部类：封装响应解析结果
    private static class ResponseInfo {
        int statusCode;
        String statusMsg;
        Map<String, String> headers = new HashMap<>();
        String body = "";
    }
}