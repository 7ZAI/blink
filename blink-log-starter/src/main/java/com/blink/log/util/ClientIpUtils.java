package com.blink.log.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端IP工具类
 * <p>
 * 从 HTTP 请求中获取客户端真实 IP 地址，
 * 支持通过代理、负载均衡等场景。
 *
 * @author binblink
 */
public final class ClientIpUtils {

    private static final String UNKNOWN = "unknown";

    private ClientIpUtils() {
    }

    /**
     * 获取客户端IP地址
     * <p>
     * 支持通过以下请求头获取真实IP：
     * <ul>
     *   <li>X-Forwarded-For</li>
     *   <li>Proxy-Client-IP</li>
     *   <li>WL-Proxy-Client-IP</li>
     *   <li>HTTP_CLIENT_IP</li>
     *   <li>HTTP_X_FORWARDED_FOR</li>
     * </ul>
     *
     * @param request HTTP请求
     * @return IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalidIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (isInvalidIp(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isInvalidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（如：proxy1, proxy2, realIp）
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 判断IP是否无效
     *
     * @param ip IP地址
     * @return true-无效 false-有效
     */
    private static boolean isInvalidIp(String ip) {
        return StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip);
    }
}