package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.SyncChannelDataReq;
import com.blink.gateway.admin.service.DataSyncService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据同步控制器
 * 提供渠道数据、路由数据、配置数据的同步功能
 *
 * @author binblink
 */
@RestController
@RequestMapping("/dataSync")
public class DataSyncController {

    @Resource
    private DataSyncService dataSyncService;

    /**
     * 同步渠道数据到网关
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/syncChannelData")
    public ResponseDTO<EmptyBody> syncChannelData(@RequestBody @Validated RequestDTO<SyncChannelDataReq> reqDto) {
        dataSyncService.syncChannelData(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 同步路由数据到网关
     *
     * @return 操作结果
     */
    @PostMapping("/syncRouteData")
    public ResponseDTO<EmptyBody> syncRouteData() {
        dataSyncService.syncRouteData();
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 同步配置数据到网关
     *
     * @return 操作结果
     */
    @PostMapping("/syncConfigData")
    public ResponseDTO<EmptyBody> syncConfigData() {
        dataSyncService.syncConfigData();
        return ResponseDTO.newSuccessInstance();
    }
}
