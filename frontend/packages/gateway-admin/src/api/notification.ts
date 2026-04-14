import request from '@/utils/request'

// ==================== Types ====================

export interface NotificationItem {
  notificationId: number
  title: string
  content: string
  type: string
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS'
  sourceRef?: string
  createdTime: string
  read?: boolean
  targetType?: string
  targetUserId?: number
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

export const getNotificationList = (): Promise<NotificationListRsp> => {
  return request.post('/notification/list', { body: {} })
}

export const getUnreadCount = (): Promise<UnreadCountRsp> => {
  return request.post('/notification/unreadCount', { body: {} })
}

export const markRead = (notificationId: number): Promise<void> => {
  return request.post('/notification/markRead', { body: { notificationId } })
}

export const markAllRead = (): Promise<void> => {
  return request.post('/notification/markAllRead', { body: {} })
}

export const getNotificationHistory = (
  params: QueryHistoryParams
): Promise<{ rows: NotificationItem[]; total: number }> => {
  return request.post('/notification/history', { body: params })
}

export const notificationApi = {
  getList: getNotificationList,
  getUnreadCount,
  markRead,
  markAllRead,
  getHistory: getNotificationHistory,
}
