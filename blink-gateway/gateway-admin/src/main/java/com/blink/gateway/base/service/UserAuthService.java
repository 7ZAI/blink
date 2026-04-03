package com.blink.gateway.base.service;

import com.blink.gateway.base.dto.req.FirstTimeResetPasswordReq;
import com.blink.gateway.base.dto.req.SysLoginReq;
import com.blink.gateway.base.dto.req.SysLogoutReq;
import com.blink.gateway.base.dto.rsp.LoginConfigRsp;
import com.blink.gateway.base.dto.rsp.SysLoginRsp;
import com.blink.gateway.base.entity.SysUserDO;
import com.blink.framework.common.exception.BlinkException;

/**
 * 用户登入 登出模块
 * 使用 Sa-Token 进行认证管理
 *
 * @author binblink
 */
public interface UserAuthService {

    /**
     * 登入
     *
     * @param loginParam 登入参数
     * @return 登录响应信息
     * @throws BlinkException 业务异常
     */
    SysLoginRsp login(SysLoginReq loginParam) throws BlinkException;

    /**
     * 登出
     *
     * @param logoutParam 登出参数
     * @throws BlinkException 业务异常
     */
    void logout(SysLogoutReq logoutParam) throws BlinkException;

    /**
     * 获取登入用户信息
     *
     * @param loginUser 用户实体
     * @param token 认证token
     * @return 登入用户封装DTO
     * @throws BlinkException 业务异常
     */
    SysLoginRsp getLoginUserInfo(SysUserDO loginUser, String token) throws BlinkException;

    /**
     * 根据token获取当前登录用户信息
     *
     * @param token 认证token
     * @return 登入用户封装DTO
     * @throws BlinkException 当token无效或用户不存在时抛出异常
     */
    SysLoginRsp getLoginUserInfo(String token) throws BlinkException;

    /**
     * 根据用户ID获取当前登录用户信息
     *
     * @param userId 用户ID
     * @return 登入用户封装DTO
     * @throws BlinkException 当用户不存在时抛出异常
     */
    SysLoginRsp getLoginUserInfo(Integer userId) throws BlinkException;

    /**
     * 获取登录配置
     *
     * @return 登录配置
     * @throws BlinkException 业务异常
     */
    LoginConfigRsp getLoginConfig() throws BlinkException;

    /**
     * 首次登录重置密码
     *
     * @param token 用户认证token
     * @param resetParam 重置密码参数
     * @throws BlinkException 业务异常
     */
    void firstTimeResetPassword(String token, FirstTimeResetPasswordReq resetParam) throws BlinkException;
}