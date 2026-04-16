package com.blink.framework.test.helper;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security 测试辅助工具
 * 用于测试中快速配置安全上下文
 *
 * 使用方式：
 * <pre>
 * // 在测试中设置安全上下文
 * @BeforeEach
 * void setup() {
 *     SecurityMockHelper.setAuthentication("admin", "ADMIN", "USER");
 * }
 *
 * // 或使用 @WithMockUser 注解（推荐）
 * @Test
 * @WithMockUser(username = "admin", roles = {"ADMIN"})
 * void testWithAdmin() { }
 * </pre>
 *
 * @author binblink
 * @since 2026-04-16
 */
public class SecurityMockHelper {

    /**
     * 设置安全上下文（指定用户名和角色）
     *
     * @param username 用户名
     * @param roles    角色列表
     */
    public static void setAuthentication(String username, String... roles) {
        Authentication auth = createAuthentication(username, roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * 设置安全上下文（指定用户名、密码和角色）
     *
     * @param username 用户名
     * @param password 密码
     * @param roles    角色列表
     */
    public static void setAuthentication(String username, String password, String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                username, password, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * 设置安全上下文（自定义 Authentication）
     *
     * @param authentication Authentication 对象
     */
    public static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 清除安全上下文
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 获取当前认证用户名
     *
     * @return 用户名，未认证返回 null
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    /**
     * 检查是否已认证
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    /**
     * 检查是否拥有指定角色
     *
     * @param role 角色（无需 ROLE_ 前缀）
     * @return 是否拥有角色
     */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /**
     * 创建 Authentication 对象
     *
     * @param username 用户名
     * @param roles    角色列表
     * @return Authentication 对象
     */
    private static Authentication createAuthentication(String username, String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(
                username, "N/A", authorities);
    }

    /**
     * 创建超级管理员上下文
     */
    public static void setSuperAdmin() {
        setAuthentication("admin", new String[]{"SUPER_ADMIN", "ADMIN", "USER"});
    }

    /**
     * 创建普通用户上下文
     */
    public static void setNormalUser(String username) {
        setAuthentication(username, new String[]{"USER"});
    }

    /**
     * 创建匿名用户上下文（未认证）
     */
    public static void setAnonymous() {
        clearAuthentication();
    }
}