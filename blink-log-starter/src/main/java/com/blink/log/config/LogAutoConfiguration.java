package com.blink.log.config;

import com.blink.log.aop.BlinkControllerLogAspect;
import com.blink.log.aop.BlinkDubboLogAspect;
import com.blink.log.aop.LogExecutionAspect;
import com.blink.log.aop.OperationLogAspect;
import com.blink.log.function.LogConverter;
import com.blink.log.function.LogEnabledFunction;
import com.blink.log.function.LogPersistFunction;
import com.blink.log.function.UserInfoProviderFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 日志自动配置类
 * <p>
 * 当项目中引入 blink-log-starter 依赖时自动装配。
 * <p>
 * 业务模块需要实现以下函数式接口（用于入库日志）：
 * <ul>
 *   <li>{@link LogPersistFunction} - 日志持久化（必需）</li>
 *   <li>{@link LogConverter} - 日志转换（必需）</li>
 *   <li>{@link LogEnabledFunction} - 日志开关判断（可选，默认启用）</li>
 *   <li>{@link UserInfoProviderFunction} - 用户信息提供（可选）</li>
 * </ul>
 *
 * @author binblink
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(OperationLogAspect.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(LogProperties.class)
public class LogAutoConfiguration {

    /**
     * 配置操作日志入库切面
     * <p>
     * 只有当 LogPersistFunction 和 LogConverter 都存在时才创建切面。
     * 受 blink.log.record.enabled 配置控制
     *
     * @param logProperties            日志配置属性
     * @param logPersistFunction       日志持久化函数
     * @param logEnabledFunction       日志开关判断函数
     * @param userInfoProviderFunction 用户信息提供函数
     * @param logConverter             日志转换函数
     * @return 操作日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "blink.log.record", name = "enabled", havingValue = "true", matchIfMissing = true)
    @SuppressWarnings("unchecked")
    public <T> OperationLogAspect<T> operationLogAspect(
            LogProperties logProperties,
            LogPersistFunction<T> logPersistFunction,
            LogEnabledFunction logEnabledFunction,
            UserInfoProviderFunction userInfoProviderFunction,
            LogConverter<T> logConverter) {

        log.info("初始化操作日志入库切面 - OperationLogAspect 已启用");

        return new OperationLogAspect<>(
                logProperties,
                logPersistFunction,
                logEnabledFunction,
                userInfoProviderFunction,
                logConverter
        );
    }

    /**
     * 配置 Controller 控制台日志切面
     * <p>
     * 受 blink.log.console.enable-controller-log 配置控制
     *
     * @param logProperties 日志配置属性
     * @return Controller 日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "blink.log.console", name = "enable-controller-log", havingValue = "true", matchIfMissing = true)
    public BlinkControllerLogAspect blinkControllerLogAspect(LogProperties logProperties) {
        log.info("初始化 Controller 控制台日志切面 - BlinkControllerLogAspect 已启用");
        return new BlinkControllerLogAspect(logProperties);
    }

    /**
     * 配置 Dubbo 服务控制台日志切面
     * <p>
     * 受 blink.log.console.enable-dubbo-log 配置控制，默认关闭
     * 需要在配置文件中显式启用：blink.log.console.enable-dubbo-log: true
     *
     * @param logProperties 日志配置属性
     * @return Dubbo 日志切面
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "blink.log.console", name = "enable-dubbo-log", havingValue = "true")
    public BlinkDubboLogAspect blinkDubboLogAspect(LogProperties logProperties) {
        log.info("初始化 Dubbo 服务控制台日志切面 - BlinkDubboLogAspect 已启用");
        return new BlinkDubboLogAspect(logProperties);
    }

    /**
     * 注解@LogExecution 方法日志切面
     *
     * @return
     */
    @Bean
    public LogExecutionAspect logExecutionAspect() {
        return new LogExecutionAspect();

    }

}