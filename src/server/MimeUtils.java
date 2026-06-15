package server;

import java.util.HashMap;
import java.util.Map;

public class MimeUtils {
    private static final Map<String, String> MIME_MAP = new HashMap<>();

    static {
        // 文本类型
        MIME_MAP.put(".html", "text/html; charset=UTF-8");
        MIME_MAP.put(".htm", "text/html; charset=UTF-8");
        MIME_MAP.put(".txt", "text/plain; charset=UTF-8");
        MIME_MAP.put(".css", "text/css; charset=UTF-8");
        MIME_MAP.put(".js", "application/javascript; charset=UTF-8");
        MIME_MAP.put(".json", "application/json; charset=UTF-8");
        MIME_MAP.put(".xml", "application/xml; charset=UTF-8");
        MIME_MAP.put(".csv", "text/csv; charset=UTF-8");
        MIME_MAP.put(".md", "text/markdown; charset=UTF-8");
        
        // 图片类型
        MIME_MAP.put(".jpg", "image/jpeg");
        MIME_MAP.put(".jpeg", "image/jpeg");
        MIME_MAP.put(".png", "image/png");
        MIME_MAP.put(".gif", "image/gif");
        MIME_MAP.put(".svg", "image/svg+xml");
        MIME_MAP.put(".ico", "image/x-icon");
        MIME_MAP.put(".webp", "image/webp");
        MIME_MAP.put(".bmp", "image/bmp");
        
        // 字体类型
        MIME_MAP.put(".woff", "font/woff");
        MIME_MAP.put(".woff2", "font/woff2");
        MIME_MAP.put(".ttf", "font/ttf");
        MIME_MAP.put(".otf", "font/otf");
        
        // 其他常见类型
        MIME_MAP.put(".pdf", "application/pdf");
        MIME_MAP.put(".zip", "application/zip");
        MIME_MAP.put(".tar", "application/x-tar");
        MIME_MAP.put(".gz", "application/gzip");
        MIME_MAP.put(".mp3", "audio/mpeg");
        MIME_MAP.put(".mp4", "video/mp4");
        MIME_MAP.put(".avi", "video/x-msvideo");
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