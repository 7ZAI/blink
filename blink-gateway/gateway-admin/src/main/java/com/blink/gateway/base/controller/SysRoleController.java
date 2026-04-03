package com.blink.gateway.base.controller;

import com.blink.gateway.base.dto.req.*;
import com.blink.gateway.base.dto.rsp.QueryUserRolesRsp;
import com.blink.gateway.base.dto.rsp.RoleDetailRsp;
import com.blink.gateway.base.dto.vo.SysRoleVO;
import com.blink.gateway.base.service.SysRoleService;
import com.blink.gateway.base.dto.rsp.QuerySysRoleRsp;
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
    public ResponseDTO<SysRoleVO> saveSysRole(@RequestBody @Validated RequestDTO<AddSysRoleReq> reqDto) throws BlinkException {
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
    public ResponseDTO<EmptyBody> deleteSysRole(@RequestBody @Validated RequestDTO<DeleteSysRoleReq> reqDto) throws BlinkException {
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
    public ResponseDTO<SysRoleVO> modifySysRole(@RequestBody @Validated RequestDTO<UpdateSysRoleReq> reqDto) throws BlinkException {
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
    public ResponseDTO<QuerySysRoleRsp> getSysRoleList(@RequestBody @Validated RequestDTO<QuerySysRoleReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysRoleService.getSysRoleList(reqDto.getBody()));
    }


    /**
     * 根据用户信息查询 用户角色
     *
     * @param reqDto
     * @return {@link ResponseDTO<    QueryUserRolesReq    >}
     * @throws BlinkException
     */
    @PostMapping("/getSysRolesByUser")
    public ResponseDTO<QueryUserRolesRsp> getSysRolesByUser(@RequestBody @Validated RequestDTO<QueryUserRolesReq> reqDto) throws BlinkException {
        return  ResponseDTO.newSuccessInstance(sysRoleService.getSysRolesByUser(reqDto.getBody()));
    }

    /**
     * 为角色分配权限
     *
     * @param reqDto 请求参数
     * @return 响应
     * @throws BlinkException
     */
    @PostMapping("/assignPermissions")
    public ResponseDTO<EmptyBody> assignPermissions(@RequestBody @Validated RequestDTO<AssignPermissionReq> reqDto) throws BlinkException {
        sysRoleService.assignPermissions(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 为角色分配菜单
     *
     * @param reqDto 请求参数
     * @return 响应
     * @throws BlinkException
     */
    @PostMapping("/assignMenus")
    public ResponseDTO<EmptyBody> assignMenus(@RequestBody @Validated RequestDTO<AssignMenuReq> reqDto) throws BlinkException {
        sysRoleService.assignMenus(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 查询角色详情
     *
     * @param reqDto 请求参数
     * @return 角色详情
     * @throws BlinkException
     */
    @PostMapping("/getRoleDetail")
    public ResponseDTO<RoleDetailRsp> getRoleDetail(@RequestBody @Validated RequestDTO<QueryRoleDetailReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysRoleService.getRoleDetail(reqDto.getBody()));
    }

    /**
     * 为用户分配角色
     *
     * @param reqDto 请求参数
     * @return 响应
     * @throws BlinkException
     */
    @PostMapping("/assignRoleToUsers")
    public ResponseDTO<EmptyBody> assignRoleToUsers(@RequestBody @Validated RequestDTO<AssignRoleToUsersReq> reqDto) throws BlinkException {
        sysRoleService.assignRoleToUsers(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

}
