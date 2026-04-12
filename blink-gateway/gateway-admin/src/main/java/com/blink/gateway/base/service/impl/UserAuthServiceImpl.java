package com.blink.gateway.base.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.gateway.base.constants.BaseErrCodeConstant;
import com.blink.gateway.base.constants.CommonConstants;
import com.blink.gateway.base.constants.RedisKeyConstants;
import com.blink.gateway.base.dto.req.*;
import com.blink.gateway.base.dto.rsp.LoginConfigRsp;
import com.blink.gateway.base.dto.rsp.QueryShowMenuRsp;
import com.blink.gateway.base.dto.rsp.SysLoginRsp;
import com.blink.gateway.base.dto.vo.CaptchaVO;
import com.blink.gateway.base.dto.vo.SysConfigVO;
import com.blink.gateway.base.dto.vo.SysUserVO;
import com.blink.gateway.base.entity.SysRoleDO;
import com.blink.gateway.base.entity.SysUserDO;
import com.blink.gateway.base.mapper.SysRoleMapper;
import com.blink.gateway.base.mapper.SysUserMapper;
import com.blink.gateway.base.service.SysConfigService;
import com.blink.gateway.base.service.SysMenuService;
import com.blink.gateway.base.service.SysPermissionService;
import com.blink.gateway.base.service.UserAuthService;
import com.blink.gateway.base.service.UserDataScopeCacheService;
import com.blink.gateway.base.service.UserLoginHelperService;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户登入登出服务实现类
 * 使用 Sa-Token 进行认证管理
 *
 * @author binblink
 */
@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
public class UserAuthServiceImpl implements UserAuthService {

    @Resource
    private SysUserMapper userMapper;

    @Resource
    private SysMenuService menuService;

    @Resource
    private SysRoleMapper roleMapper;

    @Resource
    private SysPermissionService permissionService;

    @Resource
    private RedisClient redisClient;

    @Resource
    private UserLoginHelperService userLoginHelperService;

    @Resource
    private SysConfigService sysConfigService;


    @Resource
    private UserDataScopeCacheService userDataScopeCacheService;

    /**
     * 验证码类型配置
     */
    @Value("${blink.captcha.type:clickWord}")
    private String captchaType;

    /**
     * Session 中存储用户信息的 key
     */
    private static final String SESSION_USER_INFO_KEY = "userInfo";

    @Override
    public SysLoginRsp login(SysLoginReq loginParam) throws BlinkException {
        // 验证码校验
        validateCaptcha(loginParam);

        String loginName = loginParam.getLoginName();
        String password = loginParam.getPassword();

        SysUserDO loginUser = userMapper.selectOne(new QueryWrapper<SysUserDO>().lambda()
                .eq(SysUserDO::getLoginName, loginName));

        // 用户不存在
        if (ObjectUtil.isNull(loginUser)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 使用 BCrypt 验证密码
        String hashedPassword = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(password, loginUser.getSalt());
        Integer retry = loginUser.getPswRetry();

        // 用户状态判断
        if (!CommonConstants.USER_LOCKED_NOT.equals(loginUser.getLocked())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_LOCKED);
        }

        // 密码不正确
        if (!loginUser.getPassword().equals(hashedPassword)) {
            retry++;
            // 记录错误次数，超过3次锁定
            Integer locked = null;
            LocalDateTime lockTime = null;
            if (retry >= 3) {
                locked = CommonConstants.USER_LOCKED_ERR_PSW;
                lockTime = LocalDateTime.now();
            }
            // 使用独立事务更新错误次数，确保即使主事务回滚也能保存
            userLoginHelperService.updatePasswordRetry(loginUser.getUserId(), retry, locked, lockTime);
            BlinkException.throwBusinessException(BaseErrCodeConstant.INCORRECT_PASSWORD);
        }

        // 使用 Sa-Token 进行登录，以用户ID作为登录标识
        StpUtil.login(loginUser.getUserId());

        // 获取当前会话的 token
        String token = StpUtil.getTokenValue();

        // 查询用户相关权限、角色、菜单等信息
        SysLoginRsp result = getLoginUserInfo(loginUser, token);

        // 将用户信息存储到 Sa-Token Session 中
        UserInfoRedisDO userInfoRedis = buildUserInfoRedisDO(result);
        StpUtil.getSession().set(SESSION_USER_INFO_KEY, userInfoRedis);

        // 同时存储到 Redis（兼容现有服务，如 UserDataScopeCacheServiceImpl）
        long expireTime = 1800L; // 30分钟
        redisClient.setEx(RedisKeyConstants.USER_TOKEN + token, userInfoRedis, expireTime);

        // 生成并缓存用户数据权限信息
        userDataScopeCacheService.buildAndCache(loginUser.getUserId(), token);

        // 密码错误次数置零
        loginUser.setPswRetry(0);
        // 更新登录时间
        loginUser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(loginUser);

        // 检查是否需要重置密码（首次登录）
        if (Objects.nonNull(loginUser.getPasswordReset()) && CommonConstants.SUPER_ADMIN_YES.equals(loginUser.getPasswordReset())) {
            result.setNeedResetPassword(true);
        } else {
            result.setNeedResetPassword(false);
        }

        log.info("[Login] 用户登录成功 | loginName: {}, userId: {}", loginName, loginUser.getUserId());
        return result;
    }

    /**
     * 退出登录
     *
     * @param logoutParam 登出参数（可选，从 Sa-Token 上下文获取用户信息）
     * @throws BlinkException 业务异常
     */
    @Override
    public void logout(SysLogoutReq logoutParam) throws BlinkException {
        // 检查是否已登录
        if (!StpUtil.isLogin()) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 获取当前登录用户ID（从 Sa-Token 上下文获取）
        Integer userId = StpUtil.getLoginIdAsInt();

        // 清除用户数据权限缓存
        userDataScopeCacheService.clearCache(userId);

        // 清除 Redis 中的用户信息
        String token = StpUtil.getTokenValue();
        if (StrUtil.isNotBlank(token)) {
            redisClient.delete(RedisKeyConstants.USER_TOKEN + token);
        }

        // 使用 Sa-Token 登出
        StpUtil.logout();

        log.info("[Logout] 用户登出成功 | userId: {}", userId);
    }

    /**
     * 获取登录用户信息
     *
     * @param loginUser 登录用户实体
     * @param token     认证 token
     * @return 登录用户封装 DTO
     * @throws BlinkException 业务异常
     */
    @Override
    public SysLoginRsp getLoginUserInfo(SysUserDO loginUser, String token) throws BlinkException {
        SysLoginRsp sysLoginRsp = new SysLoginRsp();
        // 设置 token
        sysLoginRsp.setToken(token);

        // 用户信息
        SysUserVO sysUserVO = new SysUserVO();
        BeanUtil.copyProperties(loginUser, sysUserVO);


        sysLoginRsp.setUserInfo(sysUserVO);

        // 超级管理员标志 1-是超级管理员
        boolean isSuperAdmin = Objects.nonNull(loginUser.getSuperFlag())
                && CommonConstants.SUPER_ADMIN_ID.equals(loginUser.getSuperFlag());

        if (isSuperAdmin) {
            // 超级管理员返回所有菜单和超级管理员权限
            handleSuperAdminMenuAndPermissions(sysLoginRsp);
            // 超级管理员角色标识
            sysLoginRsp.setRoles(List.of(CommonConstants.SUPER_ADMIN_ROLE_CODE));
            // 超级管理员角色ID为空
            sysLoginRsp.setRoleIds(Collections.emptyList());
        } else {
            // 普通用户按角色查询菜单和权限
            handleNormalUserMenuAndPermissions(loginUser, sysLoginRsp);
        }

        return sysLoginRsp;
    }

    /**
     * 根据token获取当前登录用户信息
     *
     * @param token 认证 token
     * @return 登录用户封装 DTO
     * @throws BlinkException 当 token 无效或用户不存在时抛出异常
     */
    @Override
    public SysLoginRsp getLoginUserInfo(String token) throws BlinkException {
        if (StrUtil.isBlank(token)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 检查是否已登录
        if (!StpUtil.isLogin()) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 从 Sa-Token Session 获取用户信息
        UserInfoRedisDO userInfoRedis = (UserInfoRedisDO) StpUtil.getSession().get(SESSION_USER_INFO_KEY);
        if (Objects.isNull(userInfoRedis)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 获取用户实体
        SysUserDO loginUser = userMapper.selectById(userInfoRedis.getUserId());
        if (Objects.isNull(loginUser)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        return getLoginUserInfo(loginUser, token);
    }

    /**
     * 根据用户ID获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 登录用户封装 DTO
     * @throws BlinkException 当用户不存在时抛出异常
     */
    @Override
    public SysLoginRsp getLoginUserInfo(Integer userId) throws BlinkException {
        // 获取用户实体
        SysUserDO loginUser = userMapper.selectById(userId);
        if (Objects.isNull(loginUser)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 从 Sa-Token 获取当前 token
        String token = StpUtil.getTokenValue();

        return getLoginUserInfo(loginUser, token);
    }

    /**
     * 获取登录配置
     *
     * @return 登录配置
     * @throws BlinkException 业务异常
     */
    @Override
    public LoginConfigRsp getLoginConfig() throws BlinkException {
        // 尝试从缓存获取
        Object cached = redisClient.get(RedisKeyConstants.SYSTEM_CONFIG);
        if (cached != null) {
            return BeanUtil.copyProperties(cached, LoginConfigRsp.class);
        }

        // 从数据库获取
        LoginConfigRsp config = new LoginConfigRsp();

        // 登录验证码开关 - 直接传入完整 key
        Boolean captchaEnabled = sysConfigService.getBooleanConfig(
                CommonConstants.SysConfigKeys.LOGIN_CAPTCHA_ENABLED, true);
        config.setCaptchaEnabled(captchaEnabled);

        // 验证码类型
        config.setCaptchaType(captchaType);

        var queryParam = new QueryOneSysConfigReq();
        // 系统标题 - 直接传入完整 key（buildCacheKey 会统一处理）
        queryParam.setConfigKey(CommonConstants.SysConfigKeys.SYSTEM_TITLE);
        SysConfigVO titleConfig = sysConfigService.getOneConfigFromCacheOrDataBase(queryParam);

        if (titleConfig != null && titleConfig.getConfigValue() != null) {
            config.setSystemTitle(titleConfig.getConfigValue());
        } else {
            config.setSystemTitle(CommonConstants.DEFAULT_SYSTEM_TITLE);
        }

        // 系统Logo
        queryParam.setConfigKey(CommonConstants.SysConfigKeys.SYSTEM_LOGO);
        SysConfigVO logoConfig = sysConfigService.getOneConfigFromCacheOrDataBase(queryParam);

        if (logoConfig != null && logoConfig.getConfigValue() != null) {
            config.setSystemLogo(logoConfig.getConfigValue());
        } else {
            config.setSystemLogo(CommonConstants.DEFAULT_SYSTEM_LOGO);
        }

        // 页脚信息
        queryParam.setConfigKey(CommonConstants.SysConfigKeys.SYSTEM_FOOTER);
        SysConfigVO footerConfig = sysConfigService.getOneConfigFromCacheOrDataBase(queryParam);
        if (footerConfig != null && footerConfig.getConfigValue() != null) {
            config.setSystemFooter(footerConfig.getConfigValue());
        } else {
            config.setSystemFooter(CommonConstants.DEFAULT_SYSTEM_FOOTER);
        }

        // 用户默认头像
        queryParam.setConfigKey(CommonConstants.SysConfigKeys.USER_DEFAULT_AVATAR);
        SysConfigVO avatarConfig = sysConfigService.getOneConfigFromCacheOrDataBase(queryParam);
        if (avatarConfig != null && avatarConfig.getConfigValue() != null) {
            config.setDefaultAvatar(avatarConfig.getConfigValue());
        } else {
            config.setDefaultAvatar(CommonConstants.DEFAULT_USER_AVATAR);
        }

        // 缓存5分钟
        redisClient.setEx(RedisKeyConstants.SYSTEM_CONFIG, config, 300L);

        return config;
    }

    /**
     * 首次登录重置密码
     *
     * @param token       用户认证 token
     * @param resetParam  重置密码参数
     * @throws BlinkException 业务异常
     */
    @Override
    public void firstTimeResetPassword(String token, FirstTimeResetPasswordReq resetParam) throws BlinkException {
        // 验证新密码和确认密码是否一致
        if (!resetParam.getNewPassword().equals(resetParam.getConfirmPassword())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PASSWORD_CONFIRM_ERR);
        }

        // 检查是否已登录
        if (!StpUtil.isLogin()) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 获取当前登录用户ID
        Integer userId = StpUtil.getLoginIdAsInt();

        // 获取当前登录用户
        SysUserDO currentUser = userMapper.selectById(userId);

        if (currentUser == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 验证用户是否需要重置密码
        if (!CommonConstants.SUPER_ADMIN_YES.equals(currentUser.getPasswordReset())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PASSWORD_RESET_NOT_REQUIRED);
        }

        // 生成新盐值并加密新密码
        String newSalt = org.springframework.security.crypto.bcrypt.BCrypt.gensalt();
        String newPasswordHash = org.springframework.security.crypto.bcrypt.BCrypt.hashpw(resetParam.getNewPassword(), newSalt);

        // 更新用户密码和重置标识
        currentUser.setPassword(newPasswordHash);
        currentUser.setSalt(newSalt);
        currentUser.setPasswordReset(CommonConstants.SUPER_ADMIN_NO);
        currentUser.setUpdateBy(currentUser.getLoginName());
        userMapper.updateById(currentUser);

        log.info("[UserAuth] 首次登录重置密码成功 | userId: {}", userId);
    }

    /**
     * 处理超级管理员的菜单和权限
     * 超级管理员返回所有菜单和超级管理员权限标识
     *
     * @param sysLoginRsp 登录响应对象
     */
    private void handleSuperAdminMenuAndPermissions(SysLoginRsp sysLoginRsp) {
        // 查询所有菜单（不限制角色）
        QueryShowMenuRsp menus = menuService.getAllMenus();

        // 菜单信息
        if (Objects.nonNull(menus)) {
            sysLoginRsp.setMenus(menus.getMenus());
            sysLoginRsp.setFunctionMenu(menus.getFunctionMenu());
        }

        // 超级管理员权限标识
        sysLoginRsp.setPermissions(Set.of(CommonConstants.SUPER_ADMIN_PERMISSION));
    }

    /**
     * 处理普通用户的菜单和权限
     *
     * @param loginUser   登录用户
     * @param sysLoginRsp 登录响应对象
     */
    private void handleNormalUserMenuAndPermissions(SysUserDO loginUser, SysLoginRsp sysLoginRsp) {
        var queryUserRolesReqDTO = new QueryUserRolesReq();
        queryUserRolesReqDTO.setUserId(loginUser.getUserId());

        List<SysRoleDO> roles = roleMapper.findSysRolesByUser(queryUserRolesReqDTO);
        List<Integer> roleIds = roles.stream().map(SysRoleDO::getRoleId).collect(Collectors.toList());

        // 没有任何角色授权的用户无法登录
        if (CollUtil.isEmpty(roles)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DONT_HAVE_ANY_ROLE);
        }

        QueryShowMenuReq reqBody = new QueryShowMenuReq();
        reqBody.setRoleIds(roleIds);

        QueryShowMenuRsp menus = menuService.getSysMenusByRoles(reqBody);

        Set<String> permissions = permissionService.getPermissionsByRoles(roleIds);

        // 菜单信息
        if (Objects.nonNull(menus)) {
            sysLoginRsp.setMenus(menus.getMenus());
            sysLoginRsp.setFunctionMenu(menus.getFunctionMenu());
        }

        // 角色信息
        List<String> rolesVO = roles.stream().map(SysRoleDO::getRoleCode).collect(Collectors.toList());
        sysLoginRsp.setRoles(rolesVO);
        sysLoginRsp.setRoleIds(roleIds);

        // 权限标识
        sysLoginRsp.setPermissions(permissions);
    }


    /**
     * 校验验证码
     * <p>
     * 根据系统配置决定是否需要验证码校验
     * </p>
     *
     * @param loginParam 登录请求参数
     * @throws BlinkException 验证码校验失败时抛出异常
     */
    private void validateCaptcha(SysLoginReq loginParam) throws BlinkException {
        // 检查是否开启了登录验证码
        Boolean captchaEnabled = sysConfigService.getBooleanConfig(
                CommonConstants.SysConfigKeys.LOGIN_CAPTCHA_ENABLED, true);

        // 如果未开启验证码，直接返回
        if (!captchaEnabled) {
            log.debug("登录验证码未开启，跳过验证码校验");
            return;
        }

        // 获取验证码信息
        CaptchaVO captchaReqVO = loginParam.getCaptchaVO();

        // 如果未传入验证码信息，抛出异常
        if (ObjectUtil.isNull(captchaReqVO)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.CAPTCHA_NOT_VALID);
        }

        // 如果验证码校验参数为空，抛出异常
        if (ObjectUtil.isEmpty(captchaReqVO.getCaptchaVerification())) {
            log.warn("登录请求中验证码校验参数为空");
            BlinkException.throwBusinessException(BaseErrCodeConstant.CAPTCHA_NOT_VALID);
        }

        // 通过 Redis 验证 token 是否已验证
        String captchaVerification = captchaReqVO.getCaptchaVerification();
        String verifiedKey = "captcha:verified:" + captchaVerification;
        Object verified = redisClient.get(verifiedKey);

        if (verified == null) {
            log.warn("验证码校验失败，token 未找到或已过期: {}", captchaVerification);
            BlinkException.throwBusinessException(BaseErrCodeConstant.CAPTCHA_EXPIRED);
        }

        // 验证成功后删除 token，防止重复使用
        redisClient.delete(verifiedKey);
        log.debug("登录验证码校验成功");
    }

    /**
     * 构建用户 Redis 存储对象
     *
     * @param rspInfo 登录响应信息
     * @return UserInfoRedisDO 用户 Redis 存储对象
     */
    private UserInfoRedisDO buildUserInfoRedisDO(SysLoginRsp rspInfo) {
        SysUserVO userInfo = rspInfo.getUserInfo();
        UserInfoRedisDO userInfoRedis = new UserInfoRedisDO();
        BeanUtil.copyProperties(userInfo, userInfoRedis);
        userInfoRedis.setLoginDateTime(userInfo.getLastLoginTime());
        userInfoRedis.setPermissions(rspInfo.getPermissions());
        userInfoRedis.setToken(rspInfo.getToken());
        userInfoRedis.setRoleIds(rspInfo.getRoleIds());
        return userInfoRedis;
    }
}