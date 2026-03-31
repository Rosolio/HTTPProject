package client;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Locale;
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
            System.out.print("请输入URL (例如: http://localhost:8007/index.html): ");
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
            // 包含端口（如 localhost:8007/path 或 localhost:8007）
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

    // 新增一个重载方法，用于处理相对路径（基于当前URL的主机和端口）
    private static UrlInfo parseUrl(String url, UrlInfo currentUrl) {
        // 如果是相对路径（不以http://开头），则基于当前URL补全
        if (!url.startsWith("http://")) {
            String fullUrl;
            if (url.startsWith("/")) {
                // 绝对路径（相对于主机）：/new.html → http://当前主机:端口/new.html
                fullUrl = "http://" + currentUrl.host + ":" + currentUrl.port + url;
            } else {
                // 相对路径（相对于当前路径）：暂不处理，简化为绝对路径逻辑
                fullUrl = "http://" + currentUrl.host + ":" + currentUrl.port + "/" + url;
            }
            return parseUrl(fullUrl);
        }
        // 否则按原逻辑解析完整URL
        return parseUrl(url);
    }

    // 发送HTTP请求并处理响应（包含重定向逻辑）
    private static void sendRequest(String method, UrlInfo urlInfo, String body) throws IOException {
        int redirectCount = 0;
        UrlInfo currentUrl = urlInfo;

        while (redirectCount < MAX_REDIRECT) {
            System.out.println("\n发送请求到: " + currentUrl);
            try (Socket socket = new Socket(currentUrl.host, currentUrl.port)) {
                // 按字节发送/接收，避免 Content-Length 与字符集不一致
                OutputStream rawOut = socket.getOutputStream();
                InputStream rawIn = socket.getInputStream();

                byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);

                // 1. 发送请求行 + 2. 发送请求头（ASCII）
                StringBuilder request = new StringBuilder();
                request.append(method).append(" ").append(currentUrl.path).append(" HTTP/1.1\r\n");
                request.append("Host: ").append(currentUrl.host).append(":").append(currentUrl.port).append("\r\n");
                request.append("Connection: close\r\n"); // 客户端使用短连接
                if ("POST".equals(method)) {
                    request.append("Content-Type: application/x-www-form-urlencoded\r\n");
                    request.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
                }
                // 对GET请求添加缓存验证头（If-Modified-Since）
                if ("GET".equals(method)) {
                    String cacheKey = currentUrl.toString();
                    Long cached = resourceCache.get(cacheKey);
                    if (cached != null) {
                        request.append("If-Modified-Since: ").append(cached).append("\r\n");
                    }
                }
                request.append("\r\n"); // 空行分隔头和体

                rawOut.write(request.toString().getBytes(StandardCharsets.US_ASCII));

                // 3. 发送POST请求体（UTF-8 字节）
                if ("POST".equals(method) && bodyBytes.length > 0) {
                    rawOut.write(bodyBytes);
                }
                rawOut.flush();

                // 4. 接收并解析响应
                ResponseInfo response = parseResponse(rawIn);
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
                    if (response.headers.containsKey("last-modified")) {
                        try {
                            long lastModified = Long.parseLong(response.headers.get("last-modified"));
                            resourceCache.put(currentUrl.toString(), lastModified);
                        } catch (NumberFormatException e) {
                            // 忽略格式错误的Last-Modified
                        }
                    }
                    return;
                }

                // 处理301/302（重定向）
                if (response.statusCode == 301 || response.statusCode == 302) {
                    String location = response.headers.get("location");
                    if (location == null || location.isEmpty()) {
                        System.out.println("重定向错误: 未找到Location头");
                        return;
                    }
                    System.out.println("重定向到: " + location);
                    UrlInfo newUrl = parseUrl(location, currentUrl);
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
    private static ResponseInfo parseResponse(InputStream in) throws IOException {
        ResponseInfo response = new ResponseInfo();

        // 1. 解析状态行（如 HTTP/1.1 200 OK）
        String statusLine = readLine(in);
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
        while ((headerLine = readLine(in)) != null && !headerLine.isEmpty()) {
            int idx = headerLine.indexOf(':');
            if (idx > 0) {
                String name = headerLine.substring(0, idx).trim().toLowerCase(Locale.ROOT);
                String value = headerLine.substring(idx + 1).trim();
                response.headers.put(name, value);
            }
        }

        // 3. 解析响应体（根据Content-Length读取）
        byte[] bodyBytes;
        if (response.headers.containsKey("content-length")) {
            try {
                long contentLength = Long.parseLong(response.headers.get("content-length"));
                bodyBytes = readFixedBytes(in, contentLength);
            } catch (NumberFormatException e) {
                // 忽略格式错误的Content-Length，尝试读取所有内容
                bodyBytes = readRemainingBytes(in);
            }
        } else {
            // 无Content-Length时读取到流结束
            bodyBytes = readRemainingBytes(in);
        }

        // 4. 按内容类型决定如何展示响应体（文本/二进制）
        if (bodyBytes.length == 0) {
            response.body = "";
        } else {
            String contentType = response.headers.get("content-type");
            boolean looksLikeText = false;
            if (contentType != null) {
                String ct = contentType.toLowerCase(Locale.ROOT);
                looksLikeText = ct.startsWith("text/") || ct.contains("application/json") || ct.contains("application/xml")
                        || ct.contains("text/html") || ct.contains("application/javascript");
            }

            if (looksLikeText) {
                Charset charset = extractCharset(contentType);
                response.body = new String(bodyBytes, charset != null ? charset : StandardCharsets.UTF_8);
            } else {
                response.body = "[Binary response: " + bodyBytes.length + " bytes]";
            }
        }

        return response;
    }

    private static Charset extractCharset(String contentType) {
        if (contentType == null) return null;
        // Content-Type: text/html; charset=UTF-8
        String[] parts = contentType.split(";");
        for (String part : parts) {
            String p = part.trim().toLowerCase(Locale.ROOT);
            if (p.startsWith("charset=")) {
                String charsetName = part.trim().substring("charset=".length()).trim();
                try {
                    return Charset.forName(charsetName);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
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
            if (read == -1) throw new EOFException("Unexpected EOF while reading response body");
            offset += read;
        }
        return data;
    }

    // 读取输入流剩余内容（用于无Content-Length的情况）
    private static byte[] readRemainingBytes(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toByteArray();
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