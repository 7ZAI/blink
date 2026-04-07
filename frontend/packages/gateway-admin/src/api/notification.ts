import request from '@/utils/request'

// ==================== Types ====================

export interface NotificationItem {
  notificationId: number
  title: string
  content: string
  type: 'SYSTEM' | 'OPERATION' | 'ALERT'
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS'
  sourceRef?: string
  createdTime: string
  read?: boolean
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
    body: { notificationId }
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
export const getNotificationHistory = (params: QueryHistoryParams): Promise<{ rows: NotificationItem[], total: number }> => {
  return request.post('/notification/history', { body: params })
}

// API object
export const notificationApi = {
  getList: getNotificationList,
  getUnreadCount,
  markRead,
  markAllRead,
  getHistory: getNotificationHistory
}