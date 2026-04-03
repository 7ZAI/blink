package com.blink.gateway.base.dto.req;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增数据过滤规则请求DTO
 *
 * @author binblink
 */
@Getter
@Setter
public class AddDataFilterReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 过滤规则名称
     */
    @NotBlank(message = "过滤规则名称不能为空")
    private String dataFilterName;

    /**
     * 过滤规则英文名称
     */
    private String dataFilterEnName;

    /**
     * 实体类全限定名
     */
    @NotBlank(message = "实体类不能为空")
    private String entityClass;

    /**
     * 对应表名
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * 规则类型
     */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    /**
     * 规则配置JSON
     */
    @NotBlank(message = "规则配置不能为空")
    private String ruleConfig;

    /**
     * 备注
     */
    private String remark;
}