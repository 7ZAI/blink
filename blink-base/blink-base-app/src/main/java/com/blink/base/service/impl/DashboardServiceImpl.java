package com.blink.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.base.constants.RedisKeyConstans;
import com.blink.base.dto.rsp.DashboardRsp;
import com.blink.base.entity.SysMenuDO;
import com.blink.base.entity.SysRoleDO;
import com.blink.base.entity.SysUserDO;
import com.blink.base.mapper.SysMenuMapper;
import com.blink.base.mapper.SysRoleMapper;
import com.blink.base.mapper.SysUserMapper;
import com.blink.base.service.DashboardService;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Dashboard 服务实现
 *
 * @author binblink
 */
@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private RedisClient redisClient;

    @Override
    public DashboardRsp getDashboardData() throws BlinkException {
        DashboardRsp rsp = new DashboardRsp();

        // 统计总用户数
        Long totalUsers = sysUserMapper.selectCount(
                new QueryWrapper<SysUserDO>().lambda()
                        .eq(SysUserDO::getDelFlag, false)
        );
        rsp.setTotalUsers(totalUsers.intValue());

        // 统计在线用户数（Redis token 数量）
        String tokenPattern = RedisKeyConstans.USER_TOKEN + "*";
        Long onlineUsers = redisClient.countByPrefix(tokenPattern);
        rsp.setOnlineUsers(onlineUsers.intValue());

        // 统计总角色数
        Long totalRoles = sysRoleMapper.selectCount(
                new QueryWrapper<SysRoleDO>().lambda()
                        .eq(SysRoleDO::getDelFlag, false)
        );
        rsp.setTotalRoles(totalRoles.intValue());

        // 统计总菜单数
        Long totalMenus = sysMenuMapper.selectCount(
                new QueryWrapper<SysMenuDO>().lambda()
                        .eq(SysMenuDO::getDelFlag, false)
        );
        rsp.setTotalMenus(totalMenus.intValue());

        return rsp;
    }
}