package com.blink.gateway.base.dto.req;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 查询组列表请求参数对象（树形数据不分页）
 *
 * @author binblink
 * @since 2024-01-04
 */
@Getter
@Setter
@ToString
public class QuerySysGroupReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组名称（模糊查询）
     */
    private String groupName;

    /**
     * 父组id
     */
    private Integer groupParentId;

    /**
     * 组领导
     */
    private String groupLeader;
}
