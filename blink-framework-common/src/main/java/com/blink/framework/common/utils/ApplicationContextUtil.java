package com.blink.framework.common.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 *  静态方法中获取 spring 管理的bean的组件
 *
 * @author binblink
 */
public class ApplicationContextUtil  {

    private static ApplicationContext applicationContext;
    private static Environment environment;


    public void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationContextUtil.applicationContext = applicationContext;
        ApplicationContextUtil.environment = applicationContext.getEnvironment();
    }

    /**
     *  获取applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        assertContextInjected();
        return applicationContext;
    }

    /**
     * 通过name获取 Bean
     */
    public static Object getBean(String name) {
        assertContextInjected();
        return getApplicationContext().getBean(name);
    }

    /**
     *  通过class获取Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        assertContextInjected();
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
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
     * 获取当前环境
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
        Assert.state(applicationContext != null,
                "applicationContext 未注入，请在 Spring 配置中注册");
    }

}
