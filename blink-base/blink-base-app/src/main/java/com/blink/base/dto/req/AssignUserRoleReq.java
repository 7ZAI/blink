package com.blink.base.dto.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AssignUserRoleReq 分配用户角色请求参数
 *
 * @author binblink
 */
@Data
public class AssignUserRoleReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID列表（批量分配）
     */
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Integer> userIdList;

    /**
     * 角色ID列表
     */
    @NotNull(message = "角色列表不能为空")
    private List<Integer> roleIdList;
}
