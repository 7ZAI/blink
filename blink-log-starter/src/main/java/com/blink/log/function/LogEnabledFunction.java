package com.blink.log.function;

/**
 * 日志开关判断函数式接口
 * <p>
 * 由业务模块实现，返回指定日志类型是否启用。
 * 支持从配置中心、数据库、Redis 等获取开关状态。
 * <p>
 * 使用示例：
 * <pre>
 * &#064;Bean
 * public LogEnabledFunction logEnabledFunction(SysConfigService configService) {
 *     return logType -&gt; {
 *         String configKey = getConfigKeyByType(logType);
 *         return configService.getBooleanConfig(configKey, true);
 *     };
 * }
 * </pre>
 *
 * @author binblink
 */
@FunctionalInterface
public interface LogEnabledFunction {

    /**
     * 判断指定类型的日志是否启用
     *
     * @param logType 日志类型编码，对应 {@link com.blink.log.constant.LogType#getCode()}
     * @return true-启用日志记录 false-禁用日志记录
     */
    boolean isEnabled(String logType);
}