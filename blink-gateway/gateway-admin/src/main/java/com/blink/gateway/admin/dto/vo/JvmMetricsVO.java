package com.blink.gateway.admin.dto.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * JVM 监控指标 VO
 *
 * @author binblink
 */
@Data
public class JvmMetricsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 堆内存使用量 (bytes)
     */
    private Long heapUsed;

    /**
     * 堆内存最大值 (bytes)
     */
    private Long heapMax;

    /**
     * 堆内存使用率 (%)
     */
    private Double heapUsagePercent;

    /**
     * 非堆内存使用量 (bytes)
     */
    private Long nonHeapUsed;

    /**
     * 年轻代 GC 次数
     */
    private Long youngGcCount;

    /**
     * 年轻代 GC 时间 (ms)
     */
    private Long youngGcTime;

    /**
     * 老年代 GC 次数
     */
    private Long oldGcCount;

    /**
     * 老年代 GC 时间 (ms)
     */
    private Long oldGcTime;

    /**
     * 活跃线程数
     */
    private Integer liveThreads;

    /**
     * 峰值线程数
     */
    private Integer peakThreads;

    /**
     * 守护线程数
     */
    private Integer daemonThreads;

    /**
     * 采样时间戳
     */
    private Long timestamp;
}