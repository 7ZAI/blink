package com.blink.gateway.admin.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 推送配置请求参数
 *
 * @author binblink
 */
@Data
public class PushConfigReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置 DataId
     */
    private String dataId;

    /**
     * 配置 Group
     */
    private String group;

    /**
     * 配置内容
     */
    private String content;

    /**
     * 配置描述（可选）
     */
    private String description;
}
