package com.blink.gateway.admin.util;

/**
 * Gateway Admin 工具类
 *
 * @author binblink
 * @since 2026-04-14
 */
public class GatewayAdminUtil {

    /**
     * 将 Object 转换为 Long 值（支持 String 和 Number 类型）
     *
     * @param value 原始值
     * @return Long 值，转换失败返回 0L
     */
    public static Long toLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 将 Object 转换为 Double 值（支持 String 和 Number 类型）
     *
     * @param value 原始值
     * @return Double 值，转换失败返回 0.0
     */
    public static Double toDoubleValue(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * 将 Object 转换为 Integer 值（支持 String 和 Number 类型）
     *
     * @param value 原始值
     * @return Integer 值，转换失败返回 0
     */
    public static Integer toIntValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}