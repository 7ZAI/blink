package com.blink.gateway.admin.constants;

/**
 * 定时任务常量
 *
 * @author binblink
 * @since 2026-04-12
 */
public interface ScheduleConstant {

    /**
     * 实例状态同步定时任务 Cron 表达式（每30分钟执行）
     */
    String INSTANCE_SYNC_CRON = "0 */30 * * * ?";

    /**
     * 历史数据清理定时任务 Cron 表达式（每天凌晨2点执行）
     */
    String METRICS_HISTORY_CLEAN_CRON = "0 0 2 * * ?";
}