package com.blink.gateway.base.dto.req;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * QuerySysDictDataReq 查询字典数据列表请求参数对象
 * </p>
 *
 * @author blink
 * @since 2026-03-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class QuerySysDictDataReq extends PageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典数据主键id
     */
    private Integer dictCode;

    /**
     * 关联字典类型编码
     */
    private String dictType;

    /**
     * 字典标签（显示值）
     */
    private String dictLabel;

    /**
     * 字典键值（实际值）
     */
    private String dictValue;

    /**
     * 是否默认：0-否 1-是
     */
    private Boolean isDefault;

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
