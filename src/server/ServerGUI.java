package server;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerGUI extends JFrame {
    private static final int DEFAULT_PORT = 8007;
    private static final int DEFAULT_MAX_THREADS = 50;
    
    private JTextField portField;
    private JTextField maxThreadsField;
    private JButton startButton;
    private JButton stopButton;
    private JTextPane logPane;
    private JLabel statusLabel;
    private JLabel connectionCountLabel;
    
    private HttpServer server;
    private Thread serverThread;
    private boolean isRunning = false;
    
    // 日志样式
    private StyledDocument logDocument;
    private Style infoStyle;
    private Style errorStyle;
    private Style successStyle;
    
    public ServerGUI() {
        initStyles();
        initUI();
        setupMenuBar();
        redirectSystemStreams();
    }
    
    private void initStyles() {
        // 样式将在initUI后初始化
    }
    
    private void initUI() {
        setTitle("HTTP Server - 服务器控制台");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        
        // 顶部面板 - 服务器配置
        JPanel topPanel = createConfigPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // 中间面板 - 日志输出
        JPanel centerPanel = createLogPanel();
        add(centerPanel, BorderLayout.CENTER);
        
        // 底部面板 - 状态信息
        JPanel bottomPanel = createStatusPanel();
        add(bottomPanel, BorderLayout.SOUTH);
        
        // 初始化样式
        initLogStyles();
        
        // 添加初始日志
        logMessage("服务器GUI已启动，请配置参数后点击'启动服务器'按钮", infoStyle);
        logMessage("默认端口: " + DEFAULT_PORT + ", 默认线程池大小: " + DEFAULT_MAX_THREADS, infoStyle);
    }
    
    private void initLogStyles() {
        logDocument = logPane.getStyledDocument();
        
        infoStyle = logPane.addStyle("info", null);
        StyleConstants.setForeground(infoStyle, Color.BLACK);
        StyleConstants.setFontSize(infoStyle, 12);
        
        errorStyle = logPane.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, Color.RED);
        StyleConstants.setFontSize(errorStyle, 12);
        StyleConstants.setBold(errorStyle, true);
        
        successStyle = logPane.addStyle("success", null);
        StyleConstants.setForeground(successStyle, new Color(0, 128, 0));
        StyleConstants.setFontSize(successStyle, 12);
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("服务器配置"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 端口配置
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("监听端口:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 10);
        panel.add(portField, gbc);
        
        // 线程池大小
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("线程池大小:"), gbc);
        
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        maxThreadsField = new JTextField(String.valueOf(DEFAULT_MAX_THREADS), 10);
        panel.add(maxThreadsField, gbc);
        
        // 按钮
        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        startButton = new JButton("启动服务器");
        startButton.setBackground(new Color(76, 175, 80));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> startServer());
        
        stopButton = new JButton("停止服务器");
        stopButton.setBackground(new Color(244, 67, 54));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFocusPainted(false);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopServer());
        
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        panel.add(buttonPanel, gbc);
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("服务器日志"));
        
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(250, 250, 250));
        logPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 清除日志按钮
        JButton clearLogButton = new JButton("清除日志");
        clearLogButton.addActionListener(e -> {
            try {
                logDocument.remove(0, logDocument.getLength());
                logMessage("日志已清除", infoStyle);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearLogButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 5));
        
        statusLabel = new JLabel("状态: 未运行");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        panel.add(statusLabel);
        
        connectionCountLabel = new JLabel("连接数: 0");
        connectionCountLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        panel.add(connectionCountLabel);
        
        return panel;
    }
    
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        fileMenu.setMnemonic('F');
        
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.setMnemonic('X');
        exitItem.addActionListener(e -> {
            if (isRunning) {
                stopServer();
            }
            System.exit(0);
        });
        fileMenu.add(exitItem);
        
        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        helpMenu.setMnemonic('H');
        
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
    
    private void showAboutDialog() {
        String message = "HTTP Server GUI\n\n" +
                "基于Java Socket API开发的HTTP服务器\n" +
                "支持HTTP/1.1协议\n" +
                "功能: GET/POST/PUT/DELETE/HEAD请求处理\n" +
                "      静态资源服务、用户管理、重定向、缓存\n\n" +
                "版本: 2.0\n" +
                "作者: 计网第7小组";
        JOptionPane.showMessageDialog(this, message, "关于", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void startServer() {
        if (isRunning) {
            logMessage("服务器已在运行中", errorStyle);
            return;
        }
        
        int port;
        int maxThreads;
        
        try {
            port = Integer.parseInt(portField.getText().trim());
            maxThreads = Integer.parseInt(maxThreadsField.getText().trim());
            
            if (port < 1 || port > 65535) {
                throw new NumberFormatException("端口范围: 1-65535");
            }
            if (maxThreads < 1 || maxThreads > 1000) {
                throw new NumberFormatException("线程池大小: 1-1000");
            }
        } catch (NumberFormatException ex) {
            logMessage("参数错误: " + ex.getMessage(), errorStyle);
            JOptionPane.showMessageDialog(this, "请输入有效的参数!\n端口: 1-65535\n线程池: 1-1000", 
                    "参数错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 禁用配置控件
        portField.setEnabled(false);
        maxThreadsField.setEnabled(false);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        
        // 启动服务器线程
        server = new HttpServer();
        serverThread = new Thread(() -> {
            try {
                HttpServer.setGUI(this);
                HttpServer.startServer(port, maxThreads);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    logMessage("服务器启动失败: " + ex.getMessage(), errorStyle);
                    resetUIState();
                });
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        
        isRunning = true;
        updateStatus("运行中", new Color(0, 128, 0));
        logMessage("服务器启动中，端口: " + port + ", 线程池: " + maxThreads, successStyle);
    }
    
    private void stopServer() {
        if (!isRunning) {
            logMessage("服务器未运行", errorStyle);
            return;
        }
        
        try {
            HttpServer.stopServer();
            isRunning = false;
            updateStatus("已停止", Color.RED);
            logMessage("服务器已停止", successStyle);
            resetUIState();
        } catch (Exception ex) {
            logMessage("停止服务器时出错: " + ex.getMessage(), errorStyle);
        }
    }
    
    private void resetUIState() {
        portField.setEnabled(true);
        maxThreadsField.setEnabled(true);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }
    
    private void updateStatus(String status, Color color) {
        statusLabel.setText("状态: " + status);
        statusLabel.setForeground(color);
    }
    
    public void updateConnectionCount(int count) {
        SwingUtilities.invokeLater(() -> {
            connectionCountLabel.setText("连接数: " + count);
        });
    }
    
    public void logMessage(String message, Style style) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String logEntry = "[" + timestamp + "] " + message + "\n";
                logDocument.insertString(logDocument.getLength(), logEntry, style);
                logPane.setCaretPosition(logDocument.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    public void logInfo(String message) {
        logMessage(message, infoStyle);
    }
    
    public void logError(String message) {
        logMessage(message, errorStyle);
    }
    
    public void logSuccess(String message) {
        logMessage(message, successStyle);
    }
    
    private void redirectSystemStreams() {
        // 重定向System.out和System.err到GUI日志
        PrintStream out = new PrintStream(System.out) {
            @Override
            public void println(String x) {
                logInfo(x);
            }
            
            @Override
            public void print(String x) {
                logInfo(x);
            }
        };
        
        PrintStream err = new PrintStream(System.err) {
            @Override
            public void println(String x) {
                logError(x);
            }
            
            @Override
            public void print(String x) {
                logError(x);
            }
        };
        
        System.setOut(out);
        System.setErr(err);
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
        });
    }
}