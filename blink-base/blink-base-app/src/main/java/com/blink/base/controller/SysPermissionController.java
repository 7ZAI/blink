package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryPermissionIdentityRsp;
import com.blink.base.dto.rsp.QuerySysPermissionRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.base.service.SysPermissionService;
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
 * 权限菜单 管理API
 *
 * @author binblink
 * @module blink
 * @since 2024-01-13
 */
@RestController
@RequestMapping("/sysPermission")
public class SysPermissionController {

    @Resource
    private SysPermissionService sysPermissionService;


    /**
     * 新增权限菜单
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/saveSysPermission")
    public ResponseDTO<SysPermissionVO> saveSysPermission(@RequestBody @Validated RequestDTO<AddSysPermissionReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysPermissionService.saveSysPermission(reqDto.getBody()));
    }

    /**
     * 删除权限菜单
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/deleteSysPermission")
    public ResponseDTO<EmptyBody> deleteSysPermission(@RequestBody @Validated RequestDTO<DeleteSysPermissionReq> reqDto) throws BlinkException {
        sysPermissionService.deleteSysPermission(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新权限菜单
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/modifySysPermission")
    public ResponseDTO<EmptyBody> modifySysPermission(@RequestBody @Validated RequestDTO<UpdateSysPermissionReq> reqDto) throws BlinkException {
        sysPermissionService.modifySysPermission(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 根据查询条件查询权限菜单列表
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     * @throws Throwable
     */
    @PostMapping("/getSysPermissionList")
    public ResponseDTO<QuerySysPermissionRsp<SysPermissionDO>> getSysPermissionList(@RequestBody @Validated RequestDTO<QuerySysPermissionReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysPermissionService.getSysPermissionList(reqDto.getBody()));
    }

    /**
     * 根据url 查询 权限标识
     *
     * @param reqDto
     * @return {@link ResponseDTO< QueryPermissionIdentityRsp >}
     * @throws BlinkException
     */
    @PostMapping("/getPermissionByUrl")
    public ResponseDTO<QueryPermissionIdentityRsp> getPermissionByUrl(@RequestBody @Validated RequestDTO<QueryPermissionIdentityReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysPermissionService.getPermissionByUrl(reqDto.getBody()));
    }


    /**
     * 获取所有接口权限
     *
     * @param reqDto 空实体参数
     * @return {@link ResponseDTO<SysPermissionVO>}
     * @throws BlinkException
     */
    @PostMapping("/getAllApiPermission")
    public ResponseDTO<GetAllApiPermissionsRsp> getAllApiPermission(@RequestBody @Validated RequestDTO<GetAllApiPermissionsReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysPermissionService.getAllApiPermission(reqDto.getBody()));
    }


    /**
     * 根据用户id 查询权限标识
     *
     * @param reqDto 入参
     * @return {@link ResponseDTO< QueryUserPermissionRsp >}
     * @throws BlinkException
     */
    @PostMapping("/getPermissionsByUserId")
    public ResponseDTO<QueryUserPermissionRsp> getPermissionsByUserId(@RequestBody @Validated RequestDTO<QueryUserPermissionReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysPermissionService.getPermissionsByUserId(reqDto.getBody()));
    }


}
