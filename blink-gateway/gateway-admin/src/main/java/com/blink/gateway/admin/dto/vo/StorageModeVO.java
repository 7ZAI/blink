package com.blink.gateway.admin.dto.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存储方式 VO
 *
 * @author binblink
 */
@Getter
@Setter
public class StorageModeVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 存储模式标识
     */
    private String mode;

    /**
     * 存储方式名称
     */
    private String name;

    /**
     * 存储方式描述
     */
    private String description;
}