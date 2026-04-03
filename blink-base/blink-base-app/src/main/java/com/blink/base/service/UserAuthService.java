package com.blink.base.service;

import com.blink.base.dto.req.FirstTimeResetPasswordReq;
import com.blink.base.dto.req.SysLoginReq;
import com.blink.base.dto.req.SysLogoutReq;
import com.blink.base.dto.rsp.LoginConfigRsp;
import com.blink.base.dto.rsp.SysLoginRsp;
import com.blink.base.entity.SysUserDO;
import com.blink.framework.common.exception.BlinkException;


/**
 * 用户登入 登出模块
 */
public interface UserAuthService {

    /**
     * 登入
     *
     * @param loginParam 登入参数
     * @return
     * @throws BlinkException
     */
    SysLoginRsp login(SysLoginReq loginParam) throws BlinkException;

    /**
     * 登出
     *
     * @param logoutParam 登出参数
     * @return
     * @throws BlinkException
     */
    void logout(SysLogoutReq logoutParam) throws BlinkException;

    /**
     * 获取登入用户信息
     * @param loginUser 用户实体
     * @param token 认证token
     * @return 登入用户封装DTO
     * @throws BlinkException
     */
    SysLoginRsp getLoginUserInfo(SysUserDO loginUser, String token) throws BlinkException;

    /**
     * 根据token获取当前登录用户信息
     * @param token 认证token
     * @return 登入用户封装DTO
     * @throws BlinkException
     */
    SysLoginRsp getLoginUserInfo(String token) throws BlinkException;

    /**
     * 获取登录配置
     * @return 登录配置
     * @throws BlinkException
     */
    LoginConfigRsp getLoginConfig() throws BlinkException;

    /**
     * 首次登录重置密码
     *
     * @param token 用户认证token
     * @param resetParam 重置密码参数
     * @throws BlinkException
     */
    void firstTimeResetPassword(String token, FirstTimeResetPasswordReq resetParam) throws BlinkException;
}
