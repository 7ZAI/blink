-- ============================================================
-- Blink SSE Notification System - Database Migration
-- Version: V2026.04.06
-- Description: Create notification tables for SSE message system
-- ============================================================

-- 消息通知表
CREATE TABLE IF NOT EXISTS sys_notification (
    notification_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    title                VARCHAR(100) NOT NULL COMMENT '消息标题',
    content              VARCHAR(500) NOT NULL COMMENT '消息内容',
    type                 VARCHAR(20) NOT NULL COMMENT '消息类型: SYSTEM/OPERATION/ALERT',
    severity             VARCHAR(20) NOT NULL DEFAULT 'INFO' COMMENT '严重级别: INFO/WARNING/ERROR/SUCCESS',
    target_type          VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '目标类型: ALL/USER',
    target_user_id       INT NULL COMMENT '目标用户ID，定向推送时使用',
    source_ref           VARCHAR(100) NULL COMMENT '来源关联ID，如同步任务ID、配置ID',
    created_by           INT NULL COMMENT '创建人',
    created_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_time          DATETIME NULL COMMENT '过期时间，过期后不再展示',
    INDEX idx_target_user (target_user_id, created_time),
    INDEX idx_created_time (created_time),
    INDEX idx_type_severity (type, severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '系统消息通知表';

-- 用户消息读取状态表
CREATE TABLE IF NOT EXISTS sys_notification_read (
    read_id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id      BIGINT NOT NULL COMMENT '消息ID',
    user_id              INT NOT NULL COMMENT '用户ID',
    read_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '读取时间',
    UNIQUE KEY uk_notification_user (notification_id, user_id),
    INDEX idx_user_read (user_id, read_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '消息读取状态表';