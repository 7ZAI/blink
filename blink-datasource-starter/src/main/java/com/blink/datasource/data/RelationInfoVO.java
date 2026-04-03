package com.blink.datasource.data;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 关联关系信息VO
 * 表示从某个实体出发可用的关联关系
 *
 * @author binblink
 */
@Data
public class RelationInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联关系名称（如"角色关联"、"用户关联"）
     */
    private String name;

    /**
     * 关联关系英文名称
     */
    private String enName;

    /**
     * 关联表名
     */
    private String relationTable;

    /**
     * 当前实体的关联字段（如 sys_user.user_id）
     */
    private String sourceField;

    /**
     * 关联表中关联当前实体的字段
     */
    private String relationSourceField;

    /**
     * 关联表中关联目标实体的字段
     */
    private String relationTargetField;

    /**
     * 目标实体表名（用于前端显示匹配值选择器类型）
     */
    private String targetTable;

    /**
     * 目标实体字段名
     */
    private String targetField;

    /**
     * 目标实体名称（如"角色"、"部门"）
     */
    private String targetName;

    /**
     * 支持的匹配类型
     */
    private List<String> supportMatchTypes;

    /**
     * 匹配类型的描述（key为匹配类型，value为描述文本）
     * 根据目标实体类型动态生成
     */
    private Map<String, String> matchTypeLabels;
}