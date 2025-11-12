package server;

import java.util.HashMap;
import java.util.Map;

public class MimeUtils {
    private static final Map<String, String> MIME_MAP = new HashMap<>();

    static {
        // 文本类型：html、plain
        MIME_MAP.put(".html", "text/html; charset=UTF-8");
        MIME_MAP.put(".txt", "text/plain; charset=UTF-8");
        // 非文本类型：jpg（图片）
        MIME_MAP.put(".jpg", "image/jpeg");
        MIME_MAP.put(".png", "image/png"); // 额外支持，满足需求
    }

    // 根据文件后缀获取MIME类型，默认返回application/octet-stream
    public static String getMimeType(String path) {
        int lastDotIndex = path.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "application/octet-stream";
        }
        String suffix = path.substring(lastDotIndex).toLowerCase();
        return MIME_MAP.getOrDefault(suffix, "application/octet-stream");
    }
}