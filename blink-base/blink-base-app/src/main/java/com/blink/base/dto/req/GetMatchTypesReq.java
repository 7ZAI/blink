package com.blink.base.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 获取匹配类型选项请求
 *
 * @author binblink
 */
@Data
public class GetMatchTypesReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 过滤对象表名（如 sys_role）
     */
    private String tableName;

    /**
     * 关联关系名称（如"用户关联"、"角色关联"）
     */
    private String relationName;
}