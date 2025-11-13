package com.blink.framework.core.entity;

import java.io.Serializable;

/**
 * 数据字典 用来约束入参的最大边界
 */
public class SysDataDictDO implements Serializable  {

    private static final long serialVersionUID = 1L;

    private String dictName;

    private String dataType;

    private Integer maxLength;

    private String dataPattern;

    private Integer dataPrecision;

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
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
        return "SysDataDictDO{" +
                "dictName='" + dictName + '\'' +
                ", dataType='" + dataType + '\'' +
                ", maxLength=" + maxLength +
                ", dataPattern='" + dataPattern + '\'' +
                ", dataPrecision=" + dataPrecision +
                '}';
    }
}
