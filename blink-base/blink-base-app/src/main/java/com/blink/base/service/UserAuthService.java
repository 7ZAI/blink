package com.blink.base.service;

import com.blink.base.dto.req.SysLoginReqDTO;
import com.blink.base.dto.req.SysLogoutReqDTO;
import com.blink.base.dto.rsp.SysLoginRspDTO;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
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
    SysLoginRspDTO login(SysLoginReqDTO loginParam) throws BlinkException;

    /**
     * 登出
     *
     * @param logoutParam 登出参数
     * @return
     * @throws BlinkException
     */
    void logout(SysLogoutReqDTO logoutParam) throws BlinkException;
}
