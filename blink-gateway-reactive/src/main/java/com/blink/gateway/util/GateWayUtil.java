package com.blink.gateway.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.blink.framework.common.data.SysConfigCacheDO;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.blink.gateway.constant.GatewayConstant.*;
import static com.blink.gateway.constant.GatewayConstant.SWITCH_ON;

/**
 * DataBuffer工具类
 *
 * @author binblink
 */
public class GateWayUtil {


    public static <T> T convertDataBufferToObject(DataBuffer dataBuffer, Class<T> clazz) {
        // 将DataBuffer转换为字节数组
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        // 释放DataBuffer资源
        DataBufferUtils.release(dataBuffer);
        // 将字节数组转换为字符串（假设是JSON）
        String json = new String(bytes, StandardCharsets.UTF_8);
        return JSON.parseObject(bytes, clazz);
    }

    /**
     * 获取客户端IP地址（WebFlux环境）
     *
     * @param request ServerHttpRequest
     * @return 客户端IP地址
     */
    public static String getClientIp(ServerHttpRequest request) {
        String ip = null;

        // 尝试从各种Header中获取IP
        List<String> xff = request.getHeaders().get("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            ip = xff.get(0);
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            List<String> xri = request.getHeaders().get("X-Real-IP");
            if (xri != null && !xri.isEmpty()) {
                ip = xri.get(0);
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "";

            // 处理IPv6本地地址
            if ("0:0:0:0:0:0:0:1".equals(ip)) {
                ip = "127.0.0.1";
            }
        }

        return ip;
    }

    /**
     * 是否应该缓存body
     * 缓存条件：post请求 和contentType 为  application/ json
     *
     * @param httpRequest
     * @return
     */
    public static boolean shouldCacheRequestBody(ServerHttpRequest httpRequest) {
        var currentType = httpRequest.getHeaders().getContentType();
        var method = httpRequest.getMethod();

        return method.matches(HttpMethod.POST.name())
                && MediaType.APPLICATION_JSON.equalsTypeAndSubtype(currentType);

    }

    //参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
    public static Class getConfigValueByType(SysConfigCacheDO config) {

        if (0 == config.getConfigType()) {
            return String.class;
        }

        if (1 == config.getConfigType()) {
            return Integer.class;
        }

        if (2 == config.getConfigType()) {
            return Boolean.class;
        }

        if (3 == config.getConfigType()) {
            return JSONObject.class;
        }

        if (4 == config.getConfigType()) {
            return String[].class;
        }

        //默认类型
        return String.class;
    }

    /**
     * 缓存组件拿不到值 使用默认值
     *
     * @param key 配置参数key值
     * @return
     */
    public static SysConfigCacheDO getDefaultConfig(String key) {

        SysConfigCacheDO sysConfigCacheDO = new SysConfigCacheDO();

        //默认防重发随机数过期时间 10分钟
        if (REQ_NONCE_EXPIRE_TIME_KEY.equals(key)) {
            sysConfigCacheDO.setConfigValue(Long.toString(REQ_NONCE_EXPIRE_TIME));
        }
        //默认防重发请求过期时间5分钟
        if (REQ_TIMESTAMP_EFFECT_TIME_KEY.equals(key)) {
            sysConfigCacheDO.setConfigValue(Long.toString(REQ_DEFAULT_EFFECT_TIME));
        }

        //默认关闭防重放
        if (REQUEST_REPLAY_DEFEND_SWITCH.equals(key)) {
            sysConfigCacheDO.setConfigValue(String.valueOf(SWITCH_OFF));
        }
        //默认开启签名验证
        if (SIGNTURE_SWITCH_KEY.equals(key)) {
            sysConfigCacheDO.setConfigValue(String.valueOf(SWITCH_ON));
        }

        return sysConfigCacheDO;
    }

}
