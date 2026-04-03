package com.blink.gateway.base.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * 委托任务请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
public class DelegateTaskReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 当前用户ID
     */
    @NotBlank(message = "当前用户ID不能为空")
    private String currentUserId;

    /**
     * 目标用户ID（被委托人）
     */
    @NotBlank(message = "被委托人ID不能为空")
    private String targetUserId;
}