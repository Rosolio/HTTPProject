package server;

import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    private static final int PORT = 8080; // 监听端口
    private static final UserService userService = new UserService(); // 内存用户服务

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("HTTP Server started on port: " + PORT);
            while (true) { // 循环接收客户端连接（BIO）
                Socket clientSocket = serverSocket.accept();
                // 启动线程处理单个请求（支持长连接需保持线程不立即关闭）
                new Thread(new RequestHandler(clientSocket, userService)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}