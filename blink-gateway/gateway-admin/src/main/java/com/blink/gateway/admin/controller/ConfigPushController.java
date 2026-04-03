package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.GetConfigHistoryReq;
import com.blink.gateway.admin.dto.req.PushConfigReq;
import com.blink.gateway.admin.dto.req.RollbackConfigReq;
import com.blink.gateway.admin.dto.rsp.ConfigHistoryRsp;
import com.blink.gateway.admin.service.ConfigPushService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置推送控制器
 * 提供 Nacos 配置推送、历史查询、配置回滚等功能
 *
 * @author binblink
 */
@RestController
@RequestMapping("/configPush")
public class ConfigPushController {

    @Resource
    private ConfigPushService configPushService;

    /**
     * 推送配置到 Nacos
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/pushConfig")
    public ResponseDTO<EmptyBody> pushConfig(@RequestBody @Validated RequestDTO<PushConfigReq> reqDto) {
        configPushService.pushConfigToNacos(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 获取配置历史列表
     *
     * @param reqDto 请求参数
     * @return 配置历史列表
     */
    @PostMapping("/getConfigHistory")
    public ResponseDTO<ConfigHistoryRsp> getConfigHistory(@RequestBody @Validated RequestDTO<GetConfigHistoryReq> reqDto) {
        return configPushService.getConfigHistory(reqDto.getBody());
    }

    /**
     * 回滚配置到指定版本
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/rollbackConfig")
    public ResponseDTO<EmptyBody> rollbackConfig(@RequestBody @Validated RequestDTO<RollbackConfigReq> reqDto) {
        configPushService.rollbackConfig(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }
}