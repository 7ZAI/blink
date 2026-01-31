package com.blink.base.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blink.base.constans.BaseErrCodeConstant;
import com.blink.base.constans.CommonConstans;
import com.blink.base.constans.RedisKeyConstans;
import com.blink.base.dto.req.QueryShowMenuReqDTO;
import com.blink.base.dto.req.QueryUserRolesReqDTO;
import com.blink.base.dto.req.SysLoginReqDTO;
import com.blink.base.dto.req.SysLogoutReqDTO;
import com.blink.base.dto.rsp.QueryShowMenuRspDTO;
import com.blink.base.dto.rsp.SysLoginRspDTO;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.entity.SysRoleDO;
import com.blink.base.entity.SysUserDO;
import com.blink.base.mapper.SysRoleMapper;
import com.blink.base.mapper.SysUserMapper;
import com.blink.base.service.SysMenuService;
import com.blink.base.service.SysPermissionService;
import com.blink.base.service.UserAuthService;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.UserInfoRedisDO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.utils.JacksonUtil;
import com.blink.framework.redis.component.RedisClient;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
public class SysUserAuthServiceImpl implements UserAuthService {

    private final Logger logger = LoggerFactory.getLogger(SysUserAuthServiceImpl.class);
    @Resource
    private CaptchaService captchaService;

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


    @Override
    public SysLoginRspDTO login(SysLoginReqDTO loginParam) throws BlinkException {

        //TODO 是否开启验证码校验 后期设置成系统配置项
        if (false) {
            CaptchaVO captchaVO = new CaptchaVO();
            BeanUtil.copyProperties(loginParam.getCaptchaVO(), captchaVO);
            //验证码验证错误
            if (!captchaService.verification(captchaVO).isSuccess()) {
                //验证码校验失败，返回信息告诉前端
                //repCode  0000  无异常，代表成功
                //repCode  9999  服务器内部异常
                //repCode  0011  参数不能为空
                //repCode  6110  验证码已失效，请重新获取
                //repCode  6111  验证失败
                //repCode  6112  获取验证码失败,请联系管理员
                logger.info("登入失败");
                BlinkException.throwBusinessException(BaseErrCodeConstant.INCORRECT_CAPTCHA);
            }
        }

        String username = loginParam.getUsername();
        String password = loginParam.getPassword();

        SysUserDO loginUser = userMapper.selectOne(new QueryWrapper<SysUserDO>().lambda()
                .eq(SysUserDO::getLoginName, username));
        //用户不存在
        if (ObjectUtil.isNull(loginUser)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }
        password = BCrypt.hashpw(password, loginUser.getSalt());
        Integer retry = loginUser.getPswRetry();

        //用户状态判断
        if (!CommonConstans.USER_LOCKED_NOT.equals(loginUser.getLocked())) {

            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_LOCKED);
        }

        //密码不正确
        if (!loginUser.getPassword().equals(password)) {
            retry++;
            loginUser.setPswRetry(retry);
            //TODO 记录错误次数 超过3次锁 后期设置成系统配置项
            if (retry >= 3) {
                loginUser.setLocked(CommonConstans.USER_LOCKED_ERR_PSW);
                loginUser.setLockTime(LocalDateTime.now());
            }
            userMapper.updateById(loginUser);
            BlinkException.throwBusinessException(BaseErrCodeConstant.INCORRECT_PASSWORD);

        }


        //生成token 选择方案1
        //方案1 uuid 存 redis 更安全 适用短期登入 企业应用
        //方案2 jwt 加密存在前端 后端校验  适用长期登入 面对公众的网站
        //方案3 jwt token 取代方案1 多了一道加签 验签 防止修改的作用

        String token = IdUtil.simpleUUID();
//        String token = BlinkJwtUtil.generateToken(loginUser.getUsername(),String.valueOf(loginUser.getUserId()));
        //查询用户相关权限 角色 菜单等信息
        SysLoginRspDTO result = getLoginUserInfo(loginUser, token);

        //踢出久登入
        UserInfoRedisDO older = JacksonUtil.convert(redisClient.get(RedisKeyConstans.USER_INFO + loginUser.getUserId()), UserInfoRedisDO.class);

        if (Objects.nonNull(older)) {
            redisClient.delete(RedisKeyConstans.USER_TOKEN + older.getToken());
            //保存被顶替登入的用户的旧token 用来提示用户在别处登入了
            redisClient.setEx(RedisKeyConstans.USER_TOKEN_OLD + older.getToken(), older.getUserId(), Long.valueOf(60 * 5));
        }

        //存入redis
        storeUserInfoToRedis(result);
        //密码错误次数置零
        loginUser.setPswRetry(0);
        //更新登入时间
        loginUser.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(loginUser);
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
    public void logout(SysLogoutReqDTO logoutParam) throws BlinkException {

        UserInfoRedisDO userInfo = JacksonUtil.convert(redisClient.get(RedisKeyConstans.USER_TOKEN + logoutParam.getToken()), UserInfoRedisDO.class);

        if (Objects.isNull(userInfo)) {
            // 用户已登出
            BlinkException.throwBusinessException(BaseErrCodeConstant.USER_NOT_EXIT);
        }

        if (logoutParam.getToken().equals(userInfo.getToken()) && logoutParam.getUserId().equals(String.valueOf(userInfo.getUserId()))) {
            redisClient.delete(RedisKeyConstans.USER_TOKEN + logoutParam.getToken());
            redisClient.delete(RedisKeyConstans.USER_INFO + logoutParam.getUserId());
            return;
        }

        logger.error("非法登出请求 requestID {}", BlinkRequestContextHolder.getRequestId());
        // userId 和 token不匹配
        BlinkException.throwBusinessException();
    }

    /**
     * 保存token和用户信息到redis
     *
     * @param rspInfo
     */
    private void storeUserInfoToRedis(SysLoginRspDTO rspInfo) {

        SysUserVO userInfo = rspInfo.getUserInfo();

        var userInfoRedis = new UserInfoRedisDO();

        BeanUtil.copyProperties(userInfo, userInfoRedis);

        userInfoRedis.setLoginDateTime(userInfo.getLastLoginTime());
        userInfoRedis.setPermissions(rspInfo.getPermissions());
        userInfoRedis.setToken(rspInfo.getToken());

        //TODO 过期时间 30分钟 后期设置成系统配置项
        Long expireTime = Long.valueOf(60 * 30);
        redisClient.delete(RedisKeyConstans.USER_TOKEN + rspInfo.getToken());
//        redisClient.remove(RedisKeyConstans.USER_INFO + userInfo.getUserId());
        //存用户登入凭证和存用户信息
        redisClient.setEx(RedisKeyConstans.USER_TOKEN + rspInfo.getToken(), userInfoRedis, expireTime);
        //存用户信息
        redisClient.setEx(RedisKeyConstans.USER_INFO + userInfo.getUserId(), userInfoRedis, expireTime);
    }

    /**
     * 获取登入用户信息
     *
     * @param loginUser 用户DTO
     * @param token     登入凭证
     * @return 返回用户信息
     */
    private SysLoginRspDTO getLoginUserInfo(SysUserDO loginUser, String token) {

        var queryUserRolesReqDTO = new QueryUserRolesReqDTO();
        queryUserRolesReqDTO.setUserId(loginUser.getUserId());

        List<SysRoleDO> roles = roleMapper.findSysRolesByUser(queryUserRolesReqDTO);
        List<Integer> roleIds = roles.stream().map(SysRoleDO::getRoleId).collect(Collectors.toList());

        //没有任何角色授权的用户 无法登入
        if (CollUtil.isEmpty(roles)) {
            BlinkException.throwBusinessException(BaseErrCodeConstant.DONT_HAVE_ANY_ROLE);
        }

        QueryShowMenuReqDTO reqBody = new QueryShowMenuReqDTO();
        reqBody.setRoleIds(roleIds);

        QueryShowMenuRspDTO menus = menuService.getSysMenusByRoles(reqBody);

        Set<String> permissions = permissionService.getPermissionsByRoles(roleIds);

        SysLoginRspDTO sysLoginRspDTO = new SysLoginRspDTO();
        //装配前端所需信息
        sysLoginRspDTO.setToken(token);

        //菜单信息
        if (Objects.nonNull(menus)) {
            sysLoginRspDTO.setMenus(menus.getMenus());
            sysLoginRspDTO.setFunctionMenu(menus.getFunctionMenu());
        }

        //用户信息
        var sysUserVO = new SysUserVO();
        BeanUtil.copyProperties(loginUser, sysUserVO);
        sysLoginRspDTO.setUserInfo(sysUserVO);
        //角色信息
        List<String> rolesVO = roles.stream().map(SysRoleDO::getRoleCode).collect(Collectors.toList());

        sysLoginRspDTO.setRoles(rolesVO);

        //权限标识
        sysLoginRspDTO.setPermissions(permissions);

        return sysLoginRspDTO;
    }
}
