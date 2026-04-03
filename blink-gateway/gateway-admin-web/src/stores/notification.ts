import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface NotificationItem {
  id: string
  title: string
  message: string
  type: 'info' | 'warning' | 'error' | 'success'
  time: Date
  read: boolean
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationItem[]>([])

  const addNotification = (notification: Omit<NotificationItem, 'id' | 'time' | 'read'>) => {
    notifications.value.unshift({
      ...notification,
      id: Date.now().toString(),
      time: new Date(),
      read: false
    })
  }

  const markAsRead = (id: string) => {
    const item = notifications.value.find(n => n.id === id)
    if (item) item.read = true
  }

  const markAllAsRead = () => {
    notifications.value.forEach(n => n.read = true)
  }

  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  return {
    notifications,
    addNotification,
    markAsRead,
    markAllAsRead,
    unreadCount
  }
})