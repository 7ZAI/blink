package com.blink.base.controller;

import com.blink.base.dto.rsp.QueryGateWayRoutesRspDTO;
import com.blink.base.entity.RouteDefinitionDO;
import com.blink.base.service.GateWayRoutesService;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * <p>
 * 网关路由管理 动态路由实现
 * </p>
 *
 * @author binblink
 * @module blink
 */
@Controller
@RequestMapping("/routes")
public class GateWayRoutesController {

    @Autowired
    private GateWayRoutesService gateWayRoutesService;

    /**
     * 保存对接渠道
     *
     * @param reqDto
     * @return {@link ResponseDTO < EmptyBody >}
     * @throws BlinkException
     */
    @PostMapping("/saveRoute")
    public ResponseDTO<EmptyBody> saveRoute(@RequestBody @Validated RequestDTO<List<RouteDefinitionDO>> reqDto) throws BlinkException {
        gateWayRoutesService.saveRoute(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除对接渠道
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws BlinkException
     */
    @PostMapping("/deleteRoute")
    public ResponseDTO<EmptyBody> deleteRoute(@RequestBody @Validated RequestDTO<List<String>> reqDto) throws BlinkException {
        gateWayRoutesService.deleteRoute(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 根据查询条件查询对接渠道列表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws BlinkException
     */
    @PostMapping("/getChannelList")
    public ResponseDTO<QueryGateWayRoutesRspDTO> getRouteslList(@RequestBody @Validated RequestDTO<EmptyBody> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(gateWayRoutesService.getRouteslList(reqDto.getBody()));
    }
}
