package com.blink.framework.openfeign.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feign 配置属性类：外部可通过 feign.custom.xxx 覆盖默认值
 */
@ConfigurationProperties(prefix = "blink.feign")
public class FeignCustomProperties {
    /** 是否启用自定义配置 */
    private boolean enabled = true;

    /** 全局超时配置 */
    private Timeout timeout = new Timeout();

    /** 全局日志配置 */
    private Log log = new Log();

    /** 全局拦截器配置 */
    private Interceptor interceptor = new Interceptor();

    // 内部类：超时配置
    public static class Timeout {
        /** 连接超时（毫秒） */
        private int connect = 3000;
        /** 读取超时（毫秒） */
        private int read = 5000;

        // getter/setter
        public int getConnect() {
            return connect;
        }

        public void setConnect(int connect) {
            this.connect = connect;
        }

        public int getRead() {
            return read;
        }

        public void setRead(int read) {
            this.read = read;
        }
    }

    // 内部类：日志配置
    public static class Log {
        /** 日志级别：NONE/BASIC/HEADERS/FULL */
        private String level = "BASIC";

        // getter/setter
        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    // 内部类：拦截器配置
    public static class Interceptor {
        /** 是否启用 traceId 传递拦截器 */
        private boolean traceIdEnabled = true;
        /** 是否启用 token 传递拦截器 */
        private boolean tokenEnabled = true;

        // getter/setter
        public boolean isTraceIdEnabled() {
            return traceIdEnabled;
        }

        public void setTraceIdEnabled(boolean traceIdEnabled) {
            this.traceIdEnabled = traceIdEnabled;
        }

        public boolean isTokenEnabled() {
            return tokenEnabled;
        }

        public void setTokenEnabled(boolean tokenEnabled) {
            this.tokenEnabled = tokenEnabled;
        }
    }

    // 顶层 getter/setter
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public Interceptor getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }
}