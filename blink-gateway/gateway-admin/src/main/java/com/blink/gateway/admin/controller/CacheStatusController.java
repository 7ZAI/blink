package com.blink.gateway.admin.controller;

import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.gateway.admin.dto.req.CacheCheckReq;
import com.blink.gateway.admin.dto.req.CacheSyncReq;
import com.blink.gateway.admin.dto.rsp.CacheCheckRsp;
import com.blink.gateway.admin.dto.rsp.SyncLogRsp;
import com.blink.gateway.admin.service.CacheStatusService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 缓存状态控制器
 * 提供一致性检查和数据同步功能
 *
 * @author binblink
 */
@RestController
@RequestMapping("/cacheStatus")
public class CacheStatusController {

    @Resource
    private CacheStatusService cacheStatusService;

    /**
     * 获取网关实例列表
     *
     * @return 实例列表
     */
    @GetMapping("/instances")
    public ResponseDTO<?> getInstances() {
        return cacheStatusService.getGatewayInstances();
    }

    /**
     * 执行一致性检查
     *
     * @param reqDto 请求参数
     * @return 检查结果
     */
    @PostMapping("/check")
    public ResponseDTO<CacheCheckRsp> check(@RequestBody RequestDTO<CacheCheckReq> reqDto) {
        return cacheStatusService.checkConsistency(reqDto.getBody());
    }

    /**
     * 同步数据到网关
     *
     * @param reqDto 请求参数
     * @return 操作结果
     */
    @PostMapping("/sync")
    public ResponseDTO<EmptyBody> sync(@RequestBody RequestDTO<CacheSyncReq> reqDto) {
        return cacheStatusService.syncData(reqDto.getBody());
    }

    /**
     * 获取同步日志列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 日志列表
     */
    @GetMapping("/logs")
    public ResponseDTO<SyncLogRsp> getLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return cacheStatusService.getSyncLogs(pageNum, pageSize);
    }
}