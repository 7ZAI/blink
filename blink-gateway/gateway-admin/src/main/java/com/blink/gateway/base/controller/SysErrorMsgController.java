package com.blink.gateway.base.controller;


import com.blink.base.dto.req.QueryErrMsgReq;
import com.blink.base.dto.rsp.QueryErrMsgRsp;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.gateway.base.service.SysErrorMsgService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *   错误码查询 API
 *   仅为内部调用 不对外公开
 *   internal 为内部调用api专用前缀
 * </p>
 *
 * @author binblink
 *
 * @module blink
 */
@RestController
@RequestMapping("/internal/error/msg")
public class SysErrorMsgController {

    @Resource
    private SysErrorMsgService sysErrorMsgService;


    /**
     * 根据错误码和语言查询单个错误码消息
     *
     * @param requestDTO 请求参数，包含错误码和语言
     * @return ResponseDTO<QueryErrMsgRsp> 错误消息响应
     * @throws BlinkException 当错误消息不存在时抛出业务异常
     */
    @PostMapping("/getMsg")
    public ResponseDTO<QueryErrMsgRsp> getMsg(@RequestBody @Validated RequestDTO<QueryErrMsgReq> requestDTO) throws BlinkException {
        QueryErrMsgRsp rspDTO = sysErrorMsgService.getErrorMsg(requestDTO.getBody());
        return ResponseDTO.newSuccessInstance(rspDTO);
    }
}
