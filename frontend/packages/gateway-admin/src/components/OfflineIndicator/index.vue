<template>
  <Transition name="slide">
    <div v-if="isOffline" class="offline-banner">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ t('common.offline') }}</span>
      <span class="reconnecting">{{ t('common.reconnecting') }}</span>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { useOffline } from '@/composables/useOffline'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'OfflineIndicator' })

const { isOffline } = useOffline()
const { t } = useI18n()
</script>

<style scoped lang="scss">
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateY(-100%);
}

.reconnecting {
  margin-left: 8px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
