package com.blink.gateway.admin.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 导入路由请求DTO
 *
 * @author binblink
 * @since 2026-04-12
 */
@Getter
@Setter
public class ImportRoutesReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * JSON格式的路由配置内容
     */
    private String jsonContent;

    /**
     * 是否覆盖已存在的路由
     */
    private Boolean overwrite;
}