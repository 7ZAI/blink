package com.blink.base.controller;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.SysUserRsp;
import com.blink.base.dto.rsp.UserPermissionRsp;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.base.dto.vo.UserPreferenceVO;
import com.blink.base.service.SysUserPreferenceService;
import com.blink.base.service.SysUserService;
import com.blink.framework.common.context.BlinkRequestContextHolder;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
import com.blink.framework.common.exception.BlinkException;
import com.blink.log.annotation.RecordLog;
import com.blink.log.constant.LogType;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统用户前端控制器
 *
 * @author binblink
 * @since 2023-12-26
 */
@RestController
@RequestMapping("/sysUser")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysUserPreferenceService sysUserPreferenceService;

    /**
     * 新增系统用户
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @RecordLog(type = LogType.OPERATION, description = "新增系统用户")
    @PostMapping("/saveSysUser")
    public ResponseDTO<EmptyBody> saveSysUser(@RequestBody @Validated RequestDTO<AddSysUserReq> reqDto) throws BlinkException {
        sysUserService.saveSysUser(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 删除系统用户
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @RecordLog(type = LogType.OPERATION, description = "删除系统用户")
    @PostMapping("/deleteSysUser")
    public ResponseDTO<EmptyBody> deleteSysUser(@RequestBody @Validated RequestDTO<DeleteSysUserReq> reqDto) throws BlinkException {
        sysUserService.deleteSysUser(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 更新系统用户
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<EmptyBody>}
     */
    @RecordLog(type = LogType.OPERATION, description = "修改系统用户")
    @PostMapping("/modifySysUser")
    public ResponseDTO<EmptyBody> modifySysUser(@RequestBody @Validated RequestDTO<UpdateSysUserReq> reqDto) throws BlinkException {
        sysUserService.modifySysUser(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 根据查询条件查询系统用户列表 默认查询全部 由权限过滤器默认过滤组id 用户
     *
     * @param reqDto 请求参数
     * @return {@link ResponseDTO<SysUserRsp>}
     */
    @PostMapping("/getSysUserList")
    public ResponseDTO<SysUserRsp> getSysUserList(@RequestBody @Validated RequestDTO<QuerySysUserReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysUserService.getSysUserList(reqDto.getBody()));
    }

    /**
     * 根据用户名查询系统用户详情
     *
     * @param reqDto 请求参数
     * @return 用户详情
     */
    @PostMapping("/getSysUserDetail")
    public ResponseDTO<SysUserVO> getSysUserDetail(@RequestBody @Validated RequestDTO<QuerySysUserReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysUserService.getSysUserDetail(reqDto.getBody()));
    }

    /**
     * 锁定/解锁用户
     *
     * @param reqDto 请求参数
     * @return 响应
     */
    @RecordLog(type = LogType.OPERATION, description = "锁定/解锁用户")
    @PostMapping("/lockUser")
    public ResponseDTO<EmptyBody> lockUser(@RequestBody @Validated RequestDTO<LockUserReq> reqDto) throws BlinkException {
        sysUserService.lockUser(reqDto.getBody().getUserId(), reqDto.getBody().getLocked());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 批量分配用户角色
     *
     * @param reqDto 请求参数
     * @return 响应
     * @throws BlinkException 业务异常
     */
    @RecordLog(type = LogType.OPERATION, description = "分配用户角色")
    @PostMapping("/assignUserRoles")
    public ResponseDTO<EmptyBody> assignUserRoles(@RequestBody @Validated RequestDTO<AssignUserRoleReq> reqDto) throws BlinkException {
        sysUserService.assignUserRoles(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 修改当前登录用户密码
     *
     * @param reqDto 请求参数
     * @return 响应
     * @throws BlinkException 业务异常
     */
    @RecordLog(type = LogType.OPERATION, description = "修改密码")
    @PostMapping("/modifyPassword")
    public ResponseDTO<EmptyBody> modifyPassword(@RequestBody @Validated RequestDTO<ModifyPasswordReq> reqDto) throws BlinkException {
        sysUserService.modifyPassword(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 管理员重置用户密码
     *
     * @param reqDto 请求参数
     * @return 响应
     * @throws BlinkException 业务异常
     */
    @RecordLog(type = LogType.OPERATION, description = "重置密码")
    @PostMapping("/resetPassword")
    public ResponseDTO<EmptyBody> resetPassword(@RequestBody @Validated RequestDTO<ResetPasswordReq> reqDto) throws BlinkException {
        sysUserService.resetPassword(reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 保存当前登录用户偏好设置
     *
     * @param reqDto 请求参数
     * @return 响应
     * @throws BlinkException 业务异常
     */
    @RecordLog(type = LogType.OPERATION, description = "保存偏好设置")
    @PostMapping("/saveUserPreference")
    public ResponseDTO<EmptyBody> saveUserPreference(@RequestBody @Validated RequestDTO<SaveUserPreferenceReq> reqDto) throws BlinkException {
        Integer userId = Integer.valueOf(BlinkRequestContextHolder.getUserId());
        sysUserPreferenceService.saveOrUpdatePreference(userId, reqDto.getBody());
        return ResponseDTO.newSuccessInstance();
    }

    /**
     * 获取当前登录用户偏好设置
     *
     * @param reqDto 请求参数
     * @return 响应
     * @throws BlinkException 业务异常
     */
    @RecordLog(type = LogType.OPERATION, description = "获取偏好设置")
    @PostMapping("/getUserPreference")
    public ResponseDTO<UserPreferenceVO> getUserPreference(@RequestBody RequestDTO<EmptyBody> reqDto) throws BlinkException {
        Integer userId = Integer.valueOf(BlinkRequestContextHolder.getUserId());
        return ResponseDTO.newSuccessInstance(sysUserPreferenceService.getPreferenceByUserId(userId));
    }

    /**
     * 获取用户权限信息
     *
     * @param reqDto 请求参数
     * @return 用户权限信息
     * @throws BlinkException 业务异常
     */
    @PostMapping("/getUserPermissions")
    public ResponseDTO<UserPermissionRsp> getUserPermissions(@RequestBody @Validated RequestDTO<UserIdReq> reqDto) throws BlinkException {
        return ResponseDTO.newSuccessInstance(sysUserService.getUserPermissions(reqDto.getBody()));
    }
}