package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 健康状态详情 VO
 *
 * @author binblink
 */
@Data
public class HealthDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 整体状态
     */
    private String status;

    /**
     * 各组件健康状态
     */
    private List<ComponentHealthVO> components;
}