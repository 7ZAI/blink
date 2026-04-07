package com.blink.gateway.admin.sse;

import cn.hutool.core.net.NetUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SSE 实例标识组件
 * 生成当前实例的唯一标识，用于多实例连接管理
 *
 * @author binblink
 * @since 2026-04-08
 */
@Slf4j
@Component
public class SseInstanceIdentifier {

    /**
     * 当前实例ID
     */
    @Getter
    private String instanceId;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * 初始化实例标识
     */
    @PostConstruct
    public void init() {
        // 实例ID格式：短UUID@IP:Port
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        String localIp = NetUtil.getLocalhostStr();
        this.instanceId = shortUuid + "@" + localIp + ":" + serverPort;
        log.info("[SSE] 实例标识初始化完成 | instanceId: {}", instanceId);
    }
}