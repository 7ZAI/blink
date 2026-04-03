package com.blink.gateway.base.dto.rsp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 批量获取字典数据响应对象
 *
 * @author binblink
 * @since 2026-03-21
 */
@Data
public class DictDataMapRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典数据Map，key为dictType，value为该类型下的字典数据列表
     */
    private Map<String, List<DictDataItem>> dictDataMap;

    /**
     * 字典数据项
     */
    @Data
    public static class DictDataItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 字典键值（实际值）
         */
        private String dictValue;

        /**
         * 字典标签（显示值）
         */
        private String dictLabel;

        /**
         * 表格回显样式
         */
        private String listClass;

        /**
         * 是否默认
         */
        private Boolean isDefault;
    }
}