package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.base.constants.BaseErrCodeConstant;
import com.blink.base.constants.CommonConstans;
import com.blink.base.constants.RedisKeyConstans;
import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.LoginConfigRsp;
import com.blink.base.dto.vo.CaptchaVO;
import com.blink.base.dto.vo.SysConfigVO;
import com.blink.base.service.SysConfigService;
import com.blink.base.service.UserDataScopeCacheService;
import com.blink.base.dto.rsp.QueryShowMenuRsp;
import com.blink.base.dto.rsp.SysLoginRsp;
import com.blink.base.dto.vo.SysGroupVO;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.SysGroupDO;
import com.blink.base.entity.SysRoleDO;
import com.blink.base.entity.SysUserDO;
import com.blink.base.entity.SysUserGroupRelaDO;
import com.blink.base.mapper.SysGroupMapper;
import com.blink.base.mapper.SysRoleMapper;
import com.blink.base.mapper.SysUserGroupRelaMapper;
import com.blink.base.mapper.SysUserMapper;
import com.blink.base.service.SysMenuService;
import com.blink.base.service.SysPermissionService;
import com.blink.base.service.UserAuthService;
import com.blink.base.service.UserLoginHelperService;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统登入 登出
 *
 * @author binblink
 */
@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
public class SysUserAuthServiceImpl implements UserAuthService {


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
    private SysUserGroupRelaMapper sysUserGroupRelaMapper;

    @Resource
    private SysGroupMapper sysGroupMapper;

    @Resource
    private UserDataScopeCacheService userDataScopeCacheService;


    @Override
    public SysLoginRsp login(SysLoginReq loginParam) throws BlinkException {

        // 验证码校验
        validateCaptcha(loginParam);

        String loginName = loginParam.getLoginName();
        String password = loginParam.getPassword();

        SysUserDO loginUser = userMapper.selectOne(new QueryWrapper<SysUserDO>().lambda()
                .eq(SysUserDO::getLoginName, loginName));
        //用户不存在
        if (ObjectUtil.isNull(loginUser)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 用户锁定状态判断
        if (!CommonConstans.USER_LOCKED_NOT.equals(loginUser.getLocked())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_LOCKED);
        }

        Integer retry = loginUser.getPswRetry();
        // 使用标准 BCrypt 验证（salt 已包含在存储的 hash 中）
        if (!BCrypt.checkpw(password, loginUser.getPassword())) {
            retry++;
            // 记录错误次数 超过3次锁定
            Integer locked = null;
            LocalDateTime lockTime = null;
            if (retry >= 3) {
                locked = CommonConstans.USER_LOCKED_ERR_PSW;
                lockTime = LocalDateTime.now();
            }
            // 使用独立事务更新错误次数，确保即使主事务回滚也能保存
            userLoginHelperService.updatePasswordRetry(loginUser.getUserId(), retry, locked, lockTime);
            BlinkException.throwBusinessException(BaseErrCodeConstant.INCORRECT_PASSWORD);
        }


        //生成token 选择方案1
        //方案1 uuid 存 redis 更安全 适用短期登入 企业应用
        //方案2 jwt 加密存在前端 后端校验  适用长期登入 面对公众的网站
        //方案3 jwt token 取代方案1 多了一道加签 验签 防止修改的作用

        String token = IdUtil.simpleUUID();
//        String token = BlinkJwtUtil.generateToken(loginUser.getUsername(),String.valueOf(loginUser.getUserId()));
        //查询用户相关权限 角色 菜单等信息
        SysLoginRsp result = getLoginUserInfo(loginUser, token);

        // 使用 Lua 脚本存储会话（支持多设备登录）
        UserInfoRedisDO userInfoRedis = buildUserInfoRedisDO(result);
        int maxDevices = getMaxDevices();
        String kickedToken = storeSessionWithLua(loginUser.getUserId(), token, userInfoRedis, maxDevices);

        if (StrUtil.isNotBlank(kickedToken)) {
            log.info("[Login] 踢出较早登录的设备 | userId: {}, kickedToken: {}", loginUser.getUserId(), kickedToken);
        }

        // 生成并缓存用户数据权限信息
        userDataScopeCacheService.buildAndCache(loginUser.getUserId(), token);

        //密码错误次数置零
        loginUser.setPswRetry(0);
        //更新登入时间
        loginUser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(loginUser);

        //检查是否需要重置密码(首次登录)
        if (Objects.nonNull(loginUser.getPasswordReset()) && loginUser.getPasswordReset().equals(CommonConstans.SUPER_ADMIN_YES)) {
            result.setNeedResetPassword(true);
        } else {
            result.setNeedResetPassword(false);
        }

        //登入成功
        return result;
    }


    /**
     * 退出登入
     *
     * @param logoutParam
     * @return EmptyBody
     * @throws BlinkException
     */
    @Override
    public void logout(SysLogoutReq logoutParam) throws BlinkException {

        UserInfoRedisDO userInfo = JacksonUtil.convert(redisClient.get(RedisKeyConstans.USER_TOKEN + logoutParam.getToken()), UserInfoRedisDO.class);

        if (Objects.isNull(userInfo)) {
            // 用户已登出
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        if (logoutParam.getToken().equals(userInfo.getToken()) && logoutParam.getUserId().equals(String.valueOf(userInfo.getUserId()))) {
            // 删除 token
            redisClient.delete(RedisKeyConstans.USER_TOKEN + logoutParam.getToken());
            // 从 ZSet 中移除
            redisClient.zRemove(RedisKeyConstans.USER_TOKENS + logoutParam.getUserId(), logoutParam.getToken());
            // 清除用户数据权限缓存
            userDataScopeCacheService.clearCache(Integer.valueOf(logoutParam.getUserId()));
            log.info("[Logout] 用户登出成功 | userId: {}, token: {}", logoutParam.getUserId(), logoutParam.getToken());
            return;
        }

        log.error("[Logout] 非法登出请求 | requestId: {}", BlinkRequestContextHolder.getRequestId());
        // userId 和 token不匹配
        BlinkException.throwBusinessException();
    }

    /**
     * 使用 Lua 脚本存储会话信息
     * 支持多设备登录管理
     *
     * @param userId 用户ID
     * @param newToken 新token
     * @param userInfo 用户信息
     * @param maxDevices 最大设备数
     * @return 被踢出的token，如果没有则返回null
     */
    private String storeSessionWithLua(Integer userId, String newToken, UserInfoRedisDO userInfo, int maxDevices) {
        try {
            String luaScript = loadLuaScript("lua/login_session.lua");
            List<String> keys = Arrays.asList(
                RedisKeyConstans.USER_TOKENS + userId,
                RedisKeyConstans.USER_TOKEN + newToken,
                newToken
            );

            // 使用 Redis 序列化器生成带类型信息的 JSON，确保与读取时的反序列化器兼容
            // GenericJackson2JsonRedisSerializer 会在 JSON 中添加 @class 类型信息
            String userInfoJson;
            try {
                byte[] serialized = redisClient.getValueSerializer().serialize(userInfo);
                userInfoJson = serialized != null ? new String(serialized, java.nio.charset.StandardCharsets.UTF_8) : JacksonUtil.toJson(userInfo);
            } catch (Exception e) {
                log.warn("[Login] 使用 Redis 序列化器失败，降级使用普通 JSON: {}", e.getMessage());
                userInfoJson = JacksonUtil.toJson(userInfo);
            }

            long loginTime = System.currentTimeMillis();
            // 30分钟
            long ttl = 1800L;

            List<Object> args = Arrays.asList(
                String.valueOf(maxDevices),
                String.valueOf(loginTime),
                userInfoJson,
                String.valueOf(ttl),
                String.valueOf(userId)
            );

            // 使用 Object.class 作为返回类型，Lua 脚本返回 {1, kickedToken}
            // 使用 StringRedisSerializer 执行脚本，避免 GenericJackson2JsonRedisSerializer 尝试解析非 JSON 数据
            org.springframework.data.redis.core.script.DefaultRedisScript<Object> script =
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Object.class);

            Object result = redisClient.executeWithStringSerializer(script, keys, args.toArray());

            if (result instanceof List<?> resultList) {
                if (resultList.size() >= 2 && "1".equals(String.valueOf(resultList.get(0)))) {
                    Object kickedTokenObj = resultList.get(1);
                    String kickedToken = kickedTokenObj != null ? String.valueOf(kickedTokenObj) : null;
                    return StrUtil.isNotBlank(kickedToken) ? kickedToken : null;
                }
            }
            log.warn("[Login] Lua脚本执行返回异常结果: {}", result);
            return null;
        } catch (Exception e) {
            log.error("[Login] Lua脚本执行失败，降级为普通存储: {}", e.getMessage(), e);
            // 降级处理：直接存储
            fallbackStoreSession(newToken, userInfo);
            return null;
        }
    }

    /**
     * 降级存储会话（不使用Lua脚本）
     * 使用 Redis 序列化器确保与读取时的反序列化器兼容
     *
     * @param token 用户token
     * @param userInfo 用户信息
     */
    private void
    fallbackStoreSession(String token, UserInfoRedisDO userInfo) {
        long expireTime = 1800L;
        // 直接使用 redisClient.setEx，它会使用 GenericJackson2JsonRedisSerializer 序列化
        // 这样存储的数据包含 @class 类型信息，读取时能正确反序列化
        redisClient.setEx(RedisKeyConstans.USER_TOKEN + token, userInfo, expireTime);
        log.info("[Login] 降级存储会话成功 | token: {}", token);
    }

    /**
     * 加载 Lua 脚本
     *
     * @param path Lua脚本路径
     * @return Lua脚本内容
     */
    private String loadLuaScript(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[Login] 加载Lua脚本失败: {}", path, e);
            throw new BlinkException("加载Lua脚本失败: " + path, e, "BUSS0002");
        }
    }

    /**
     * 获取最大设备登录数
     * 优先从数据库配置读取，失败则使用默认值
     *
     * @return 最大设备登录数
     */
    private int getMaxDevices() {
        try {
            Integer maxDevices = sysConfigService.getIntegerConfig(
                CommonConstans.SysConfigKeys.SESSION_MAX_CONCURRENT,
                CommonConstans.DEFAULT_MAX_DEVICES
            );
            return maxDevices != null && maxDevices > 0 ? maxDevices : CommonConstans.DEFAULT_MAX_DEVICES;
        } catch (Exception e) {
            log.warn("[Login] 获取最大设备数配置失败，使用默认值: {}", CommonConstans.DEFAULT_MAX_DEVICES);
            return CommonConstans.DEFAULT_MAX_DEVICES;
        }
    }

    /**
     * 构建用户 Redis 存储对象
     *
     * @param rspInfo 登录响应信息
     * @return UserInfoRedisDO 用户Redis存储对象
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

    /**
     * 获取登入用户信息
     *
     * @param loginUser 用户DTO
     * @param token     登入凭证
     * @return 返回用户信息
     */
    @Override
    public SysLoginRsp getLoginUserInfo(SysUserDO loginUser, String token) {

        SysLoginRsp sysLoginRsp = new SysLoginRsp();
        //装配前端所需信息
        sysLoginRsp.setToken(token);

        //用户信息
        var sysUserVO = new SysUserVO();
        BeanUtil.copyProperties(loginUser, sysUserVO);

        // 查询用户所属组织信息
        fillUserGroupInfo(loginUser.getUserId(), sysUserVO);

        sysLoginRsp.setUserInfo(sysUserVO);

        //超级管理员标志 1-是超级管理员
        boolean isSuperAdmin = Objects.nonNull(loginUser.getSuperFlag()) 
                && CommonConstans.SUPER_ADMIN_ID.equals(loginUser.getSuperFlag());

        if (isSuperAdmin) {
            //超级管理员返回所有菜单和超级管理员权限
            handleSuperAdminMenuAndPermissions(sysLoginRsp);
            //超级管理员角色标识
            sysLoginRsp.setRoles(List.of(CommonConstans.SUPER_ADMIN_ROLE_CODE));
            //超级管理员角色ID为空
            sysLoginRsp.setRoleIds(Collections.emptyList());
        } else {
            //普通用户按角色查询菜单和权限
            handleNormalUserMenuAndPermissions(loginUser, sysLoginRsp);
        }

        return sysLoginRsp;
    }

    /**
     * 处理超级管理员的菜单和权限
     * 超级管理员返回所有菜单和超级管理员权限标识
     *
     * @param sysLoginRsp 登录响应对象
     */
    private void handleSuperAdminMenuAndPermissions(SysLoginRsp sysLoginRsp) {
        //查询所有菜单（不限制角色）
        QueryShowMenuRsp menus = menuService.getAllMenus();

        //菜单信息
        if (Objects.nonNull(menus)) {
            sysLoginRsp.setMenus(menus.getMenus());
            sysLoginRsp.setFunctionMenu(menus.getFunctionMenu());
        }

        //超级管理员权限标识
        sysLoginRsp.setPermissions(Set.of(CommonConstans.SUPER_ADMIN_PERMISSION));
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

        //没有任何角色授权的用户 无法登入
        if (CollUtil.isEmpty(roles)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DONT_HAVE_ANY_ROLE);
        }

        QueryShowMenuReq reqBody = new QueryShowMenuReq();
        reqBody.setRoleIds(roleIds);

        QueryShowMenuRsp menus = menuService.getSysMenusByRoles(reqBody);

        Set<String> permissions = permissionService.getPermissionsByRoles(roleIds);

        //菜单信息
        if (Objects.nonNull(menus)) {
            sysLoginRsp.setMenus(menus.getMenus());
            sysLoginRsp.setFunctionMenu(menus.getFunctionMenu());
        }

        //角色信息
        List<String> rolesVO = roles.stream().map(SysRoleDO::getRoleCode).collect(Collectors.toList());
        sysLoginRsp.setRoles(rolesVO);
        sysLoginRsp.setRoleIds(roleIds);

        //权限标识
        sysLoginRsp.setPermissions(permissions);
    }

    /**
     * 填充用户组织信息
     *
     * @param userId    用户ID
     * @param sysUserVO 用户VO
     */
    private void fillUserGroupInfo(Integer userId, SysUserVO sysUserVO) {
        // 查询用户组织关联
        SysUserGroupRelaDO userGroupRela = sysUserGroupRelaMapper.selectOne(
                new QueryWrapper<SysUserGroupRelaDO>().lambda()
                        .eq(SysUserGroupRelaDO::getUserId, userId)
        );

        if (Objects.nonNull(userGroupRela)) {
            // 查询组织详情
            SysGroupDO group = sysGroupMapper.selectById(userGroupRela.getGroupId());
            if (Objects.nonNull(group)) {
                sysUserVO.setGroupName(group.getGroupName());

                // 填充组织详情
                SysGroupVO groupVO = new SysGroupVO();
                BeanUtil.copyProperties(group, groupVO);
                sysUserVO.setGroup(groupVO);
            }
        }
    }

    /**
     * 根据token获取当前登录用户信息
     * @param token 认证token
     * @return 登入用户封装DTO
     * @throws BlinkException 当token无效或用户不存在时抛出异常
     */
    @Override
    public SysLoginRsp getLoginUserInfo(String token) throws BlinkException {
        if (ObjectUtil.isEmpty(token)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }
        
        UserInfoRedisDO userInfoRedis = JacksonUtil.convert(
            redisClient.get(RedisKeyConstans.USER_TOKEN + token), 
            UserInfoRedisDO.class
        );
        
        if (Objects.isNull(userInfoRedis)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }
        
        SysUserDO loginUser = userMapper.selectById(userInfoRedis.getUserId());
        if (Objects.isNull(loginUser)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }
        
        return getLoginUserInfo(loginUser, token);
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
        Boolean captchaEnabled = sysConfigService.getBooleanConfig( CommonConstans.SysConfigKeys.LOGIN_CAPTCHA_ENABLED, true);

        
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
     * 获取基础系统配置 前端认证前获取接口
     * 包含登录配置、系统标题、Logo、页脚等
     * <p>
     * 使用Redis缓存，缓存时间5分钟
     * </p>
     * @return 基础系统配置
     * @throws BlinkException 当配置获取失败时抛出异常
     */
    @Override
    public LoginConfigRsp getLoginConfig() throws BlinkException {
        // 尝试从缓存获取
        Object cached = redisClient.get(RedisKeyConstans.SYSTEM_CONFIG);
        if (cached != null) {
            return JacksonUtil.convert(cached, LoginConfigRsp.class);
        }

        // 从数据库获取
        LoginConfigRsp config = new LoginConfigRsp();

        // 登录验证码开关
        Boolean captchaEnabled = sysConfigService.getBooleanConfig(
                CommonConstans.SysConfigKeys.LOGIN_CAPTCHA_ENABLED, true);
        config.setCaptchaEnabled(captchaEnabled);

        var queryParam = new QueryOneSysConfigReq();
        queryParam.setConfigKey(CommonConstans.SysConfigKeys.SYSTEM_TITLE.replaceAll(RedisKeyConstans.BLINK_PREFIX, ""));
        // 系统标题
        SysConfigVO titleConfig = sysConfigService.getOneConfigFromCacheOrDataBase(queryParam);

        if (titleConfig != null && titleConfig.getConfigValue() != null) {
            config.setSystemTitle(titleConfig.getConfigValue());
        } else {
            config.setSystemTitle(CommonConstans.DEFAULT_SYSTEM_TITLE);
        }


        queryParam.setConfigKey(CommonConstans.SysConfigKeys.SYSTEM_LOGO.replaceAll(RedisKeyConstans.BLINK_PREFIX, ""));

        // 系统Logo
        SysConfigVO logoConfig = sysConfigService.getOneConfigFromCacheOrDataBase(queryParam);

        if (logoConfig != null && logoConfig.getConfigValue() != null) {
            config.setSystemLogo(logoConfig.getConfigValue());
        } else {
            config.setSystemLogo(CommonConstans.DEFAULT_SYSTEM_LOGO);
        }

        queryParam.setConfigKey(CommonConstans.SysConfigKeys.SYSTEM_FOOTER.replaceAll(RedisKeyConstans.BLINK_PREFIX, ""));

        // 页脚信息
        SysConfigVO footerConfig = sysConfigService.getOneConfigFromCacheOrDataBase(queryParam );
        if (footerConfig != null && footerConfig.getConfigValue() != null) {
            config.setSystemFooter(footerConfig.getConfigValue());
        } else {
            config.setSystemFooter(CommonConstans.DEFAULT_SYSTEM_FOOTER);
        }

        queryParam.setConfigKey(CommonConstans.SysConfigKeys.USER_DEFAULT_AVATAR.replaceAll(RedisKeyConstans.BLINK_PREFIX, ""));

        // 用户默认头像
        SysConfigVO avatarConfig = sysConfigService.getOneConfigFromCacheOrDataBase(queryParam);
        if (avatarConfig != null && avatarConfig.getConfigValue() != null) {
            config.setDefaultAvatar(avatarConfig.getConfigValue());
        } else {
            config.setDefaultAvatar(CommonConstans.DEFAULT_USER_AVATAR);
        }

        // 缓存5分钟
        redisClient.setEx(RedisKeyConstans.SYSTEM_CONFIG, config, 300L);

        return config;
    }

    /**
     * 首次登录重置密码
     *
     * @param token 用户认证token
     * @param resetParam 重置密码参数
     * @throws BlinkException 当token无效、用户不存在或密码验证失败时抛出异常
     */
    @Override
    public void firstTimeResetPassword(String token, FirstTimeResetPasswordReq resetParam) throws BlinkException {
        // 验证新密码和确认密码是否一致
        if (!resetParam.getNewPassword().equals(resetParam.getConfirmPassword())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PASSWORD_CONFIRM_ERR);
        }

        // 通过token获取当前登录用户信息
        if (ObjectUtil.isEmpty(token)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        UserInfoRedisDO userInfoRedis = JacksonUtil.convert(
                redisClient.get(RedisKeyConstans.USER_TOKEN + token),
                UserInfoRedisDO.class
        );

        if (Objects.isNull(userInfoRedis)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 获取当前登录用户
        SysUserDO currentUser = userMapper.selectById(userInfoRedis.getUserId());

        if (currentUser == null) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        // 验证用户是否需要重置密码
        if (!CommonConstans.SUPER_ADMIN_YES.equals(currentUser.getPasswordReset())) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.PASSWORD_RESET_NOT_REQUIRED);
        }

        // 使用标准 BCrypt 加密新密码
        String newPasswordHash = BCrypt.hashpw(resetParam.getNewPassword(), BCrypt.gensalt());

        // 更新用户密码和重置标识
        currentUser.setPassword(newPasswordHash);
        currentUser.setPasswordReset(CommonConstans.SUPER_ADMIN_NO);
        currentUser.setUpdateBy(userInfoRedis.getLoginName());
        userMapper.updateById(currentUser);
    }
}
