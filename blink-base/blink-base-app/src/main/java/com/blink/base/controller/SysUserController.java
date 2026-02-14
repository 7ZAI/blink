package com.blink.base.controller;

import com.blink.base.dto.req.AddSysUserReqDTO;
import com.blink.base.dto.req.DeleteSysUserReqDTO;
import com.blink.base.dto.req.QuerySysUserReqDTO;
import com.blink.base.dto.req.UpdateSysUserReqDTO;
import com.blink.base.dto.rsp.SysUserRspDTO;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.service.SysUserService;
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
 *  系统用户前端控制器
 *
 * @author binblink
 * @since 2023-12-26
 */
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    /**
     * 新增系统用户
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/saveSysUser")
    public ResponseDTO<EmptyBody> saveSysUser(@RequestBody @Validated RequestDTO<AddSysUserReqDTO> reqDto) throws BlinkException {
        sysUserService.saveSysUser(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除系统用户
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/deleteSysUser")
    public ResponseDTO<EmptyBody> deleteSysUser(@RequestBody @Validated RequestDTO<DeleteSysUserReqDTO> reqDto) throws BlinkException {
        sysUserService.deleteSysUser(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新系统用户
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/modifySysUser")
    public ResponseDTO<EmptyBody> modifySysUser(@RequestBody @Validated RequestDTO<UpdateSysUserReqDTO> reqDto) throws BlinkException {
        sysUserService.modifySysUser(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 根据查询条件查询系统用户列表 默认查询全部 由权限过滤器默认过滤组id 用户
     *
     * @param reqDto
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @PostMapping("/getSysUserList")
    public ResponseDTO<SysUserRspDTO> getSysUserList(@RequestBody @Validated RequestDTO<QuerySysUserReqDTO> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysUserService.getSysUserList(reqDto.getBody()));
    }


    /**
     * 根据用户名查询系统用户详情
     * @param reqDto
     * @return
     */
    @PostMapping("/getSysUserDetail")
    public ResponseDTO<SysUserVO> getSysUserDetail(@RequestBody @Validated RequestDTO<QuerySysUserReqDTO> reqDto) throws BlinkException {
        return  ResponseDTO.newSuccessInstance(sysUserService.getSysUserDetail(reqDto.getBody()));
    }
}
