package com.blink.base.service;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.QueryShowMenuRspDTO;
import com.blink.base.dto.rsp.QuerySysMenuRspDTO;
import com.blink.base.dto.vo.SysMenuVO;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.RequestDTO;
import com.blink.framework.common.data.ResponseDTO;
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
    SysMenuVO saveSysMenu(AddSysMenuReqDTO saveParam) throws BlinkException;

    /**
     * 删除 系统菜单
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysMenu(DeleteSysMenuReqDTO deleteParam) throws BlinkException;

    /**
     * 更新 系统菜单
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    SysMenuVO modifySysMenu(UpdateSysMenuReqDTO updateParam) throws BlinkException;

    /**
     * 查询 系统菜单 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysMenuRspDTO getSysMenuList(QuerySysMenuReqDTO queryParam) throws BlinkException;

    /**
     * 根据用户查询其菜单 登入成功
     *
     * @param queryParam
     * @return {@link QueryShowMenuRspDTO}
     * @throws BlinkException
     */
    QueryShowMenuRspDTO getSysMenusByRoles(QueryShowMenuReqDTO queryParam) throws BlinkException;
}
