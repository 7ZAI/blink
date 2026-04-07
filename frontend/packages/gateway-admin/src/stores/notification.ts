import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useSseConnection } from '@/composables/useSseConnection'
import { notificationApi } from '@/api/notification'
import type { NotificationItem } from '@/api/notification'

export interface NotificationStoreItem extends NotificationItem {
  read: boolean
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationStoreItem[]>([])
  const unreadCount = ref(0)
  const sseStatus = ref<'connecting' | 'connected' | 'disconnected'>('disconnected')

  let sseConnection: ReturnType<typeof useSseConnection> | null = null

  const hasUnread = computed(() => unreadCount.value > 0)

  const connectSse = () => {
    if (sseConnection) {
      sseConnection.disconnect()
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
    const sseUrl = `${baseUrl}/notification/sse/connect`

    sseConnection = useSseConnection({
      url: sseUrl,
      token,
      onMessage: handleSseMessage,
      onConnect: () => {
        sseStatus.value = 'connected'
        fetchOfflineMessages()
      },
      onDisconnect: () => {
        sseStatus.value = 'disconnected'
      },
      onError: (error) => {
        sseStatus.value = 'disconnected'
        console.error('SSE error:', error)
      },
      maxRetries: 10,
      retryDelay: 1000
    })

    sseStatus.value = 'connecting'
    sseConnection.connect()
  }

  const disconnectSse = () => {
    if (sseConnection) {
      sseConnection.disconnect()
      sseConnection = null
    }
    sseStatus.value = 'disconnected'
  }

  const handleSseMessage = (msg: NotificationItem) => {
    // 检查是否已存在（按notificationId去重）
    const existing = notifications.value.find(n => n.notificationId === msg.notificationId)
    if (!existing) {
      notifications.value.unshift({
        ...msg,
        read: false
      })
      unreadCount.value++
    }

    // severity为WARNING或ERROR时弹出Toast
    if (msg.severity === 'WARNING' || msg.severity === 'ERROR') {
      ElMessage({
        type: msg.severity === 'ERROR' ? 'error' : 'warning',
        message: msg.title,
        duration: 3000,
        showClose: true
      })
    }
  }

  const fetchOfflineMessages = async () => {
    try {
      const rsp = await notificationApi.getList()
      if (rsp.notifications) {
        // 只添加不在列表中的消息
        for (const msg of rsp.notifications) {
          const existing = notifications.value.find(n => n.notificationId === msg.notificationId)
          if (!existing) {
            notifications.value.push({
              ...msg,
              read: msg.read ?? false
            })
          }
        }
      }
      unreadCount.value = rsp.unreadCount || 0
    } catch (error) {
      console.error('Failed to fetch offline messages:', error)
    }
  }

  const fetchUnreadCount = async () => {
    try {
      const rsp = await notificationApi.getUnreadCount()
      unreadCount.value = rsp.unreadCount || 0
    } catch (error) {
      console.error('Failed to fetch unread count:', error)
    }
  }

  const markAsRead = async (notificationId: number) => {
    try {
      await notificationApi.markRead(notificationId)
      const notification = notifications.value.find(n => n.notificationId === notificationId)
      if (notification && !notification.read) {
        notification.read = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch (error) {
      console.error('Failed to mark as read:', error)
    }
  }

  const markAllAsRead = async () => {
    try {
      await notificationApi.markAllRead()
      notifications.value.forEach(n => {
        n.read = true
      })
      unreadCount.value = 0
    } catch (error) {
      console.error('Failed to mark all as read:', error)
    }
  }

  const clearNotifications = () => {
    notifications.value = []
    unreadCount.value = 0
  }

  return {
    notifications,
    unreadCount,
    hasUnread,
    sseStatus,
    connectSse,
    disconnectSse,
    fetchOfflineMessages,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    clearNotifications
  }
})