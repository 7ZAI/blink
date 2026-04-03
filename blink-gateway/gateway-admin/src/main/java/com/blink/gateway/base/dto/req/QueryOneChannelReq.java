package com.blink.gateway.base.dto.req;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * QueryOneChannelReqDTO 查询单个 channel信息请求参数
 * </p>
 *
 * @author binblink
 * @since 2024-07-29
 */
@Data
public class QueryOneChannelReq implements Serializable {

    private static final long serialVersionUID = 3925418753803274699L;

    /**
     * 渠道id
     */
    private String channelId;


    /**
     * 渠道名
     */
    private String channelName;


    /**
     * 应用key值
     */
    private String appKey;


}
