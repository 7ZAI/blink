package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分页查询实例列表请求参数
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryInstanceReq extends Page {

    /**
     * 服务ID（可选，用于过滤）
     */
    private String serviceId;

    /**
     * 主机地址（可选，用于过滤）
     */
    private String host;

    /**
     * 实例状态（可选，用于过滤）
     * 0-在线，1-离线，2-下线
     */
    private Byte status;

    /**
     * 分组标识（可选，用于过滤）
     */
    private String groupKey;
}