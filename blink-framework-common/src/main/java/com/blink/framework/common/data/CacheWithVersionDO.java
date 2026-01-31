package com.blink.framework.common.data;

/**
 * 带版本的缓存DO类 用来缓存同步时带版本号更新
 * @Author binblink
 */
public class CacheWithVersionDO {

    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
