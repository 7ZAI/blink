package com.blink.base.controller;

import com.blink.base.dto.rsp.SysGroupRsp;
import com.blink.base.service.SysGroupService;
import com.blink.base.dto.req.AddSysGroupReq;
import com.blink.base.dto.req.DeleteSysGroupReq;
import com.blink.base.dto.req.UpdateSysGroupReq;
import com.blink.base.dto.req.QuerySysGroupReq;
import com.blink.base.dto.rsp.QuerySysGroupRsp;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.framework.common.record.RequestRecord;
import com.blink.framework.common.record.ResponseRecord;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *  用户组织
 *  组 管理API
 *
 *
 * @author blink
 * @since 2025-10-16
 */
@RestController
@RequestMapping("/sysGroup")
public class SysGroupController {

    @Resource
    private SysGroupService sysGroupService;


    /**
     * 新增组
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/saveSysGroup")
    public ResponseRecord<SysGroupRsp> saveSysGroup(@RequestBody @Validated RequestRecord<AddSysGroupReq> reqDto) throws BlinkException {
        return ResponseRecord.newSuccessInstance(sysGroupService.saveSysGroup(reqDto.body()));
    }

    /**
     * 删除组
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/deleteSysGroup")
    public ResponseDTO<EmptyBody> deleteSysGroup(@RequestBody @Validated RequestDTO<DeleteSysGroupReq> reqDto) throws BlinkException {
        sysGroupService.deleteSysGroup(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新组
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/modifySysGroup")
    public ResponseDTO<SysGroupRsp> modifySysGroup(@RequestBody @Validated RequestDTO<UpdateSysGroupReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysGroupService.modifySysGroup(reqDto.getBody()));
    }

    /**
     * 根据查询条件查询组列表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getSysGroupList")
    public ResponseDTO<QuerySysGroupRsp> getSysGroupList(@RequestBody @Validated RequestDTO<QuerySysGroupReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysGroupService.getSysGroupList(reqDto.getBody()));
    }


}
