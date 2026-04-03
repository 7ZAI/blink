package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * LockUserReq 锁定用户请求参数
 *
 * @author binblink
 */
@Data
public class LockUserReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /**
     * 锁定状态 0正常 1锁定
     */
    @NotNull(message = "锁定状态不能为空")
    private Integer locked;
}
