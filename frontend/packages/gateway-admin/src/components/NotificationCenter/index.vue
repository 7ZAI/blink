<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="header-item notification-trigger">
      <el-icon><Bell /></el-icon>
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </div>
    <template #dropdown>
      <el-dropdown-menu class="notification-dropdown">
        <div class="notification-header">
          <span class="title">{{ t('common.notifications') }}</span>
          <el-button v-if="notifications.length > 0" type="primary" link size="small" @click="markAllAsRead">
            {{ t('common.markAllRead') }}
          </el-button>
        </div>
        <el-scrollbar max-height="300px">
          <div v-if="notifications.length === 0" class="no-notifications">
            {{ t('common.noNotifications') }}
          </div>
          <div
            v-for="item in notifications"
            :key="item.notificationId"
            class="notification-item"
            :class="{ unread: !item.read }"
            @click="markAsRead(item.notificationId)"
          >
            <el-icon :class="item.severity.toLowerCase()" class="notification-icon">
              <component :is="getIcon(item.severity)" />
            </el-icon>
            <div class="notification-content">
              <div class="notification-title">{{ item.title }}</div>
              <div class="notification-message">{{ item.content }}</div>
              <div class="notification-time">{{ formatTime(item.createdTime) }}</div>
            </div>
          </div>
        </el-scrollbar>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Bell, InfoFilled, WarningFilled, CircleCloseFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { useNotificationStore, type NotificationStoreItem } from '@/stores/notification'

defineOptions({ name: 'NotificationCenter' })

const { t } = useI18n()
const notificationStore = useNotificationStore()

const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)

const markAsRead = (notificationId: number) => {
  notificationStore.markAsRead(notificationId)
}

const markAllAsRead = () => {
  notificationStore.markAllAsRead()
}

const handleCommand = (_command: string) => {
  // Handle notification click actions
}

const getIcon = (severity: NotificationStoreItem['severity']) => {
  const icons: Record<string, any> = {
    info: InfoFilled,
    warning: WarningFilled,
    error: CircleCloseFilled,
    success: CircleCheckFilled
  }
  return icons[severity.toLowerCase()] || InfoFilled
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
/* 触发按钮样式 - 与 Header 其他按钮一致 */
.notification-trigger {
  position: relative;

  .el-icon {
    @apply text-lg shrink-0;
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
  background-color: var(--el-color-danger);
  color: white;
  border-radius: 8px;
}

/* 下拉面板 */
.notification-dropdown {
  width: 320px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  .title {
    font-weight: 500;
    color: var(--el-text-color-primary);
  }
}

.no-notifications {
  padding: 40px 16px;
  text-align: center;
  color: var(--el-text-color-secondary);
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: var(--el-fill-color-light);
  }

  &.unread {
    background-color: rgba(59, 130, 246, 0.05);
  }
}

.notification-icon {
  font-size: 20px;
  flex-shrink: 0;

  &.info { color: var(--el-color-primary); }
  &.warning { color: var(--el-color-warning); }
  &.error { color: var(--el-color-danger); }
  &.success { color: var(--el-color-success); }
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.notification-message {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notification-time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}
</style>