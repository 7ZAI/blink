package com.blink.gateway.filter;

import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.IPAddressUtils;
import com.blink.gateway.config.prop.BlinkGatewayConfigProperties;
import com.blink.gateway.util.GateWayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.blink.gateway.constant.GateWayErrMsgCode.FORBIDDEN;
import static com.blink.gateway.constant.GateWayErrMsgCode.ILLEGAL_REQUEST;

/**
 * ip黑名单 白名单 过滤 支持ipv4 ipv6 支持网段过滤
 * 白名单优先 黑名单
 *
 * @Author binblink
 */
@Slf4j
public class IpFilter implements GlobalFilter, Ordered {

    private final BlinkGatewayConfigProperties.IPFilter config;

    public IpFilter(BlinkGatewayConfigProperties config) {
        this.config = config.getIpFilter();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        //获取真实ip
        String clientIp = extractClientIp(request);

        // 验证 IP 地址格式
        if (!IPAddressUtils.isIPv6Valid(clientIp) && !IPAddressUtils.isIPv4Valid(clientIp)) {
            log.warn("无效的 IP 地址: {}", clientIp);
            return Mono.error(new BlinkException(ILLEGAL_REQUEST));
        }

        //后续使用
        exchange.getAttributes().put("clientIp",clientIp);

        // 检查是否启用 IP 过滤
        if (!config.isIpFilterEnable()) {
            return chain.filter(exchange);
        }
        // 检查是否支持 IPv6
//        if (GateWayUtil.isIPv6(clientIp) ) {
//            log.debug("IPv6 地址被拒绝，因为 IPv6 支持未启用: {}", clientIp);
//            return Mono.error(new BlinkException(FORBIDDEN));
//        }

        // 检查白名单（如果白名单不为空，则只允许白名单中的 IP 访问）
        if (!config.getWhiteListIps().isEmpty()) {
            if (config.getWhiteListIps().contains(clientIp)) {
                log.debug("IP 地址在白名单中，允许访问: {}", clientIp);
                return chain.filter(exchange);
            } else {
                log.warn("IP 地址不在白名单中被拒绝: {}", clientIp);
                return Mono.error(new BlinkException(FORBIDDEN));
            }
        }

        //白名单为空 检查黑名单
        if (!config.getBlackListIps().isEmpty() && config.getBlackListIps().contains(clientIp)) {
            log.warn("IP 地址在黑名单中被拒绝: {}", clientIp);
            return Mono.error(new BlinkException(FORBIDDEN));
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


    @Override
    public int getOrder() {
        // 设置较高的优先级，确保在其他过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE + 100 ;
    }
}
