package com.blink.job.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务调度配置属性
 *
 * @author binblink
 */
@Data
@ConfigurationProperties(prefix = "blink.job")
public class JobProperties {

    /**
     * 是否启用任务调度
     */
    private Boolean enabled = true;

    /**
     * 日志保留天数
     */
    private Integer logRetentionDays = 30;

    /**
     * 执行线程池大小
     */
    private Integer threadPoolSize = 10;

    /**
     * 告警配置
     */
    private AlarmConfig alarm = new AlarmConfig();

    @Data
    public static class AlarmConfig {

        /**
         * 是否启用告警
         */
        private Boolean enabled = false;

        /**
         * 告警处理器列表
         */
        private String[] handlers = new String[]{"log"};

        /**
         * Webhook 地址（可选）
         */
        private String webhookUrl;
    }
}
