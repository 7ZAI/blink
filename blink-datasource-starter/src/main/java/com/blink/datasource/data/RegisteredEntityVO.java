package com.blink.datasource.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 已注册实体信息VO
 *
 * @author binblink
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredEntityVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实体类全限定名
     */
    private String entityClass;

    /**
     * 实体类中文名称
     */
    private String entityName;

    /**
     * 实体类英文名称
     */
    private String entityEnName;

    /**
     * 对应表名
     */
    private String tableName;

    /**
     * 该实体可用的关联关系列表
     * 用于关联过滤类型配置
     */
    private List<RelationInfoVO> relations;

    /**
     * 构造函数（不含关联关系）
     */
    public RegisteredEntityVO(String entityClass, String entityName, String entityEnName, String tableName) {
        this.entityClass = entityClass;
        this.entityName = entityName;
        this.entityEnName = entityEnName;
        this.tableName = tableName;
    }
}