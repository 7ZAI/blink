package com.blink.gateway.base.dto.rsp;

import com.blink.gateway.base.dto.vo.SysRoleVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 检查菜单角色分配响应结果
 *
 * @author binblink
 * @since 2026-03-23
 */
@Data
public class CheckMenuRoleRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否已分配给角色
     */
    private Boolean assigned;

    /**
     * 已分配的角色列表
     */
    private List<SysRoleVO> roles;

    /**
     * 菜单当前绑定的权限ID
     */
    private Integer currentPermId;

    /**
     * 权限是否发生变更
     */
    private Boolean permChanged;
}