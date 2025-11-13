package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QueryUserRolesRspDTO;
import com.blink.base.dto.vo.SysRoleVO;
import com.blink.base.service.SysRoleService;
import com.blink.base.dto.rsp.QuerySysRoleRspDTO;
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
 *  系统角色 管理API
 *
 * @module blink
 * @author binblink
 * @since 2024-01-03
 */
@RestController
@RequestMapping("/sysRole")
public class SysRoleController {

    @Resource
    private SysRoleService sysRoleService;

    /**
     * 新增系统角色
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/saveSysRole")
    public ResponseDTO<SysRoleVO> saveSysRole(@RequestBody @Validated RequestDTO<AddSysRoleReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysRoleService.saveSysRole(reqDto.getBody()));
    }

    /**
     * 删除系统角色
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/deleteSysRole")
    public ResponseDTO<EmptyBody> deleteSysRole(@RequestBody @Validated RequestDTO<DeleteSysRoleReqDTO> reqDto) throws BlinkException {
        sysRoleService.deleteSysRole(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新系统角色
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/modifySysRole")
    public ResponseDTO<SysRoleVO> modifySysRole(@RequestBody @Validated RequestDTO<UpdateSysRoleReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysRoleService.modifySysRole(reqDto.getBody()));
    }

    /**
     * 根据查询条件查询系统角色列表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getSysRoleList")
    public ResponseDTO<QuerySysRoleRspDTO> getSysRoleList(@RequestBody @Validated RequestDTO<QuerySysRoleReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysRoleService.getSysRoleList(reqDto.getBody()));
    }


    /**
     * 根据用户信息查询 用户角色
     *
     * @param reqDto
     * @return {@link ResponseDTO<QueryUserRolesReqDTO>}
     * @throws BlinkException
     */
    @PostMapping("/getSysRolesByUser")
    public ResponseDTO<QueryUserRolesRspDTO> getSysRolesByUser(@RequestBody @Validated RequestDTO<QueryUserRolesReqDTO> reqDto) throws BlinkException {
        return  ResponseDTO.newSuccessInstance(sysRoleService.getSysRolesByUser(reqDto.getBody()));
    }


}
