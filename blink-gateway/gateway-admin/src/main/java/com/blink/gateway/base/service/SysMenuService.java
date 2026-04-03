package com.blink.gateway.base.service;

import com.blink.gateway.base.dto.req.*;
import com.blink.gateway.base.dto.rsp.CheckMenuRoleRsp;
import com.blink.gateway.base.dto.rsp.QueryShowMenuRsp;
import com.blink.gateway.base.dto.rsp.QuerySysMenuRsp;
import com.blink.gateway.base.dto.vo.SysMenuVO;
import com.blink.framework.common.exception.BlinkException;

/**
 * <p>
 * 系统菜单 服务类
 * </p>
 *
 * @author binblink
 * @since 2024-01-05
 */
public interface SysMenuService {


    /**
     * 保存 系统菜单
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    SysMenuVO saveSysMenu(AddSysMenuReq saveParam) throws BlinkException;

    /**
     * 删除 系统菜单
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysMenu(DeleteSysMenuReq deleteParam) throws BlinkException;

    /**
     * 更新 系统菜单
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    SysMenuVO modifySysMenu(UpdateSysMenuReq updateParam) throws BlinkException;

    /**
     * 查询 系统菜单 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysMenuRsp getSysMenuList(QuerySysMenuReq queryParam) throws BlinkException;

    /**
     * 根据用户查询其菜单 登入成功
     *
     * @param queryParam
     * @return {@link QueryShowMenuRsp}
     * @throws BlinkException
     */
    QueryShowMenuRsp getSysMenusByRoles(QueryShowMenuReq queryParam) throws BlinkException;

    /**
     * 获取所有菜单（不限制角色）
     * 用于超级管理员获取全部菜单
     *
     * @return {@link QueryShowMenuRsp}
     * @throws BlinkException
     */
    QueryShowMenuRsp getAllMenus() throws BlinkException;

    /**
     * 检查菜单是否已分配给角色
     *
     * @param reqParam 检查请求参数
     * @return 检查结果
     * @throws BlinkException 异常
     */
    CheckMenuRoleRsp checkMenuRoleAssignment(CheckMenuRoleReq reqParam) throws BlinkException;
}
