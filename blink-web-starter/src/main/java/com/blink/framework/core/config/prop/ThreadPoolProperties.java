package com.blink.framework.core.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线程池配置
 * @author binblink
 */
@Data
@ConfigurationProperties(prefix = "blink.web.async.thread-pool")
public class ThreadPoolProperties {
    
    private PoolConfig core;
    private PoolConfig io;
    private PoolConfig scheduled;
    
    @Data
    public static class PoolConfig {
        //默认关闭
        private Boolean enabled = false;
        private Integer coreSize;
        private Integer maxSize;
        private Integer queueCapacity;
        private Integer keepAliveSeconds;
        private String threadNamePrefix;
        
        // 是否基于CPU核心数动态计算线程数
        private Boolean dynamicBasedOnCpu = true;
        // 核心线程数倍数（相对于CPU核心数）
        private Double coreMultiplier = 1.0;
        // 最大线程数倍数（相对于CPU核心数）
        private Double maxMultiplier = 2.0;
        // 最小线程数（动态计算时的下限）
        private Integer minCoreSize = 1;
        // 最大线程数上限（动态计算时的上限）
        private Integer maxLimit = 200;
    }
}