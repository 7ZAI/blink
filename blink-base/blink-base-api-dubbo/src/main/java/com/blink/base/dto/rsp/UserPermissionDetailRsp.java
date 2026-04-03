package com.blink.base.dto.rsp;

import com.blink.base.dto.vo.DataFilterVO;
import com.blink.base.dto.vo.SysPermissionVO;
import com.blink.base.dto.vo.SysRoleVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 用户权限详情响应
 *
 * @author binblink
 */
@Data
public class UserPermissionDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色列表
     */
    private List<SysRoleVO> roles;

    /**
     * 接口权限列表
     */
    private List<SysPermissionVO> permissions;

    /**
     * 数据过滤权限列表
     */
    private List<DataFilterVO> dataFilters;
}