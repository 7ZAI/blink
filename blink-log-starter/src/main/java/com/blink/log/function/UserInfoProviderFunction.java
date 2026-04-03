package com.blink.log.function;

/**
 * 用户信息提供函数式接口
 * <p>
 * 由业务模块实现，提供当前登录用户信息。
 * 从上下文、Session、JWT Token 等获取用户信息。
 * <p>
 * 使用示例：
 * <pre>
 * &#064;Bean
 * public UserInfoProviderFunction userInfoProviderFunction() {
 *     return () -&gt; {
 *         String userId = BlinkRequestContextHolder.getUserId();
 *         String loginName = BlinkRequestContextHolder.getLoginName();
 *         return new UserInfoProviderFunction.UserInfo(
 *             Integer.valueOf(userId), loginName);
 *     };
 * }
 * </pre>
 *
 * @author binblink
 */
@FunctionalInterface
public interface UserInfoProviderFunction {

    /**
     * 获取当前用户信息
     *
     * @return 用户信息，如果未登录可返回 null
     */
    UserInfo getCurrentUser();

    /**
     * 用户信息DTO
     */
    record UserInfo(Integer userId, String loginName) {}
}