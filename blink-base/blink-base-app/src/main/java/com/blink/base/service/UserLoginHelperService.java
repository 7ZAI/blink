package com.blink.base.service;

/**
 * 用户登录辅助服务
 * 用于处理需要独立事务的登录相关操作
 */
public interface UserLoginHelperService {

    /**
     * 更新密码错误次数
     * 使用独立事务，确保即使主事务回滚也能保存错误次数
     *
     * @param userId   用户ID
     * @param retry    当前错误次数
     * @param locked   是否锁定（null表示不更新锁定状态）
     * @param lockTime 锁定时间（null表示不更新锁定时间）
     */
    void updatePasswordRetry(Integer userId, Integer retry, Integer locked, java.time.LocalDateTime lockTime);
}
