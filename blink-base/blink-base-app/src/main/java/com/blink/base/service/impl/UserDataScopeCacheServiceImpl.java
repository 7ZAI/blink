package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.base.constants.CommonConstants;
import com.blink.base.constants.RedisKeyConstants;
import com.blink.base.entity.SysUserGroupRelaDO;
import com.blink.base.mapper.SysGroupMapper;
import com.blink.base.mapper.SysUserGroupRelaMapper;
import com.blink.base.service.SysDataFilterService;
import com.blink.base.service.UserDataScopeCacheService;
import com.blink.datasource.data.RuleConfig;
import com.blink.datasource.data.UserDataScopeInfo;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据范围权限缓存服务实现类
 *
 * @author binblink
 */
@Service
@Slf4j
public class UserDataScopeCacheServiceImpl implements UserDataScopeCacheService {

    @Resource
    private RedisClient redisClient;

    @Resource
    private SysUserGroupRelaMapper sysUserGroupRelaMapper;

    @Resource
    private SysGroupMapper sysGroupMapper;

    @Resource
    private SysDataFilterService sysDataFilterService;

    /**
     * 缓存过期时间（秒），与用户登录token过期时间一致
     */
    private static final Long CACHE_EXPIRE_SECONDS = 60 * 30L;

    @Override
    public UserDataScopeInfo buildAndCache(Integer userId, String token) {
        if (userId == null || userId <= 0) {
            return new UserDataScopeInfo();
        }

        UserDataScopeInfo dataScopeInfo = new UserDataScopeInfo();
        dataScopeInfo.setUserId(userId);

        UserInfoRedisDO userInfo = null;

        // 优先通过 token 获取用户信息
        if (StrUtil.isNotBlank(token)) {
            userInfo = JacksonUtil.convert(
                    redisClient.get(RedisKeyConstants.USER_TOKEN + token),
                    UserInfoRedisDO.class
            );
        }

        // 如果 token 方式获取失败，尝试扫描 USER_TOKEN:* 查找
        if (ObjectUtil.isNull(userInfo)) {
            userInfo = findUserInfoByUserId(userId);
        }

        if (ObjectUtil.isNull(userInfo)) {
            log.warn("[UserDataScopeCache] 用户信息不存在 | userId: {}", userId);
            return dataScopeInfo;
        }

        // 复制基本信息
        BeanUtil.copyProperties(userInfo, dataScopeInfo);

        // 超级管理员不需要查询规则
        if (CommonConstants.SUPER_ADMIN_YES.equals(userInfo.getSuperFlag())) {
            log.debug("[UserDataScopeCache] 超级管理员跳过规则查询 | userId: {}", userId);
            cacheDataScopeInfo(userId, dataScopeInfo);
            return dataScopeInfo;
        }

        // 查询组织信息
        fillGroupInfo(dataScopeInfo);

        // 查询数据权限规则
        fillRuleConfigs(dataScopeInfo);

        // 缓存到 Redis
        cacheDataScopeInfo(userId, dataScopeInfo);

        log.info("[UserDataScopeCache] 生成用户数据权限缓存 | userId: {}, ruleCount: {}",
                userId, CollUtil.isEmpty(dataScopeInfo.getRuleConfigs()) ? 0 : dataScopeInfo.getRuleConfigs().size());

        return dataScopeInfo;
    }

    @Override
    public UserDataScopeInfo getFromCache(Integer userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        UserDataScopeInfo cached = JacksonUtil.convert(
                redisClient.get(RedisKeyConstants.DATA_SCOPE_USER + userId),
                UserDataScopeInfo.class
        );

        if (cached != null) {
            log.debug("[UserDataScopeCache] 命中缓存 | userId: {}", userId);
        }

        return cached;
    }

    @Override
    public void clearCache(Integer userId) {
        if (userId != null) {
            redisClient.delete(RedisKeyConstants.DATA_SCOPE_USER + userId);
            log.info("[UserDataScopeCache] 清除缓存 | userId: {}", userId);
        }
    }

    @Override
    public UserDataScopeInfo refreshCache(Integer userId) {
        clearCache(userId);
        return buildAndCache(userId);
    }

    /**
     * 缓存 UserDataScopeInfo 到 Redis
     */
    private void cacheDataScopeInfo(Integer userId, UserDataScopeInfo dataScopeInfo) {
        redisClient.setEx(
                RedisKeyConstants.DATA_SCOPE_USER + userId,
                dataScopeInfo,
                CACHE_EXPIRE_SECONDS
        );
    }

    /**
     * 填充用户组织信息
     */
    private void fillGroupInfo(UserDataScopeInfo dataScopeInfo) {
        SysUserGroupRelaDO userGroupRela = sysUserGroupRelaMapper.selectOne(
                new QueryWrapper<SysUserGroupRelaDO>().lambda()
                        .eq(SysUserGroupRelaDO::getUserId, dataScopeInfo.getUserId())
        );

        if (userGroupRela != null) {
            dataScopeInfo.setDeptId(userGroupRela.getGroupId());

            // 查询组织及其子部门
            List<Integer> deptIds = sysGroupMapper.selectDeptAndChildrenById(userGroupRela.getGroupId());
            dataScopeInfo.setDeptIds(deptIds);
        }
    }

    /**
     * 填充数据权限规则配置
     */
    private void fillRuleConfigs(UserDataScopeInfo dataScopeInfo) {
        List<RuleConfig> ruleConfigs = sysDataFilterService.getRuleConfigsByUserId(dataScopeInfo.getUserId());
        dataScopeInfo.setRuleConfigs(CollUtil.isEmpty(ruleConfigs) ? new ArrayList<>() : ruleConfigs);
    }

    /**
     * 通过扫描 USER_TOKEN:* 查找用户信息（降级方案）
     * 当无法通过 token 直接获取用户信息时，扫描所有 token 键查找匹配的用户
     *
     * @param userId 用户ID
     * @return 用户信息，未找到返回 null
     */
    private UserInfoRedisDO findUserInfoByUserId(Integer userId) {
        String pattern = RedisKeyConstants.USER_TOKEN + "*";
        String oldTokenPrefix = RedisKeyConstants.USER_TOKEN_OLD;

        try (Cursor<String> cursor = redisClient.scan(pattern, 100)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // 跳过被顶替的旧 token
                if (key.startsWith(oldTokenPrefix)) {
                    continue;
                }
                Object value = redisClient.get(key);
                if (value != null) {
                    UserInfoRedisDO userInfo = JacksonUtil.convert(value, UserInfoRedisDO.class);
                    if (userInfo != null && userId.equals(userInfo.getUserId())) {
                        return userInfo;
                    }
                }
            }
        } catch (Exception e) {
            log.error("[UserDataScopeCache] 扫描用户信息失败 | userId: {}", userId, e);
        }
        return null;
    }
}