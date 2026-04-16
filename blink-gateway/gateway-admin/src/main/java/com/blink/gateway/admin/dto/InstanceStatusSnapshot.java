package com.blink.gateway.admin.dto;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 实例状态快照
 * 用于状态变化检测
 *
 * @author binblink
 * @since 2026-04-14
 */
@Data
public class InstanceStatusSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 在线状态（0-在线 1-离线 2-下线）
     */
    private Integer status;

    /**
     * 健康状态（UP/DOWN）
     */
    private String healthStatus;

    /**
     * CPU使用率（整数，便于比较）
     */
    private Integer cpuUsageInt;

    /**
     * 堆内存使用率（整数，便于比较）
     */
    private Integer heapUsageInt;

    /**
     * 采集时间戳
     */
    private Long timestamp;

    /**
     * 生成快照Key（用于Redis存储）
     */
    public static String snapshotKey(String instanceId) {
        return "blink:gateway:instance:snapshot:" + instanceId;
    }

    /**
     * 判断状态是否有显著变化
     *
     * @param previous 上一次快照
     * @param cpuThreshold CPU变化阈值（百分比）
     * @param heapThreshold 堆内存变化阈值（百分比）
     * @return 变化类型，null表示无显著变化
     */
    public StatusChangeType detectChange(InstanceStatusSnapshot previous, int cpuThreshold, int heapThreshold) {
        if (previous == null) {
            return StatusChangeType.NEW_INSTANCE;
        }

        // 在线状态变化（最重要）
        if (!ObjectUtil.equal(this.status, previous.status)) {
            return StatusChangeType.STATUS_CHANGED;
        }

        // 健康状态变化
        if (!ObjectUtil.equal(this.healthStatus, previous.healthStatus)) {
            return StatusChangeType.HEALTH_CHANGED;
        }

        // CPU 使用率显著变化
        if (this.cpuUsageInt != null && previous.cpuUsageInt != null) {
            int cpuDiff = Math.abs(this.cpuUsageInt - previous.cpuUsageInt);
            if (cpuDiff >= cpuThreshold) {
                return StatusChangeType.METRICS_CHANGED;
            }
        }

        // 堆内存使用率显著变化
        if (this.heapUsageInt != null && previous.heapUsageInt != null) {
            int heapDiff = Math.abs(this.heapUsageInt - previous.heapUsageInt);
            if (heapDiff >= heapThreshold) {
                return StatusChangeType.METRICS_CHANGED;
            }
        }

        return null;
    }

    /**
     * 状态变化类型
     */
    public enum StatusChangeType {
        /**
         * 新实例上线
         */
        NEW_INSTANCE,

        /**
         * 在线状态变化（上线/离线/下线）
         */
        STATUS_CHANGED,

        /**
         * 健康状态变化（UP/DOWN）
         */
        HEALTH_CHANGED,

        /**
         * 指标显著变化（CPU/内存）
         */
        METRICS_CHANGED
    }
}
