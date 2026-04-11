package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 组件健康状态 VO
 *
 * @author binblink
 */
@Data
public class ComponentHealthVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 组件名称
     */
    private String name;

    /**
     * 状态
     */
    private String status;

    /**
     * 详情
     */
    private Map<String, Object> details;
}