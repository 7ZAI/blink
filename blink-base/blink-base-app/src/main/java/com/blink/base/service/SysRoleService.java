package com.blink.base.service;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysRoleRsp;
import com.blink.base.dto.rsp.QueryUserRolesRsp;
import com.blink.base.dto.vo.SysRoleVO;
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
}
