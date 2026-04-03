package com.blink.gateway.admin.controller;

import com.blink.base.dto.req.QuerySimpleUserReq;
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.rsp.QuerySimpleUserRsp;
import com.blink.base.dto.rsp.UserPermissionDetailRsp;
import com.blink.base.dubbo.service.BaseDubboService;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 渠道用户控制器
 * 提供用户选择和权限查询接口
 *
 * @author binblink
 */
@Slf4j
@RestController
@RequestMapping("/channelUser")
public class ChannelUserController {

    @DubboReference(timeout = 10000, check = false)
    private BaseDubboService baseDubboService;

    /**
     * 查询简化用户列表（用于弹窗选择）
     *
     * @param reqDto 请求参数
     * @return 用户列表
     */
    @PostMapping("/getSimpleUserList")
    public ResponseDTO<QuerySimpleUserRsp> getSimpleUserList(
            @RequestBody @Validated RequestDTO<QuerySimpleUserReq> reqDto) {
        log.info("[ChannelUser] 查询简化用户列表 | keyword: {}",
                reqDto.getBody() != null ? reqDto.getBody().getKeyword() : null);
        return baseDubboService.getSimpleUserList(reqDto);
    }

    /**
     * 查询用户权限详情
     *
     * @param reqDto 请求参数
     * @return 权限详情
     */
    @PostMapping("/getUserPermissionDetail")
    public ResponseDTO<UserPermissionDetailRsp> getUserPermissionDetail(
            @RequestBody @Validated RequestDTO<UserIdReq> reqDto) {
        log.info("[ChannelUser] 查询用户权限详情 | userId: {}",
                reqDto.getBody() != null ? reqDto.getBody().getUserId() : null);
        return baseDubboService.getUserPermissionDetail(reqDto);
    }
}