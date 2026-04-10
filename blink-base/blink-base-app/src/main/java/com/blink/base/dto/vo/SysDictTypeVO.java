package com.blink.base.dto.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典类型表VO
 *
 * @author blink
 * @since 2025-03-07
 */
@Getter
@Setter
public class SysDictTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 显示顺序
     */
    private Integer orderNum;

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
     * 备注
     */
    private String remark;
}
