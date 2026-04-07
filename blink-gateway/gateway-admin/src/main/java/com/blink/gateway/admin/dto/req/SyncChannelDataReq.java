package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

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
     * 渠道 ID 列表（可选，为空则同步所有渠道）
     */
    private List<String> channelIds;

    /**
     * 同步类型：0-全量同步，1-增量同步
     */
    private Byte syncType;
}
