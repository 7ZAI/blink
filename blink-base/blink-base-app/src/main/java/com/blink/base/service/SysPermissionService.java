package com.blink.base.service;

import com.blink.base.dto.req.*;
import com.blink.base.dto.rsp.GetAllApiPermissionsRsp;
import com.blink.base.dto.rsp.QueryPermissionIdentityRsp;
import com.blink.base.dto.rsp.QuerySysPermissionRsp;
import com.blink.base.dto.rsp.QueryUserPermissionRsp;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.entity.SysPermissionDO;
import com.blink.framework.common.data.EmptyBody;
import com.blink.framework.common.data.ResponseDTO;
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
    SysPermissionVO saveSysPermission(AddSysPermissionReq saveParam) throws BlinkException;

    /**
     * 删除 权限菜单
     *
     * @param deleteParam
     * @return
     * @throws BlinkException
     */
    void deleteSysPermission(DeleteSysPermissionReq deleteParam) throws BlinkException;

    /**
     * 更新 权限菜单
     *
     * @param updateParam
     * @return
     * @throws BlinkException
     */
    void modifySysPermission(UpdateSysPermissionReq updateParam) throws BlinkException;

    /**
     * 查询 权限菜单 列表
     *
     * @param queryParam
     * @return
     * @throws BlinkException
     */
    QuerySysPermissionRsp<SysPermissionDO> getSysPermissionList(QuerySysPermissionReq queryParam) throws BlinkException;

    /**
     * 根据url 查询 权限标识
     *
     * @param queryParam
     * @return {@link QueryPermissionIdentityRsp}
     * @throws Throwable
     */
    QueryPermissionIdentityRsp getPermissionByUrl(QueryPermissionIdentityReq queryParam) throws BlinkException;

    /**
     * 根据角色获取权限集合 取角色权限交集
     *
     * @param roleIds 角色id
     * @return 取角色权限交集
     * @throws BlinkException
     */
    Set<String> getPermissionsByRoles(List<Integer> roleIds) throws BlinkException;

    /**
     * 根据用户id或url 查询权限标识
     *
     * @param reqDTO 用户id或ur DTO
     * @return 权限集合
     * @throws BlinkException
     */
    QueryUserPermissionRsp getPermissions(QueryUserPermissionReq reqDTO) throws BlinkException;

    /**
     * 获取所有接口权限
     *
     * @param body 空实体参数
     * @return {@link ResponseDTO <SysPermissionVO>}
     * @throws BlinkException
     */
    GetAllApiPermissionsRsp getAllApiPermission(GetAllApiPermissionsReq body) throws BlinkException;
}
