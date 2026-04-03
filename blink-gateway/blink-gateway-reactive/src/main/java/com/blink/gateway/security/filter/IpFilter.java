package com.blink.gateway.security.filter;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.IPAddressUtils;
import com.blink.gateway.config.prop.BlinkGatewayConfigProperties;
import com.blink.gateway.constant.GatewayConstant;
import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.blink.gateway.constant.GateWayErrMsgCode.FORBIDDEN;
import static com.blink.gateway.constant.GateWayErrMsgCode.ILLEGAL_REQUEST;

/**
 * ip黑名单 白名单 过滤 支持ipv4 ipv6 支持网段过滤
 * 白名单优先 黑名单
 *
 * @Author binblink
 */
@Slf4j
public class IpFilter implements WebFilter {

    private final BlinkGatewayConfigProperties.IPFilter config;

    public IpFilter(BlinkGatewayConfigProperties config) {
        this.config = config.getIpFilter();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        //获取真实ip
        String clientIp = extractClientIp(request);

        // 验证 IP 地址格式
        if (!IPAddressUtils.isIPv6Valid(clientIp) && !IPAddressUtils.isIPv4Valid(clientIp)) {
            log.warn("无效的 IP 地址: {}", clientIp);
            return Mono.error(new BlinkException(ILLEGAL_REQUEST));
        }
        //后续使用
        exchange.getAttributes().put(GatewayConstant.CLIENT_IP, clientIp);

        // 检查是否启用 IP 过滤
        if (!config.isIpFilterEnable()) {
            return chain.filter(exchange);
        }
        // 检查是否支持 IPv6
//        if (GateWayUtil.isIPv6(clientIp) ) {
//            log.debug("IPv6 地址被拒绝，因为 IPv6 支持未启用: {}", clientIp);
//            return Mono.error(new BlinkException(FORBIDDEN));
//        }
        // 白名单开启
        if (config.isWhiteListEnable()) {
            //单个
            List<String> whiteListIps = config.getWhiteListIps();
            //网段
            List<String> whiteListNetwork = config.getWhiteListIpRanges();
            // 检查白名单（如果白名单不为空，则只允许白名单中的 IP 访问）
            if (!whiteListIps.isEmpty()) {
                if (whiteListIps.contains(clientIp)) {
                    log.debug("IP 地址在白名单中，允许访问: {}", clientIp);
                    return chain.filter(exchange);
                }
            }

            if (!whiteListNetwork.isEmpty()) {
                for (String network : whiteListNetwork) {
                    if (IPAddressUtils.isIpInNetwork(clientIp, network)) {
                        log.debug("IP 地址在白名单网段中，允许访问: {}", clientIp);
                        return chain.filter(exchange);
                    }
                }
            }
            log.warn("IP 地址不在白名单中被拒绝: {}", clientIp);
            return Mono.error(new BlinkException(FORBIDDEN));
        }

        //黑名单开启
        if (config.isBlackListEnable()) {
            //单个
            List<String> blackListIps = config.getBlackListIps();
            //网段
            List<String> blackListNetwork = config.getBlackListIpRanges();

            //白名单为空 检查黑名单
            if (!blackListIps.isEmpty()) {

                if (blackListIps.contains(clientIp)) {
                    log.warn("IP 地址在黑名单中 拒绝访问: {}", clientIp);
                    return Mono.error(new BlinkException(FORBIDDEN));
                }
            }

            if (!blackListNetwork.isEmpty()) {
                for (String network : blackListNetwork) {
                    if (IPAddressUtils.isIpInNetwork(clientIp, network)) {
                        log.warn("IP 地址在黑名单网段中 拒绝访问: {}", clientIp);
                        return Mono.error(new BlinkException(FORBIDDEN));
                    }
                }
            }
        }

        // 黑白名单都为空或白名单为空且不在黑名单中，允许访问
        return chain.filter(exchange);
    }


    /**
     * 提取客户端真实 IP
     */
    private String extractClientIp(ServerHttpRequest request) {
        String remoteAddress = "";
        if (request.getRemoteAddress() != null) {
            remoteAddress = request.getRemoteAddress().getAddress().getHostAddress();
        }

        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");

        return GateWayUtil.extractClientIp(remoteAddress, xForwardedFor, xRealIp);
    }

}
