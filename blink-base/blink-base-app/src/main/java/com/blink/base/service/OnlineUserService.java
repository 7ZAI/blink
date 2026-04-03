package com.blink.base.service;

import com.blink.base.dto.req.KickoutUserReq;
import com.blink.base.dto.req.QueryOnlineUserReq;
import com.blink.base.dto.rsp.OnlineUserRsp;
import com.blink.framework.common.exception.BlinkException;

import java.util.List;

/**
 * 在线用户服务接口
 * 提供在线用户查询、强制下线等功能
 *
 * @author binblink
 * @since 2026-03-21
 */
public interface OnlineUserService {

    /**
     * 查询在线用户列表
     *
     * @param queryParam 查询参数
     * @return 在线用户列表响应
     * @throws BlinkException 业务异常
     */
    OnlineUserRsp getOnlineUserList(QueryOnlineUserReq queryParam) throws BlinkException;

    /**
     * 强制用户下线
     *
     * @param kickoutUserReq 强制下线请求参数
     * @throws BlinkException 业务异常
     */
    void kickoutUser(KickoutUserReq kickoutUserReq) throws BlinkException;

    /**
     * 根据用户ID列表查询在线用户的token
     *
     * @param userIdList 用户ID列表
     * @return 在线用户的token列表
     */
    List<String> getOnlineUserTokensByUserIds(List<Integer> userIdList);

    /**
     * 根据用户ID列表强制下线
     *
     * @param userIdList 用户ID列表
     */
    void kickoutUsersByUserIds(List<Integer> userIdList);
}
