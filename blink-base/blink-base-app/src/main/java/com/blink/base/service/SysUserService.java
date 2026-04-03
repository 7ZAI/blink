package com.blink.base.service;

import com.blink.base.dto.req.AddSysUserReq;
import com.blink.base.dto.req.AssignUserRoleReq;
import com.blink.base.dto.req.DeleteSysUserReq;
import com.blink.base.dto.req.ModifyPasswordReq;
import com.blink.base.dto.req.QuerySysUserReq;
import com.blink.base.dto.req.ResetPasswordReq;
import com.blink.base.dto.req.UserIdReq;
import com.blink.base.dto.req.UpdateSysUserReq;
import com.blink.base.dto.rsp.SysUserRsp;
import com.blink.base.dto.rsp.UserPermissionRsp;
import com.blink.base.dto.vo.SysUserVO;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 * 系统用户 服务类
 * </p>
 *
 * @author binblink
 * @since 2023-12-26
 */
public interface SysUserService {

    /**
     * 保存 系统用户
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    void saveSysUser(AddSysUserReq saveParam) throws BlinkException;

    /**
     * 删除 系统用户
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysUser(DeleteSysUserReq deleteParam) throws BlinkException;

    /**
     * 更新 系统用户
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    void modifySysUser(UpdateSysUserReq updateParam) throws BlinkException;

    /**
     * 查询 系统用户 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    SysUserRsp getSysUserList(QuerySysUserReq queryParam) throws BlinkException;


    /**
     * 查询 系统用户 详情
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    SysUserVO getSysUserDetail(QuerySysUserReq queryParam) throws BlinkException;

    /**
     * 锁定/解锁用户
     *
     * @param userId 用户ID
     * @param locked 锁定状态 0正常 1锁定
     * @throws BlinkException
     */
    void lockUser(Integer userId, Integer locked) throws BlinkException;

    /**
     * 批量分配用户角色
     *
     * @param assignParam 分配参数
     * @throws BlinkException
     */
    void assignUserRoles(AssignUserRoleReq assignParam) throws BlinkException;

    /**
     * 修改当前登录用户密码
     *
     * @param modifyPasswordParam 修改密码参数
     * @throws BlinkException
     */
    void modifyPassword(ModifyPasswordReq modifyPasswordParam) throws BlinkException;

    /**
     * 管理员重置用户密码
     *
     * @param resetPasswordParam 重置密码参数
     * @throws BlinkException
     */
    void resetPassword(ResetPasswordReq resetPasswordParam) throws BlinkException;

    /**
     * 获取用户权限信息
     *
     * @param reqParam 用户ID请求参数
     * @return 用户权限信息（角色、菜单、权限）
     * @throws BlinkException
     */
    UserPermissionRsp getUserPermissions(UserIdReq reqParam) throws BlinkException;
}
