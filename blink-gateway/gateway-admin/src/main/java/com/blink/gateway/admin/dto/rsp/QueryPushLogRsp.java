package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import com.blink.gateway.admin.entity.GaRoutePushLogDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 推送历史响应
 *
 * @author binblink
 * @since 2026-04-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryPushLogRsp extends PageDTO<GaRoutePushLogDO> {

    @Serial
    private static final long serialVersionUID = 1L;
}