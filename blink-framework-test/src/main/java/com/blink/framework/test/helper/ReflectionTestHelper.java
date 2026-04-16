package com.blink.framework.test.helper;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * 反射测试辅助工具
 * 用于测试私有方法或设置私有字段
 *
 * @author binblink
 * @since 2026-04-16
 */
public class ReflectionTestHelper {

    /**
     * 设置对象的私有字段值
     *
     * @param target    目标对象
     * @param fieldName 字段名
     * @param value     要设置的值
     */
    public static void setField(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    /**
     * 设置类的静态私有字段值
     *
     * @param targetClass 目标类
     * @param fieldName   字段名
     * @param value       要设置的值
     */
    public static void setStaticField(Class<?> targetClass, String fieldName, Object value) {
        ReflectionTestUtils.setField(targetClass, fieldName, value);
    }

    /**
     * 获取对象的私有字段值
     *
     * @param target    目标对象
     * @param fieldName 字段名
     * @return 字段值
     */
    public static Object getField(Object target, String fieldName) {
        return ReflectionTestUtils.getField(target, fieldName);
    }

    /**
     * 获取对象的私有字段值（带类型转换）
     *
     * @param target    目标对象
     * @param fieldName 字段名
     * @param clazz     返回类型
     * @return 字段值
     */
    public static <T> T getField(Object target, String fieldName, Class<T> clazz) {
        Object value = ReflectionTestUtils.getField(target, fieldName);
        return clazz.cast(value);
    }

    /**
     * 获取类的静态私有字段值
     *
     * @param targetClass 目标类
     * @param fieldName   字段名
     * @return 字段值
     */
    public static Object getStaticField(Class<?> targetClass, String fieldName) {
        return ReflectionTestUtils.getField(targetClass, fieldName);
    }

    /**
     * 调用对象的私有方法
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @param args       方法参数
     * @return 方法返回值
     */
    public static Object invokeMethod(Object target, String methodName, Object... args) {
        return ReflectionTestUtils.invokeMethod(target, methodName, args);
    }

    /**
     * 调用对象的私有方法（带类型转换）
     *
     * @param target     目标对象
     * @param methodName 方法名
     * @param clazz      返回类型
     * @param args       方法参数
     * @return 方法返回值
     */
    public static <T> T invokeMethod(Object target, String methodName, Class<T> clazz, Object... args) {
        Object result = ReflectionTestUtils.invokeMethod(target, methodName, args);
        return clazz.cast(result);
    }

    /**
     * 调用类的静态私有方法
     *
     * @param targetClass 目标类
     * @param methodName  方法名
     * @param args        方法参数
     * @return 方法返回值
     */
    public static Object invokeStaticMethod(Class<?> targetClass, String methodName, Object... args) {
        return ReflectionTestUtils.invokeMethod(targetClass, methodName, args);
    }

    /**
     * 调用类的静态私有方法（带类型转换）
     *
     * @param targetClass 目标类
     * @param methodName  方法名
     * @param clazz       返回类型
     * @param args        方法参数
     * @return 方法返回值
     */
    public static <T> T invokeStaticMethod(Class<?> targetClass, String methodName, Class<T> clazz, Object... args) {
        Object result = ReflectionTestUtils.invokeMethod(targetClass, methodName, args);
        return clazz.cast(result);
    }
}