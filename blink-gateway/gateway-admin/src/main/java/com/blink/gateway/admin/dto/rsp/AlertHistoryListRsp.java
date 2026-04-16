package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 告警历史列表响应
 *
 * @author binblink
 * @since 2026-04-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertHistoryListRsp extends PageDTO<AlertHistoryRsp> {

    @Serial
    private static final long serialVersionUID = 1L;
}