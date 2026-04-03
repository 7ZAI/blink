package com.blink.log.annotation;


import com.blink.log.sensitive.SensitiveType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段脱敏注解
 * 
 * 标注在实体类字段上，日志输出时自动脱敏
 * 
 * 使用示例：
 * <pre>
 * public class UserDTO {
 *     @SensitiveField(type = SensitiveType.PHONE)
 *     private String phone;
 *     
 *     @SensitiveField(type = SensitiveType.ID_CARD)
 *     private String idCard;
 *     
 *     @SensitiveField(type = SensitiveType.CUSTOM, prefixKeep = 2, suffixKeep = 2)
 *     private String customField;
 * }
 * </pre>
 * 
 * @author binblink
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveField {

    /**
     * 脱敏类型
     */
    SensitiveType type() default SensitiveType.CUSTOM;

    /**
     * 保留前缀字符数（仅 type=CUSTOM 时生效）
     */
    int prefixKeep() default 3;

    /**
     * 保留后缀字符数（仅 type=CUSTOM 时生效）
     */
    int suffixKeep() default 4;

    /**
     * 脱敏字符（默认 *）
     */
    char maskChar() default '*';
}
