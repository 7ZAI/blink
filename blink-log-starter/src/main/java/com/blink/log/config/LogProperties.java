package com.blink.log.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志配置属性
 * <p>
 * 配置示例：
 * <pre>
 * blink:
 *   log:
 *     record:
 *       enabled: true
 *       save-request: true
 *       save-response: true
 *     console:
 *       enable-controller-log: true
 *       upper-limit: 1000
 * </pre>
 *
 * @author binblink
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "blink.log")
public class LogProperties {

    /**
     * 入库日志配置
     */
    private LogRecord record = new LogRecord();

    /**
     * 控制台日志配置
     */
    private LogConsole console = new LogConsole();

    /**
     * 入库日志配置
     * <p>
     * 控制操作日志入库相关配置
     */
    @Getter
    @Setter
    public static class LogRecord {

        /**
         * 是否启用日志记录入库
         * <p>
         * 默认启用。即使设置为 false，也可以通过 LogEnabledFunction 覆盖。
         */
        private boolean enabled = true;

        /**
         * 是否记录请求参数
         */
        private boolean saveRequest = true;

        /**
         * 是否记录响应结果
         */
        private boolean saveResponse = true;

        /**
         * 请求参数最大长度
         */
        private int maxRequestLength = 4000;

        /**
         * 响应数据最大长度
         */
        private int maxResponseLength = 4000;

        /**
         * 错误信息最大长度
         */
        private int maxErrorMsgLength = 500;

        /**
         * User-Agent 最大长度
         */
        private int maxUserAgentLength = 500;
    }

    /**
     * 控制台日志配置
     * <p>
     * 控制切面日志输出到控制台相关配置
     */
    @Getter
    @Setter
    public static class LogConsole {

        /**
         * 是否开启 Controller 切面日志
         * <p>
         * 开启则 com.blink 包下的所有 Controller 生效
         */
        private boolean enableControllerLog = true;

        /**
         * 入参日志长度上限
         * <p>
         * 超过该长度的字符将被省略显示为 ...
         */
        private int upperLimit = 1000;

        /**
         * 日志入参超过长度自动省略
         * <p>
         * true - 超过长度自动截断省略
         * false - 默认不省略，完整输出
         */
        private boolean autoSkip = false;

        /**
         * 是否开启敏感数据脱敏
         * <p>
         * 默认关闭，开启后对密码、手机号等敏感信息进行脱敏处理
         */
        private boolean enableSensitive = false;
    }
}