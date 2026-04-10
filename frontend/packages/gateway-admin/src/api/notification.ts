import request from '@/utils/request'

// ==================== Types ====================

export interface NotificationItem {
  notificationId: number
  title: string
  content: string
  type: string // 通知类型：instance_sync_success/failed, cache_sync_summary_success/partial 等
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS'
  sourceRef?: string // 关联的消息ID，用于去重
  createdTime: string
  read?: boolean
  targetType?: string // 目标类型：user/channel
  targetUserId?: number // 目标用户ID
}

export interface NotificationListRsp {
  notifications: NotificationItem[]
  unreadCount: number
}

export interface UnreadCountRsp {
  unreadCount: number
}

export interface QueryHistoryParams {
  pageNum?: number
  pageSize?: number
  type?: string
  severity?: string
  startTime?: string
  endTime?: string
}

// ==================== API Functions ====================

/**
 * Get notification list
 */
export const getNotificationList = (): Promise<NotificationListRsp> => {
  return request.post('/notification/list', { body: {} })
}

/**
 * Get unread count
 */
export const getUnreadCount = (): Promise<UnreadCountRsp> => {
  return request.post('/notification/unreadCount', { body: {} })
}

/**
 * Mark notification as read
 */
export const markRead = (notificationId: number): Promise<void> => {
  return request.post('/notification/markRead', {
    body: { notificationId },
  })
}

/**
 * Mark all notifications as read
 */
export const markAllRead = (): Promise<void> => {
  return request.post('/notification/markAllRead', { body: {} })
}

/**
 * Get notification history
 */
export const getNotificationHistory = (
  params: QueryHistoryParams
): Promise<{ rows: NotificationItem[]; total: number }> => {
  return request.post('/notification/history', { body: params })
}

// API object
export const notificationApi = {
  getList: getNotificationList,
  getUnreadCount,
  markRead,
  markAllRead,
  getHistory: getNotificationHistory,
}
