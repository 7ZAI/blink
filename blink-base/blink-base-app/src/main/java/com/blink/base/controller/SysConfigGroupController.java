package com.blink.base.controller;

import com.blink.base.service.SysConfigGroupService;
import com.blink.base.dto.req.AddSysConfigGroupReqDTO;
import com.blink.base.dto.req.DeleteSysConfigGroupReqDTO;
import com.blink.base.dto.req.UpdateSysConfigGroupReqDTO;
import com.blink.base.dto.req.QuerySysConfigGroupReqDTO;
import com.blink.base.dto.rsp.QuerySysConfigGroupRspDTO;
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
 *
 *  参数分组表 管理API
 *
 *
 * @module blink
 * @author blink
 * @since 2025-10-16
 */
@RestController
@RequestMapping("/sysConfigGroup")
public class SysConfigGroupController {

    @Resource
    private SysConfigGroupService sysConfigGroupService;


    /**
     * 新增参数分组表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/saveSysConfigGroup")
    public ResponseDTO<EmptyBody> saveSysConfigGroup(@RequestBody @Validated RequestDTO<AddSysConfigGroupReqDTO> reqDto) throws BlinkException {
        sysConfigGroupService.saveSysConfigGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除参数分组表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/deleteSysConfigGroup")
    public ResponseDTO<EmptyBody> deleteSysConfigGroup(@RequestBody @Validated RequestDTO<DeleteSysConfigGroupReqDTO> reqDto) throws BlinkException {
        sysConfigGroupService.deleteSysConfigGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新参数分组表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/modifySysConfigGroup")
    public ResponseDTO<EmptyBody> modifySysConfigGroup(@RequestBody @Validated RequestDTO<UpdateSysConfigGroupReqDTO> reqDto) throws BlinkException {
        sysConfigGroupService.modifySysConfigGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 根据查询条件查询参数分组表列表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getSysConfigGroupList")
    public ResponseDTO<QuerySysConfigGroupRspDTO> getSysConfigGroupList(@RequestBody @Validated RequestDTO<QuerySysConfigGroupReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysConfigGroupService.getSysConfigGroupList(reqDto.getBody()));
    }


}
