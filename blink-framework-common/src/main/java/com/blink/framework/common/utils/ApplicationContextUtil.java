package com.blink.framework.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 静态方法中获取 Spring 管理的 Bean 的工具类
 * 
 * 通过实现 ApplicationContextAware 接口，在 Spring 容器启动时自动注入 ApplicationContext
 *
 * @author binblink
 */
@Component
public class ApplicationContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static Environment environment;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        ApplicationContextUtil.applicationContext = context;
        ApplicationContextUtil.environment = context.getEnvironment();
    }

    /**
     * 获取 ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        assertContextInjected();
        return applicationContext;
    }

    /**
     * 通过 name 获取 Bean
     */
    public static Object getBean(String name) {
        assertContextInjected();
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过 class 获取 Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        assertContextInjected();
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过 name 和 class 获取指定的 Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        assertContextInjected();
        return getApplicationContext().getBean(name, clazz);
    }

    /**
     * 获取环境配置
     */
    public static Environment getEnvironment() {
        assertContextInjected();
        return environment;
    }

    /**
     * 获取属性值
     */
    public static String getProperty(String key) {
        assertContextInjected();
        return environment.getProperty(key);
    }

    /**
     * 获取属性值，带默认值
     */
    public static String getProperty(String key, String defaultValue) {
        assertContextInjected();
        return environment.getProperty(key, defaultValue);
    }

    /**
     * 获取当前激活的环境
     */
    public static String[] getActiveProfiles() {
        assertContextInjected();
        return environment.getActiveProfiles();
    }

    /**
     * 是否是开发环境
     */
    public static boolean isDev() {
        assertContextInjected();
        return environment.acceptsProfiles(Profiles.of("dev"));
    }

    /**
     * 是否是生产环境
     */
    public static boolean isProd() {
        assertContextInjected();
        return environment.acceptsProfiles(Profiles.of("prod"));
    }

    /**
     * 检查 ApplicationContext 是否已注入
     */
    private static void assertContextInjected() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                    "ApplicationContext 未注入，请确保 ApplicationContextUtil 已被 Spring 扫描并注册为 Bean");
        }
    }
}
