package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同步渠道数据请求参数
 *
 * @author binblink
 */
@Data
public class SyncChannelDataReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 渠道 ID（可选，为空则同步所有渠道）
     */
    private Integer channelId;

    /**
     * 同步类型：0-全量同步，1-增量同步
     */
    private Byte syncType;
}
