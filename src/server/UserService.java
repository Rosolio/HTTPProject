package server;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    // 内存存储：username → password
    private final Map<String, String> userMap = new HashMap<>();

    // 注册：返回true表示成功，false表示用户名已存在
    public boolean register(String username, String password) {
        if (userMap.containsKey(username)) {
            return false;
        }
        userMap.put(username, password);
        return true;
    }

    // 登录：返回true表示验证通过
    public boolean login(String username, String password) {
        return password != null && password.equals(userMap.get(username));
    }
}