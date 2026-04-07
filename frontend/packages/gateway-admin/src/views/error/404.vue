<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-icon">
        <el-icon :size="120"><DocumentDelete /></el-icon>
      </div>
      <h1 class="error-code">404</h1>
      <h2 class="error-title">{{ t('error.404Message') }}</h2>
      <p class="error-desc">{{ t('error.404Desc') }}</p>
      <div class="error-actions">
        <el-button type="primary" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          {{ t('common.goBack') }}
        </el-button>
        <el-button @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          {{ t('error.backToDashboard') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'NotFound' })

const router = useRouter()
const { t } = useI18n()

const goBack = () => {
  router.back()
}

const goHome = () => {
  router.push('/dashboard')
}
</script>

<style scoped lang="scss">
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - var(--nav-height));
  padding: 40px 20px;
}

.error-content {
  text-align: center;
  max-width: 500px;
}

.error-icon {
  color: var(--text-color-placeholder);
  margin-bottom: 24px;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: var(--text-color-primary);
  line-height: 1;
  margin-bottom: 16px;
}

.error-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-color-primary);
  margin-bottom: 12px;
}

.error-desc {
  font-size: 14px;
  color: var(--text-color-secondary);
  margin-bottom: 32px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>