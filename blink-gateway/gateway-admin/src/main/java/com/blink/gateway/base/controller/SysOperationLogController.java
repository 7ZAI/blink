package com.blink.gateway.base.controller;

import com.blink.gateway.base.dto.req.DeleteSysOperationLogReq;
import com.blink.gateway.base.dto.req.QueryOperationLogReq;
import com.blink.gateway.base.dto.rsp.OperationLogDetailRsp;
import com.blink.gateway.base.dto.rsp.OperationLogRsp;
import com.blink.gateway.base.service.SysOperationLogService;
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
 * 操作日志前端控制器
 *
 * @author binblink
 */
@RestController
@RequestMapping("/sysOperationLog")
public class SysOperationLogController {

    @Resource
    private SysOperationLogService sysOperationLogService;

    /**
     * 分页查询操作日志列表
     *
     * @param reqDto 请求参数
     * @return 日志列表
     * @throws BlinkException 业务异常
     */
//    @OperationLog(type = LogType.OPERATION, description = "查询操作日志列表")
    @PostMapping("/getOperationLogList")
    public ResponseDTO<OperationLogRsp> getOperationLogList(@RequestBody @Validated RequestDTO<QueryOperationLogReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysOperationLogService.getOperationLogList(reqDto.getBody()));
    }

    /**
     * 查询操作日志详情
     *
     * @param reqDto 请求参数（包含logId）
     * @return 日志详情
     * @throws BlinkException 业务异常
     */
//    @OperationLog(type = LogType.OPERATION, description = "查询操作日志详情")
    @PostMapping("/getOperationLogDetail")
    public ResponseDTO<OperationLogDetailRsp> getOperationLogDetail(@RequestBody @Validated RequestDTO<DeleteSysOperationLogReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysOperationLogService.getOperationLogDetail(reqDto.getBody().getLogId()));
    }

}