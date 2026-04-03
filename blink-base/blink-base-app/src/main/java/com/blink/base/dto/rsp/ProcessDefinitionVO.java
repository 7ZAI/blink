package com.blink.base.dto.rsp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程定义VO
 *
 * @author binblink
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDefinitionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程定义KEY
     */
    private String processDefinitionKey;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 部署ID
     */
    private String deploymentId;

    /**
     * 部署时间
     */
    private LocalDateTime deploymentTime;

    /**
     * 是否挂起
     */
    private Boolean suspended;

    /**
     * 是否最新版本
     */
    private Boolean latestVersion;
}