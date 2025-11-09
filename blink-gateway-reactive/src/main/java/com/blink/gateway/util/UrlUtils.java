package com.blink.gateway.util;

/**
 * @Author binblink
 * @Date 2025/8/24
 */
import java.net.URI;

public class UrlUtils {

    public static String extractPath(String url) {
        try {
            URI uri = new URI(url);
            // 只取 path
            String path = uri.getPath();
            return path != null ? path : "";
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的URL: " + url, e);
        }
    }


}
