package com.blink.framework.common.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Token对：Access Token + Refresh Token
 *
 * @author binblink
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair {
    
    /** Access Token用于访问资源 */
    private String accessToken;
    
    /** Refresh Token用于刷新Access Token */
    private String refreshToken;
    
    /** Token类型 access refresh */
    private String tokenType;
    
    /** Access Token过期时间（秒） */
    private long expiresIn;
    
    /** 用户名 */
    private String username;
    
    /** 用户角色列表 */
    private List<String> roles;
}