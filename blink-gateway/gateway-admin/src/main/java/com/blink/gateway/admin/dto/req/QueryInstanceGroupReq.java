package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询实例分组列表请求参数
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryInstanceGroupReq extends Page {

    /**
     * 分组标识（模糊查询）
     */
    private String groupKey;

    /**
     * 分组名称（模糊查询）
     */
    private String groupName;

    /**
     * 状态：1启用 0禁用
     */
    private Byte status;
}
