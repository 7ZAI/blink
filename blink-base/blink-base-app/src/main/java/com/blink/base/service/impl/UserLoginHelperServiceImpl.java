package com.blink.base.service.impl;

import com.blink.base.entity.SysUserDO;
import com.blink.base.mapper.SysUserMapper;
import com.blink.base.service.UserLoginHelperService;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户登录辅助服务实现类
 * 用于处理需要独立事务的登录相关操作
 *
 * @author binblink
 */
@Service
@Slf4j
public class UserLoginHelperServiceImpl implements UserLoginHelperService {

    @Resource
    private SysUserMapper userMapper;

    /**
     * 更新密码错误次数
     * 使用 REQUIRES_NEW 事务传播，确保即使主事务回滚也能保存错误次数
     *
     * @param userId   用户ID
     * @param retry    当前错误次数
     * @param locked   是否锁定（null表示不更新锁定状态）
     * @param lockTime 锁定时间（null表示不更新锁定时间）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updatePasswordRetry(Integer userId, Integer retry, Integer locked, java.time.LocalDateTime lockTime) {
        SysUserDO user = new SysUserDO();
        user.setUserId(userId);
        user.setPswRetry(retry);
        if (locked != null) {
            user.setLocked(locked);
        }
        if (lockTime != null) {
            user.setLockTime(lockTime);
        }
        userMapper.updateById(user);
    }
}
