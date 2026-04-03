package com.blink.gateway.base.service;

import com.blink.gateway.base.dto.req.*;
import com.blink.gateway.base.dto.rsp.QuerySysRoleRsp;
import com.blink.gateway.base.dto.rsp.QueryUserRolesRsp;
import com.blink.gateway.base.dto.rsp.RoleDetailRsp;
import com.blink.gateway.base.dto.vo.SysRoleVO;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 * 系统角色 服务类
 * </p>
 *
 * @author binblink
 * @since 2024-01-03
 */
public interface SysRoleService {

    /**
     * 保存 系统角色
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    SysRoleVO saveSysRole(AddSysRoleReq saveParam) throws BlinkException;

    /**
     * 删除 系统角色
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysRole(DeleteSysRoleReq deleteParam) throws BlinkException;

    /**
     * 更新 系统角色
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    SysRoleVO modifySysRole(UpdateSysRoleReq updateParam) throws BlinkException;

    /**
     * 查询 系统角色 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysRoleRsp getSysRoleList(QuerySysRoleReq queryParam) throws BlinkException;

    /**
     * 根据用户信息查询 用户角色
     *
     * @param queryParam
     * @return {@link QueryUserRolesRsp}
     * @throws BlinkException
     */
    QueryUserRolesRsp getSysRolesByUser(QueryUserRolesReq queryParam) throws BlinkException;

    /**
     * 为角色分配权限
     *
     * @param assignParam 分配参数
     * @throws BlinkException
     */
    void assignPermissions(AssignPermissionReq assignParam) throws BlinkException;

    /**
     * 为角色分配菜单
     *
     * @param assignParam 分配参数
     * @throws BlinkException
     */
    void assignMenus(AssignMenuReq assignParam) throws BlinkException;

    /**
     * 查询角色详情
     *
     * @param queryParam 查询参数
     * @return 角色详情
     * @throws BlinkException
     */
    RoleDetailRsp getRoleDetail(QueryRoleDetailReq queryParam) throws BlinkException;

    /**
     * 为用户分配角色
     *
     * @param assignParam 分配参数
     * @throws BlinkException
     */
    void assignRoleToUsers(AssignRoleToUsersReq assignParam) throws BlinkException;
}
