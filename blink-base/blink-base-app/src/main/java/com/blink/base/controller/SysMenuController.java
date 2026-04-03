package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.CheckMenuRoleRsp;
import com.blink.base.dto.rsp.QueryShowMenuRsp;
import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.service.SysMenuService;
import com.blink.base.dto.rsp.QuerySysMenuRsp;
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
 *  系统菜单 管理API
 *
 * @module blink
 * @author binblink
 * @since 2024-01-05
 */
@RestController
@RequestMapping("/sysMenu")
public class SysMenuController {

    @Resource
    private SysMenuService sysMenuService;


    /**
     * 新增系统菜单
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/saveSysMenu")
    public ResponseDTO<SysMenuVO> saveSysMenu(@RequestBody @Validated RequestDTO<AddSysMenuReq> reqDto) throws BlinkException {

        return ResponseDTO.newSuccessInstance(sysMenuService.saveSysMenu(reqDto.getBody()));
    }

    /**
     * 删除系统菜单
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/deleteSysMenu")
    public ResponseDTO<EmptyBody> deleteSysMenu(@RequestBody @Validated RequestDTO<DeleteSysMenuReq> reqDto) throws BlinkException {
        sysMenuService.deleteSysMenu(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新系统菜单
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/modifySysMenu")
    public ResponseDTO<SysMenuVO> modifySysMenu(@RequestBody @Validated RequestDTO<UpdateSysMenuReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysMenuService.modifySysMenu(reqDto.getBody()));
    }

    /**
     * 根据查询条件查询系统菜单列表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getSysMenuList")
    public ResponseDTO<QuerySysMenuRsp> getSysMenuList(@RequestBody @Validated RequestDTO<QuerySysMenuReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysMenuService.getSysMenuList(reqDto.getBody()));
    }

    /**
     * 根据用户查询其菜单 登入成功
     *
     * @param reqDto
     * @return {@link ResponseDTO<QueryShowMenuReq>}
     * @throws BlinkException
     */
    @PostMapping("/getSysMenusByRoles")
    public ResponseDTO<QueryShowMenuRsp> getSysMenusByRoles(@RequestBody @Validated RequestDTO<QueryShowMenuReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysMenuService.getSysMenusByRoles(reqDto.getBody()));
    }

    /**
     * 检查菜单是否已分配给角色
     *
     * @param reqDto 请求参数
     * @return 检查结果
     * @throws BlinkException 异常
     */
    @PostMapping("/checkMenuRoleAssignment")
    public ResponseDTO<CheckMenuRoleRsp> checkMenuRoleAssignment(@RequestBody @Validated RequestDTO<CheckMenuRoleReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysMenuService.checkMenuRoleAssignment(reqDto.getBody()));
    }

}
