package com.blink.framework.common.data;

import java.io.Serializable;

/**
 * SysFieldConstraintDO 的镜像实体类，用于缓存
 * 保证包的隔离性
 *
 * @author binblink
 * @since 2026-03-07
 */
public class FieldConstraintCacheDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 约束名称（字段名称）
     */
    private String constraintName;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 最大长度
     */
    private Integer maxLength;

    /**
     * 数据正则校验模式
     */
    private String dataPattern;

    /**
     * 数据精度
     */
    private Integer dataPrecision;

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public String getDataPattern() {
        return dataPattern;
    }

    public void setDataPattern(String dataPattern) {
        this.dataPattern = dataPattern;
    }

    public Integer getDataPrecision() {
        return dataPrecision;
    }

    public void setDataPrecision(Integer dataPrecision) {
        this.dataPrecision = dataPrecision;
    }

    @Override
    public String toString() {
        return "FieldConstraintCacheDO{" +
                "constraintName='" + constraintName + '\'' +
                ", dataType='" + dataType + '\'' +
                ", maxLength=" + maxLength +
                ", dataPattern='" + dataPattern + '\'' +
                ", dataPrecision=" + dataPrecision +
                '}';
    }
}