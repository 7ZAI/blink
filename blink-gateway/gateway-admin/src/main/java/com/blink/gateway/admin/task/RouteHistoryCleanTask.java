package com.blink.gateway.admin.task;

import com.blink.gateway.admin.mapper.GaRouteHistoryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 路由历史清理定时任务
 * 自动清理过期的历史记录
 *
 * @author binblink
 * @since 2026-04-12
 */
@Component
@Slf4j
public class RouteHistoryCleanTask {

    @Resource
    private GaRouteHistoryMapper gaRouteHistoryMapper;

    /**
     * 保留天数
     */
    private static final Integer KEEP_DAYS = 90;

    /**
     * 每天凌晨2点执行清理
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanHistory() {
        log.info("[RouteHistoryClean] 开始执行历史记录清理任务 | keepDays: {}", KEEP_DAYS);

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(KEEP_DAYS);

            // 删除超过保留天数的历史记录
            int deletedCount = gaRouteHistoryMapper.deleteByOperateTimeBefore(threshold);

            log.info("[RouteHistoryClean] 清理历史记录完成 | threshold: {}, deleted: {}", threshold, deletedCount);
        } catch (Exception e) {
            log.error("[RouteHistoryClean] 清理历史记录失败 | error: {}", e.getMessage(), e);
        }
    }
}