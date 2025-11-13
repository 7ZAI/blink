package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QueryShowMenuRspDTO;
import com.blink.base.dto.vo.SysMenuVO;
import com.blink.base.service.SysMenuService;
import com.blink.base.dto.rsp.QuerySysMenuRspDTO;
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
    public ResponseDTO<SysMenuVO> saveSysMenu(@RequestBody @Validated RequestDTO<AddSysMenuReqDTO> reqDto) throws BlinkException {

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
    public ResponseDTO<EmptyBody> deleteSysMenu(@RequestBody @Validated RequestDTO<DeleteSysMenuReqDTO> reqDto) throws BlinkException {
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
    public ResponseDTO<SysMenuVO> modifySysMenu(@RequestBody @Validated RequestDTO<UpdateSysMenuReqDTO> reqDto) throws BlinkException {
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
    public ResponseDTO<QuerySysMenuRspDTO> getSysMenuList(@RequestBody @Validated RequestDTO<QuerySysMenuReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysMenuService.getSysMenuList(reqDto.getBody()));
    }

    /**
     * 根据用户查询其菜单 登入成功
     *
     * @param reqDto
     * @return {@link ResponseDTO<QueryShowMenuReqDTO>}
     * @throws BlinkException
     */
    @PostMapping("/getSysMenusByRoles")
    public ResponseDTO<QueryShowMenuRspDTO> getSysMenusByRoles(@RequestBody @Validated RequestDTO<QueryShowMenuReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysMenuService.getSysMenusByRoles(reqDto.getBody()));
    }


}
