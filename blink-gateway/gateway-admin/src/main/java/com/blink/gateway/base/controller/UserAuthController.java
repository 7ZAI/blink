package com.blink.gateway.base.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.blink.gateway.base.dto.req.FirstTimeResetPasswordReq;
import com.blink.gateway.base.dto.req.SysLoginReq;
import com.blink.gateway.base.dto.req.SysLogoutReq;
import com.blink.gateway.base.dto.rsp.LoginConfigRsp;
import com.blink.gateway.base.dto.rsp.SysLoginRsp;
import com.blink.gateway.base.service.UserAuthService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.core.annotation.IpRateLimit;
import com.blink.log.annotation.RecordLog;
import com.blink.log.constant.LogType;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登入 API
 *
 * @author binblink
 * @module blink
 * @since 2024-01-04
 */
@RestController
@RequestMapping("/auth")
public class UserAuthController {

    @Resource
    private UserAuthService userAuthService;

    /**
     * 登入
     *
     * @param requestDTO 请求参数
     * @return 登录响应
     * @throws BlinkException 业务异常
     */
    @RecordLog(type = LogType.LOGIN, description = "用户登录")
    @PostMapping("/login")
    public ResponseDTO<SysLoginRsp> login(@Validated @RequestBody RequestDTO<SysLoginReq> requestDTO) throws BlinkException {
        return ResponseDTO.newSuccessInstance(userAuthService.login(requestDTO.getBody()));
    }

    /**
     * 登出
     *
     * @param requestDTO 请求参数
     * @return 空响应
     * @throws BlinkException 业务异常
     */
    @RecordLog(type = LogType.LOGIN, description = "用户登出")
    @PostMapping("/logout")
    public ResponseDTO<EmptyBody> logout(@Validated @RequestBody RequestDTO<SysLogoutReq> requestDTO) throws BlinkException {
        userAuthService.logout(requestDTO.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getUserInfo")
    public ResponseDTO<SysLoginRsp> getUserInfo() throws BlinkException {
        // 从 Sa-Token 上下文获取当前登录用户ID
        Integer userId = StpUtil.getLoginIdAsInt();
        return ResponseDTO.newSuccessInstance(userAuthService.getLoginUserInfo(userId));
    }

    /**
     * 获取登录配置
     * <p>
     * 该接口对外公开访问，使用IP级别限流保护
     * 每个IP每秒最多3次请求
     * </p>
     *
     * @param requestDTO 请求参数
     * @return 登录配置
     * @throws BlinkException 业务异常
     */
    @IpRateLimit(name = "getLoginConfig", limitForPeriod = 3, limitRefreshPeriod = 1)
    @PostMapping("/getLoginConfig")
    public ResponseDTO<LoginConfigRsp> getLoginConfig(@RequestBody RequestDTO<EmptyBody> requestDTO) throws BlinkException {
        return ResponseDTO.newSuccessInstance(userAuthService.getLoginConfig());
    }

    /**
     * 首次登录重置密码
     * <p>
     * 用于用户首次登录时强制重置初始密码
     * </p>
     *
     * @param requestDTO 重置密码请求参数
     * @return 空响应
     * @throws BlinkException 业务异常
     */
    @PostMapping("/firstTimeResetPassword")
    public ResponseDTO<EmptyBody> firstTimeResetPassword(@Validated @RequestBody RequestDTO<FirstTimeResetPasswordReq> requestDTO) throws BlinkException {
        userAuthService.firstTimeResetPassword(requestDTO.getToken(), requestDTO.getBody());
        return ResponseDTO.newSuccessInstance();
    }
}