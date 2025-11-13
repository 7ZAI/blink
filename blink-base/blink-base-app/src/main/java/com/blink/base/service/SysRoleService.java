package com.blink.base.service;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QuerySysRoleRspDTO;
import com.blink.base.dto.rsp.QueryUserRolesRspDTO;
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
    SysRoleVO saveSysRole(AddSysRoleReqDTO saveParam) throws BlinkException;

    /**
     * 删除 系统角色
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysRole(DeleteSysRoleReqDTO deleteParam) throws BlinkException;

    /**
     * 更新 系统角色
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    SysRoleVO modifySysRole(UpdateSysRoleReqDTO updateParam) throws BlinkException;

    /**
     * 查询 系统角色 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysRoleRspDTO getSysRoleList(QuerySysRoleReqDTO queryParam) throws BlinkException;

    /**
     * 根据用户信息查询 用户角色
     *
     * @param queryParam
     * @return {@link QueryUserRolesRspDTO}
     * @throws BlinkException
     */
    QueryUserRolesRspDTO getSysRolesByUser(QueryUserRolesReqDTO queryParam) throws BlinkException;
}
