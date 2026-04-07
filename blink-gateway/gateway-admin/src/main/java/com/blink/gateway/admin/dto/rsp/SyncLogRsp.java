package com.blink.gateway.admin.dto.rsp;

import com.blink.framework.common.data.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步日志响应
 *
 * @author binblink
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SyncLogRsp extends PageDTO<SyncLogRsp.SyncLogItem> {

    /**
     * 同步日志项
     */
    @Data
    public static class SyncLogItem {
        /**
         * ID
         */
        private Long id;

        /**
         * 同步类型
         */
        private String syncType;

        /**
         * 同步模式: 0-全量, 1-增量
         */
        private Byte syncMode;

        /**
         * 同步的 key 列表
         */
        private List<String> syncKeys;

        /**
         * 操作人
         */
        private String operator;

        /**
         * 状态: 0-成功, 1-部分失败, 2-失败
         */
        private Byte status;

        /**
         * 实例数量
         */
        private Integer instanceCount;

        /**
         * 成功数量
         */
        private Integer successCount;

        /**
         * 创建时间
         */
        private LocalDateTime createTime;
    }
}