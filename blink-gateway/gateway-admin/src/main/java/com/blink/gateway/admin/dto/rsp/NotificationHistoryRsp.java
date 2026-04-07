package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 消息历史响应
 *
 * @author binblink
 * @since 2026-04-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationHistoryRsp extends PageDTO<NotificationItemRsp> {
}
