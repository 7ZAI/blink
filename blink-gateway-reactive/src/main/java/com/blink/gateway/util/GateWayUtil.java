package com.blink.gateway.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.blink.framework.common.data.SysConfigCacheDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.ReactiveRedisClient;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.blink.gateway.constant.GatewayConstant.*;
import static com.blink.gateway.constant.GatewayConstant.SWITCH_ON;

/**
 * DataBuffer工具类
 *
 * @author binblink
 */
@Slf4j
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
        ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "";
        }

        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }


        return ip;
    }



    /**
     * 提取客户端真实 IP
     * @param remoteAddress 远程地址
     * @param xForwardedFor X-Forwarded-For 头
     * @param xRealIp X-Real-IP 头
     * @return 真实 IP 地址
     */
    public static String extractClientIp(String remoteAddress,
                                         String xForwardedFor,
                                         String xRealIp) {
        String ip = null;

        // 1. 首先检查 X-Forwarded-For
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For 可能有多个IP，第一个是真实IP
            ip = xForwardedFor.split(",")[0].trim();
        }

        // 2. 检查 X-Real-IP
        if ((ip == null || ip.isEmpty()) && xRealIp != null && !xRealIp.isEmpty()) {
            ip = xRealIp.trim();
        }

        // 3. 使用远程地址
        if (ip == null || ip.isEmpty()) {
            ip = remoteAddress;
            // 移除 IPv6 地址的方括号
            if (ip != null && ip.contains(":")) {
                ip = ip.replace("[", "").replace("]", "");
                // 如果是 IPv6 地址且包含端口，移除端口部分
                if (ip.contains(":")) {
                    int lastColonIndex = ip.lastIndexOf(":");
                    if (lastColonIndex != -1) {
                        // 检查是否可能是 IPv6 地址（包含多个冒号）
                        long colonCount = ip.chars().filter(ch -> ch == ':').count();
                        if (colonCount <= 1) {
                            // 可能是 IPv4 地址加端口
                            ip = ip.substring(0, lastColonIndex);
                        }
                    }
                }
            }
        }

        return ip;
    }

    /**
     * 检查 IP 是否在列表中（支持 CIDR）
     * @param ip 要检查的 IP
     * @param ipList IP 列表（支持 CIDR）
     * @return 是否匹配
     */
    public static boolean isIpInList(String ip, List<String> ipList) {
        if (ip == null || ip.isEmpty() || ipList == null || ipList.isEmpty()) {
            return false;
        }

        try {
            IPAddress clientIp = new IPAddressString(ip).toAddress();

            for (String ipPattern : ipList) {
                if (ipPattern == null || ipPattern.isEmpty()) {
                    continue;
                }

                IPAddressString patternString = new IPAddressString(ipPattern);
                // 检查是否是 CIDR 格式
                if (patternString.isPrefixed()) {
                    // CIDR 格式，如 192.168.1.0/24
                    IPAddress pattern = patternString.toAddress();
                    if (pattern != null && pattern.contains(clientIp)) {
                        return true;
                    }
                } else {
                    // 单个 IP 或 IP 范围
                    IPAddress pattern = patternString.toAddress();
                    if (pattern != null) {
                        if (pattern.isMultiple()) {
                            // IP 范围，如 192.168.1.1-192.168.1.100
                            IPAddressSeqRange range = pattern.toSequentialRange();
                            if (range.contains(clientIp)) {
                                return true;
                            }
                        } else {
                            // 单个 IP
                            if (pattern.equals(clientIp)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("IP 地址解析失败: ip={}, error={}", ip, e.getMessage());
        }

        return false;
    }

    /**
     * 验证 IP 地址格式
     * @param ip IP 地址
     * @return 是否有效
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        try {
            new IPAddressString(ip).toAddress();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否是 IPv6 地址
     * @param ip IP 地址
     * @return 是否是 IPv6
     */
    public static boolean isIPv6(String ip) {
        try {
            IPAddress address = new IPAddressString(ip).toAddress();
            return address != null && address.isIPv6();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否是 IPv4 地址
     * @param ip IP 地址
     * @return 是否是 IPv4
     */
    public static boolean isIPv4(String ip) {
        try {
            IPAddress address = new IPAddressString(ip).toAddress();
            return address != null && address.isIPv4();
        } catch (Exception e) {
            return false;
        }
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

    /**
     * 如果Stream和group不存在  则创建 Stream 和消费者组（响应式方式）
     *
     * @param redisClient redis客户端
     * @param streamKey  流key值
     * @param groupName 组名称
     * @return Mono<Boolean> true/false
     */
    public static Mono<Boolean> createStreamAndGroup(ReactiveRedisClient redisClient, String streamKey, String groupName ) {

        // 检查 Stream 是否存在，如果不存在则创建
        return redisClient.xPending(streamKey, groupName)
                //存在
                .map(pms -> {
                    log.info(" stream:{} 和 group:{} 已经存在!", streamKey, groupName);
                    return true;
                })
                //不存在 会报错
                .onErrorResume(e -> {
                    log.error("xPending error", e);
                    log.warn("在stream:{} 中 组:{}不存在", streamKey, groupName);
                    log.info("在stream:{} 中创建组:{}", streamKey, groupName);

                    // createGroup 如果stream 不存在也会创建 再创建组
                    return redisClient.xGroupCreate(streamKey, groupName, "0-0")
                            .map(s -> {
                                log.info("在stream:{} 创建组:{} 创建结果：{}",streamKey, groupName, s);
                                return true;
                            }).switchIfEmpty(Mono.error(new BlinkException("创建组失败")));
                });
    }

}
