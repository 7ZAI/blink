package com.blink.gateway.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Getter;
import lombok.Setter;

/**
 * 查询数据过滤规则请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryDataFilterReq extends Page {

    /**
     * 过滤规则名称（模糊查询）
     */
    private String dataFilterName;

    /**
     * 实体类全限定名
     */
    private String entityClass;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 状态 0启用 1禁用
     */
    private Byte status;
}