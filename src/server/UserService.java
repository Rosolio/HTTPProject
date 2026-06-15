package server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class UserService {
    // 内存存储：username → password (加密后)
    private final Map<String, String> userMap = new HashMap<>();
    private final Object lock = new Object(); // 用于线程安全

    // 注册：返回true表示成功，false表示用户名已存在
    public boolean register(String username, String password) {
        synchronized (lock) {
            if (userMap.containsKey(username)) {
                return false;
            }
            String encryptedPassword = encryptPassword(password);
            userMap.put(username, encryptedPassword);
            return true;
        }
    }

    // 登录：返回true表示验证通过
    public boolean login(String username, String password) {
        synchronized (lock) {
            String storedPassword = userMap.get(username);
            if (storedPassword == null) {
                return false;
            }
            String encryptedPassword = encryptPassword(password);
            return encryptedPassword.equals(storedPassword);
        }
    }
    
    // 更新用户密码：返回true表示成功，false表示用户不存在
    public boolean updateUser(String username, String newPassword) {
        synchronized (lock) {
            if (!userMap.containsKey(username)) {
                return false;
            }
            String encryptedPassword = encryptPassword(newPassword);
            userMap.put(username, encryptedPassword);
            return true;
        }
    }
    
    // 删除用户：返回true表示成功，false表示用户不存在
    public boolean deleteUser(String username) {
        synchronized (lock) {
            return userMap.remove(username) != null;
        }
    }
    
    // 获取用户列表（用于调试）
    public String getUserList() {
        synchronized (lock) {
            if (userMap.isEmpty()) {
                return "No users registered";
            }
            StringBuilder sb = new StringBuilder("Registered users:\n");
            for (String username : userMap.keySet()) {
                sb.append("- ").append(username).append("\n");
            }
            return sb.toString();
        }
    }
    
    // 密码加密：使用SHA-256哈希算法
    private String encryptPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available, but handle anyway
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}