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

  // 标记是否已设置可见性监听器，避免重复添加
  let visibilityListenerSetup = false

  const hasUnread = computed(() => unreadCount.value > 0)

  /**
   * 检查页面是否在后台（用户不在浏览器或切换到其他标签页）
   */
  const isPageHidden = () => document.hidden || document.visibilityState === 'hidden'

  /**
   * 请求浏览器通知权限
   */
  const requestNotificationPermission = async () => {
    if (!('Notification' in window)) {
      console.warn('[Notification] 浏览器不支持 Web Notifications')
      return false
    }

    if (Notification.permission === 'granted') {
      return true
    }

    if (Notification.permission !== 'denied') {
      const permission = await Notification.requestPermission()
      return permission === 'granted'
    }

    return false
  }

  /**
   * 发送浏览器系统通知（当用户不在页面时）
   */
  const sendBrowserNotification = (msg: NotificationItem) => {
    // 只有在页面隐藏时才发送系统通知
    if (!isPageHidden()) {
      return
    }

    // 检查是否有通知权限
    if (Notification.permission !== 'granted') {
      return
    }

    try {
      const notification = new Notification(msg.title, {
        body: msg.content,
        icon: '/favicon.ico',
        tag: msg.sourceRef || `notification-${msg.notificationId}`,
        requireInteraction: msg.severity === 'ERROR' || msg.severity === 'WARNING',
      })

      // 点击通知时聚焦到页面
      notification.onclick = () => {
        window.focus()
        notification.close()
        // 标记为已读
        if (msg.notificationId) {
          markAsRead(msg.notificationId)
        }
      }

      // 5秒后自动关闭（非严重通知）
      if (msg.severity !== 'ERROR' && msg.severity !== 'WARNING') {
        setTimeout(() => notification.close(), 5000)
      }

      console.log('[Notification] 系统通知已发送 | title:', msg.title)
    } catch (e) {
      console.error('[Notification] 发送系统通知失败:', e)
    }
  }

  /**
   * 监听页面可见性变化，请求通知权限
   */
  const setupVisibilityListener = () => {
    // 避免重复添加监听器
    if (visibilityListenerSetup) {
      return
    }
    visibilityListenerSetup = true

    // 当页面变为隐藏时，尝试获取通知权限
    document.addEventListener('visibilitychange', async () => {
      if (document.hidden) {
        await requestNotificationPermission()
      }
    })

    // 页面加载时也尝试获取权限
    requestNotificationPermission()
  }

  const connectSse = () => {
    if (sseConnection) {
      sseConnection.disconnect()
    }

    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
    // gateway-admin context-path is /gateway-admin
    const sseUrl = `${baseUrl}/gateway-admin/notification/sse/connect`

    sseConnection = useSseConnection({
      url: sseUrl,
      token,
      onMessage: handleSseMessage,
      onConnect: () => {
        sseStatus.value = 'connected'
        fetchOfflineMessages()
        // 连接成功时设置可见性监听
        setupVisibilityListener()
      },
      onDisconnect: () => {
        sseStatus.value = 'disconnected'
      },
      onError: (error) => {
        sseStatus.value = 'disconnected'
        console.error('SSE error:', error)
      },
      maxRetries: 10,
      retryDelay: 1000,
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
    // 区分进度通知和汇总通知
    const isProgressNotification = msg.type?.startsWith('instance_sync')
    const isSummaryNotification = msg.type?.startsWith('cache_sync_summary')

    // 进度通知：只显示 Toast 弹窗，不加入通知列表
    if (isProgressNotification) {
      // 如果用户不在页面，也发送系统通知
      sendBrowserNotification(msg)

      ElMessage({
        type: msg.severity === 'ERROR' ? 'error' : 'success',
        message: msg.content,
        duration: 2000,
        showClose: true,
      })
      return
    }

    // 汇总通知和普通通知：加入通知列表
    const sourceRef = msg.sourceRef
    let existing: NotificationStoreItem | undefined

    // 汇总通知按 sourceRef 去重（同一消息只有一个汇总）
    if (isSummaryNotification && sourceRef) {
      existing = notifications.value.find(
        (n) => n.sourceRef === sourceRef && n.type?.startsWith('cache_sync_summary')
      )
    } else if (sourceRef) {
      existing = notifications.value.find((n) => n.sourceRef === sourceRef)
    } else {
      existing = notifications.value.find((n) => n.notificationId === msg.notificationId)
    }

    if (!existing) {
      notifications.value.unshift({
        ...msg,
        read: false,
      })
      unreadCount.value++
    } else {
      // 更新已有通知的内容
      existing.title = msg.title
      existing.content = msg.content
      existing.severity = msg.severity
      existing.type = msg.type
      existing.createdTime = msg.createdTime
      if (existing.read) {
        existing.read = false
        unreadCount.value++
      }
    }

    // 如果用户不在页面，发送系统通知
    sendBrowserNotification(msg)

    // WARNING 或 ERROR 的汇总通知也弹出 Toast
    if (msg.severity === 'WARNING' || msg.severity === 'ERROR') {
      ElMessage({
        type: msg.severity === 'ERROR' ? 'error' : 'warning',
        message: msg.title + ': ' + msg.content,
        duration: 5000,
        showClose: true,
      })
    }
  }

  const fetchOfflineMessages = async () => {
    try {
      const rsp = await notificationApi.getList()
      if (rsp.notifications) {
        // 只添加不在列表中的消息（离线期间错过的消息）
        for (const msg of rsp.notifications) {
          // 检查是否已存在（避免覆盖 SSE 实时推送的通知）
          const existing = notifications.value.find((n) => n.notificationId === msg.notificationId)
          if (!existing) {
            notifications.value.push({
              ...msg,
              read: msg.read ?? false,
            })
          }
          // 注意：如果已存在，不覆盖本地状态（SSE 实时通知优先）
        }
      }
      // 计算本地未读数（SSE 实时通知 + 离线通知）
      const localUnreadCount = notifications.value.filter((n) => !n.read).length
      // 只有当本地未读数为 0 且后端有未读数时，才使用后端的值
      // 这样不会丢失 SSE 实时推送的未读通知
      if (localUnreadCount === 0 && rsp.unreadCount > 0) {
        unreadCount.value = rsp.unreadCount
      } else {
        unreadCount.value = localUnreadCount
      }
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
      const notification = notifications.value.find((n) => n.notificationId === notificationId)
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
      notifications.value.forEach((n) => {
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
    clearNotifications,
    requestNotificationPermission,
  }
})
