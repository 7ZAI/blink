package com.blink.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询数据过滤规则请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class QueryDataFilterReq extends PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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