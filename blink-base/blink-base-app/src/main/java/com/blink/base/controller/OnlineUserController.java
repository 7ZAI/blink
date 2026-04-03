package com.blink.base.controller;

import com.blink.base.dto.req.KickoutUserReq;
import com.blink.base.dto.req.QueryOnlineUserReq;
import com.blink.base.dto.rsp.OnlineUserRsp;
import com.blink.base.service.OnlineUserService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线用户管理控制器
 * 提供在线用户查询、强制下线等功能
 *
 * @author binblink
 * @since 2026-03-21
 */
@RestController
@RequestMapping("/onlineUser")
public class OnlineUserController {

    @Resource
    private OnlineUserService onlineUserService;

    /**
     * 查询在线用户列表
     *
     * @param requestDTO 请求参数
     * @return {@link ResponseDTO<OnlineUserRsp>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/list")
    public ResponseDTO<OnlineUserRsp> getOnlineUserList(@RequestBody RequestDTO<QueryOnlineUserReq> requestDTO) throws BlinkException {
        return ResponseDTO.newSuccessInstance(onlineUserService.getOnlineUserList(requestDTO.getBody()));
    }

    /**
     * 强制用户下线
     *
     * @param requestDTO 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws BlinkException 业务异常
     */
    @PostMapping("/kickout")
    public ResponseDTO<EmptyBody> kickoutUser(@Validated @RequestBody RequestDTO<KickoutUserReq> requestDTO) throws BlinkException {
        onlineUserService.kickoutUser(requestDTO.getBody());
        return ResponseDTO.newSuccessInstance();
    }
}
