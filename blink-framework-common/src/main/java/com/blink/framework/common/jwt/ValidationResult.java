package com.blink.framework.common.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token验证结果
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    /** 是否有效 */
    private boolean valid;
    
    /** 验证消息 */
    private String message;
    
    /** 失败原因 */
    private String reason;
    
    /**
     * 成功的验证结果
     */
    public static ValidationResult success(String message) {
        return ValidationResult.builder()
            .valid(true)
            .message(message)
            .build();
    }
    
    /**
     * 过期的Token结果
     */
    public static ValidationResult expired(String message) {
        return ValidationResult.builder()
            .valid(false)
            .message(message)
            .reason("EXPIRED")
            .build();
    }
    
    /**
     * 无效的Token结果
     */
    public static ValidationResult invalid(String message) {
        return ValidationResult.builder()
            .valid(false)
            .message(message)
            .reason("INVALID")
            .build();
    }
}