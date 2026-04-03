package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 配置历史视图对象
 *
 * @author binblink
 */
@Data
public class ConfigHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 历史 ID
     */
    private Integer historyId;

    /**
     * 数据 ID
     */
    private String dataId;

    /**
     * 配置组
     */
    private String group;

    /**
     * 配置内容
     */
    private String content;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作时间
     */
    private String operationTime;

    /**
     * 操作人
     */
    private String operator;
}