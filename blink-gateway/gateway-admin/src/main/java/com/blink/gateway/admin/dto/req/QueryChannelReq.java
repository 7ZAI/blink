package com.blink.gateway.admin.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 查询渠道列表请求参数
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryChannelReq extends PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 渠道 ID
     */
    private String channelId;

    /**
     * 渠道名
     */
    private String channelName;

    /**
     * 应用 key 值
     */
    private String appKey;

    /**
     * 应用 secret 值
     */
    private String appSecret;

    /**
     * 渠道开关 0 开启 1 关闭
     */
    private Byte enable;

    /**
     * 加密开关 0 开启 1 关闭
     */
    private Byte encryptionSwitch;

    /**
     * 认证方式 0 带状态的 token  1 jwt
     */
    private Byte tokenType;

    /**
     * 权限校验开关 0 开启 1 关闭
     */
    private Byte authoritySwitch;
}
