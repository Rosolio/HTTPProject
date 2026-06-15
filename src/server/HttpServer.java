package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpServer {
    private static final int DEFAULT_PORT = 8007;
    private static final int DEFAULT_MAX_THREADS = 50;
    
    private static ServerSocket serverSocket;
    private static ExecutorService threadPool;
    private static final UserService userService = new UserService();
    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static ServerGUI gui;
    private static int currentPort;
    private static int currentMaxThreads;

    public static void main(String[] args) {
        // 命令行模式启动
        startServer(DEFAULT_PORT, DEFAULT_MAX_THREADS);
    }
    
    public static void setGUI(ServerGUI gui) {
        HttpServer.gui = gui;
    }
    
    public static void startServer(int port, int maxThreads) {
        if (running.get()) {
            logError("服务器已在运行中");
            return;
        }
        
        currentPort = port;
        currentMaxThreads = maxThreads;
        threadPool = Executors.newFixedThreadPool(maxThreads);
        
        try {
            serverSocket = new ServerSocket(port);
            running.set(true);
            
            logSuccess("HTTP Server started on port: " + port);
            logInfo("Thread pool size: " + maxThreads);
            logInfo("访问 http://localhost:" + port + "/client.html 打开客户端GUI");
            
            // 注册shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stopServer();
            }));
            
            // 更新GUI连接计数
            if (gui != null) {
                gui.updateConnectionCount(0);
            }
            
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int connId = connectionCount.incrementAndGet();
                    
                    logInfo("New connection #" + connId + " from " + 
                           clientSocket.getInetAddress().getHostAddress() + 
                           ":" + clientSocket.getPort());
                    
                    // 更新GUI
                    if (gui != null) {
                        gui.updateConnectionCount(connId);
                    }
                    
                    // 使用线程池处理请求
                    threadPool.execute(new RequestHandler(clientSocket, userService, connId));
                } catch (Exception e) {
                    if (running.get()) {
                        logError("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logError("Server startup failed: " + e.getMessage());
            running.set(false);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }
    
    public static void stopServer() {
        if (!running.get()) {
            return;
        }
        
        running.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            logError("Error closing server socket: " + e.getMessage());
        }
        
        if (threadPool != null) {
            threadPool.shutdown();
        }
        
        logSuccess("Server stopped");
    }
    
    public static boolean isRunning() {
        return running.get();
    }
    
    public static int getConnectionCount() {
        return connectionCount.get();
    }
    
    public static int getPort() {
        return currentPort;
    }
    
    public static int getMaxThreads() {
        return currentMaxThreads;
    }
    
    // 日志方法
    private static void logInfo(String message) {
        if (gui != null) {
            gui.logInfo(message);
        } else {
            System.out.println(message);
        }
    }
    
    private static void logError(String message) {
        if (gui != null) {
            gui.logError(message);
        } else {
            System.err.println(message);
        }
    }
    
    private static void logSuccess(String message) {
        if (gui != null) {
            gui.logSuccess(message);
        } else {
            System.out.println(message);
        }
    }
}