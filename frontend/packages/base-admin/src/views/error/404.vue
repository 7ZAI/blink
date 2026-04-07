<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-code">404</div>
      <div class="error-title">页面未找到</div>
      <div class="error-desc">抱歉，您访问的页面不存在或已被移除</div>
      <el-button type="primary" @click="$router.push('/')">
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { HomeFilled } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--bg-color-page);
  transition: background-color 0.3s;
}

.error-content {
  text-align: center;
  padding: 40px;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: var(--primary-color);
  line-height: 1;
  margin-bottom: 20px;
  text-shadow: 2px 2px 4px rgba(59, 130, 246, 0.2);
}

.error-title {
  font-size: 24px;
  color: var(--text-color-primary);
  margin-bottom: 12px;
}

.error-desc {
  font-size: 14px;
  color: var(--text-color-secondary);
  margin-bottom: 30px;
}

.el-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
