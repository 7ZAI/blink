package com.blink.framework.common.data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author binblink
 * @Date 2025/10/16
 */
public class SysConfigCacheDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 963285923626533164L;
    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 参数键名
     */
    private String configKey;

    /**
     * 参数名称
     */
    private String configName;

    /**
     * 参数值
     */
    private String configValue;

    /**
     * 参数类型：0-字符串 1-数字 2-布尔 3-JSON 4-数组
     */
    private Byte configType;

    /**
     * 参数描述
     */
    private String description;
    /**
     * 备注
     */
    private String remark;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public Byte getConfigType() {
        return configType;
    }

    public void setConfigType(Byte configType) {
        this.configType = configType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "SysConfigRedisDO{" +
                "id=" + id +
                ", configKey='" + configKey + '\'' +
                ", configName='" + configName + '\'' +
                ", configValue='" + configValue + '\'' +
                ", configType=" + configType +
                ", description='" + description + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}
