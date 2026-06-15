package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer {
    private static final int PORT = 8007; // 监听端口
    private static final int MAX_THREADS = 50; // 最大线程数
    private static final UserService userService = new UserService(); // 内存用户服务
    private static final AtomicInteger connectionCount = new AtomicInteger(0); // 连接计数器

    public static void main(String[] args) {
        // 使用线程池管理线程，提高性能
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("HTTP Server started on port: " + PORT);
            System.out.println("Thread pool size: " + MAX_THREADS);
            
            // 注册shutdown hook，优雅关闭线程池
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                threadPool.shutdown();
                System.out.println("Thread pool shutdown complete.");
            }));
            
            while (true) { // 循环接收客户端连接（BIO）
                try {
                    Socket clientSocket = serverSocket.accept();
                    int connId = connectionCount.incrementAndGet();
                    System.out.println("New connection #" + connId + " from " + 
                                     clientSocket.getInetAddress().getHostAddress() + 
                                     ":" + clientSocket.getPort());
                    
                    // 使用线程池处理请求
                    threadPool.execute(new RequestHandler(clientSocket, userService, connId));
                } catch (Exception e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }
    
    // 获取服务器统计信息
    public static int getConnectionCount() {
        return connectionCount.get();
    }
}