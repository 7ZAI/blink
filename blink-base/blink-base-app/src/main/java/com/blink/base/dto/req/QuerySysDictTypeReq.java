package com.blink.base.dto.req;

import com.blink.framework.common.data.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 查询字典类型表列表请求参数对象
 *
 * @author blink
 * @since 2025-03-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QuerySysDictTypeReq extends Page {

    /**
     * 字典主键id
     */
    private Integer dictId;

    /**
     * 字典类型编码（唯一标识）
     */
    private String dictType;

    /**
     * 字典类型名称
     */
    private String dictName;

    /**
     * 状态：0-启用 1-禁用
     */
    private Boolean status;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 语言标识
     */
    private String locale;
}
