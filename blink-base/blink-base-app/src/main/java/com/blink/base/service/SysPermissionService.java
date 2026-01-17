package com.blink.base.service;

import com.blink.base.dto.req.AddSysPermissionReqDTO;
import com.blink.base.dto.req.DeleteSysPermissionReqDTO;
import com.blink.base.dto.req.QuerySysPermissionReqDTO;
import com.blink.base.dto.req.UpdateSysPermissionReqDTO;
import com.blink.base.dto.rsp.QueryPermissionIdentityRspDTO;
import com.blink.base.dto.rsp.QuerySysPermissionRspDTO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.framework.common.exception.BlinkException;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 权限菜单 服务类
 * </p>
 *
 * @author binblink
 * @since 2024-01-13
 */
public interface SysPermissionService {

    /**
     * 保存 权限菜单
     *
     * @param saveParam
     * @return
     * @throws BlinkException
     */
    SysPermissionVO saveSysPermission(AddSysPermissionReqDTO saveParam) throws BlinkException;

    /**
     * 删除 权限菜单
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysPermission(DeleteSysPermissionReqDTO deleteParam) throws BlinkException;

    /**
     * 更新 权限菜单
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    void modifySysPermission(UpdateSysPermissionReqDTO updateParam) throws BlinkException;

    /**
     * 查询 权限菜单 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysPermissionRspDTO<SysPermissionDO> getSysPermissionList(QuerySysPermissionReqDTO queryParam) throws BlinkException;

    /**
     * 根据url 查询 权限标识
     * @param queryParam
     * @return {@link QueryPermissionIdentityRspDTO}
     * @throws Throwable
     */
    QueryPermissionIdentityRspDTO getPermissionByUrl(QuerySysPermissionReqDTO queryParam) throws BlinkException;

    /**
     * 根据角色获取权限集合 取角色权限交集
     *
     * @param roleIds 角色id
     * @return 取角色权限交集
     * @throws BlinkException
     */
    Set<String> getPermissionsByRoles(List<Integer> roleIds) throws BlinkException;
}
