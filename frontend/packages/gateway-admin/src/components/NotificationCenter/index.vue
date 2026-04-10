<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="notification-trigger" :class="{ shaking: isShaking }">
      <el-icon class="trigger-icon"><Bell /></el-icon>
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </div>
    <template #dropdown>
      <el-dropdown-menu class="notification-dropdown">
        <div class="notification-header">
          <span class="title">{{ t('common.notifications') }}</span>
          <el-button
            v-if="notifications.length > 0"
            type="primary"
            link
            size="small"
            @click="markAllAsRead"
          >
            {{ t('common.markAllRead') }}
          </el-button>
        </div>
        <el-scrollbar max-height="300px">
          <div v-if="notifications.length === 0" class="no-notifications">
            <el-icon class="empty-icon"><Bell /></el-icon>
            <span>{{ t('common.noNotifications') }}</span>
          </div>
          <div
            v-for="item in notifications"
            :key="item.notificationId"
            class="notification-item"
            :class="{ unread: !item.read }"
            @click="handleNotificationClick(item)"
          >
            <el-icon :class="getSeverityClass(item.severity)" class="notification-icon">
              <component :is="getIcon(item.severity)" />
            </el-icon>
            <div class="notification-content">
              <div class="notification-title">{{ item.title }}</div>
              <div class="notification-message">{{ item.content }}</div>
              <div class="notification-time">{{ formatTime(item.createdTime) }}</div>
            </div>
          </div>
        </el-scrollbar>
        <div v-if="notifications.length > 0" class="notification-footer">
          <el-button type="primary" link size="small" @click="viewAll">
            {{ t('common.viewAll') }}
          </el-button>
        </div>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  Bell,
  InfoFilled,
  WarningFilled,
  CircleCloseFilled,
  CircleCheckFilled,
} from '@element-plus/icons-vue'
import { useNotificationStore, type NotificationStoreItem } from '@/stores/notification'

defineOptions({ name: 'NotificationCenter' })

const router = useRouter()
const { t } = useI18n()
const notificationStore = useNotificationStore()

const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)

// 铃铛震动动画状态
const isShaking = ref(false)
const prevUnreadCount = ref(0)

// 监听未读数变化，触发震动动画
watch(unreadCount, (newCount, oldCount) => {
  if (newCount > oldCount && newCount > 0) {
    // 新消息到来，触发震动
    isShaking.value = true
    // 1.5秒后停止震动
    setTimeout(() => {
      isShaking.value = false
    }, 1500)
  }
  prevUnreadCount.value = newCount
})

const markAsRead = (notificationId: number) => {
  notificationStore.markAsRead(notificationId)
}

const markAllAsRead = () => {
  notificationStore.markAllAsRead()
}

const handleCommand = (_command: string) => {
  // Handle notification click actions
}

const viewAll = () => {
  // 导航到通知历史页面
  router.push('/notification')
}

/**
 * 处理通知项点击
 * 导航到通知历史页面查看详情，不自动标记已读
 */
const handleNotificationClick = (item: NotificationStoreItem) => {
  // 导航到通知历史页面
  router.push('/notification')
}

const getIcon = (severity: NotificationStoreItem['severity']) => {
  const icons: Record<string, any> = {
    info: InfoFilled,
    warning: WarningFilled,
    error: CircleCloseFilled,
    success: CircleCheckFilled,
  }
  return icons[severity.toLowerCase()] || InfoFilled
}

const getSeverityClass = (severity: NotificationStoreItem['severity']) => {
  return severity.toLowerCase()
}

const formatTime = (time: string) => {
  const now = new Date()
  const diff = now.getTime() - new Date(time).getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)

  if (minutes < 1) return t('common.justNow')
  if (minutes < 60) return t('common.minutesAgo', { n: minutes })
  if (hours < 24) return t('common.hoursAgo', { n: hours })
  return new Date(time).toLocaleDateString()
}
</script>

<style scoped lang="scss">
/* 触发按钮样式 - 与 Header 其他按钮统一 */
.notification-trigger {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-color-regular, #606266);

  &:hover {
    color: var(--primary-color, #3b82f6);
    background: rgba(59, 130, 246, 0.1);
  }

  .trigger-icon {
    font-size: 20px;
  }

  /* 铃铛震动动画 */
  &.shaking {
    animation: bell-shake 0.5s ease-in-out 3;

    .trigger-icon {
      animation: bell-ring 0.3s ease-in-out 5;
    }

    .badge {
      animation: badge-bounce 0.4s ease-in-out 4;
    }
  }
}

/* 铃铛整体震动 */
@keyframes bell-shake {
  0%, 100% {
    transform: rotate(0deg);
  }
  10%, 30%, 50%, 70%, 90% {
    transform: rotate(-10deg);
  }
  20%, 40%, 60%, 80% {
    transform: rotate(10deg);
  }
}

/* 铃铛图标晃动 */
@keyframes bell-ring {
  0%, 100% {
    transform: rotate(0deg) scale(1);
  }
  25% {
    transform: rotate(-15deg) scale(1.1);
  }
  50% {
    transform: rotate(0deg) scale(1);
  }
  75% {
    transform: rotate(15deg) scale(1.1);
  }
}

/* 徽章弹跳 */
@keyframes badge-bounce {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.3);
  }
}

/* 未读数徽章 */
.badge {
  position: absolute;
  top: 2px;
  right: 4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 500;
  line-height: 16px;
  text-align: center;
  background-color: var(--danger-color, #ef4444);
  color: white;
  border-radius: 8px;
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* 下拉面板 */
.notification-dropdown {
  width: 360px;
  padding: 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color-lighter, #f0f0f0);
  background: var(--bg-color-overlay, #ffffff);

  .title {
    font-weight: 600;
    font-size: 14px;
    color: var(--text-color-primary, #303133);
  }
}

.no-notifications {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 16px;
  color: var(--text-color-secondary, #909399);

  .empty-icon {
    font-size: 32px;
    opacity: 0.5;
  }
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-bottom: 1px solid var(--border-color-lighter, #f5f5f5);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background-color: var(--bg-color-page, #f5f7fa);
  }

  &.unread {
    background-color: rgba(59, 130, 246, 0.05);

    &:hover {
      background-color: rgba(59, 130, 246, 0.1);
    }
  }
}

.notification-icon {
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;

  &.info {
    color: var(--primary-color, #3b82f6);
  }
  &.warning {
    color: var(--warning-color, #f59e0b);
  }
  &.error {
    color: var(--danger-color, #ef4444);
  }
  &.success {
    color: var(--success-color, #10b981);
  }
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 500;
  font-size: 13px;
  color: var(--text-color-primary, #303133);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-message {
  font-size: 12px;
  color: var(--text-color-secondary, #909399);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-time {
  font-size: 11px;
  color: var(--text-color-placeholder, #c0c4cc);
  margin-top: 6px;
}

.notification-footer {
  display: flex;
  justify-content: center;
  padding: 12px 16px;
  border-top: 1px solid var(--border-color-lighter, #f0f0f0);
  background: var(--bg-color-overlay, #ffffff);
}

/* 深色模式适配 */
[data-theme='dark'] {
  .notification-trigger:hover {
    background: rgba(59, 130, 246, 0.2);
  }

  .notification-dropdown {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  }

  .notification-header,
  .notification-footer {
    background: #1e293b;
    border-color: #334155;
  }

  .notification-item {
    border-color: #334155;

    &:hover {
      background-color: #334155;
    }

    &.unread {
      background-color: rgba(59, 130, 246, 0.1);

      &:hover {
        background-color: rgba(59, 130, 246, 0.15);
      }
    }
  }
}
</style>
