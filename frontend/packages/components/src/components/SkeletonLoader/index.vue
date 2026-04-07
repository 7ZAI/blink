<template>
  <div class="skeleton-container">
    <!-- 表格骨架屏 -->
    <div v-if="type === 'table'" class="skeleton-table">
      <div class="skeleton-header">
        <div v-for="i in columns" :key="i" class="skeleton-cell skeleton-shimmer"></div>
      </div>
      <div v-for="row in rows" :key="row" class="skeleton-row">
        <div v-for="col in columns" :key="col" class="skeleton-cell skeleton-shimmer"></div>
      </div>
    </div>

    <!-- 卡片骨架屏 -->
    <div v-else-if="type === 'card'" class="skeleton-card">
      <div class="skeleton-card-header skeleton-shimmer"></div>
      <div class="skeleton-card-body">
        <div class="skeleton-line skeleton-shimmer" style="width: 80%"></div>
        <div class="skeleton-line skeleton-shimmer" style="width: 60%"></div>
        <div class="skeleton-line skeleton-shimmer" style="width: 40%"></div>
      </div>
    </div>

    <!-- 列表骨架屏 -->
    <div v-else-if="type === 'list'" class="skeleton-list">
      <div v-for="i in rows" :key="i" class="skeleton-list-item">
        <div class="skeleton-avatar skeleton-shimmer"></div>
        <div class="skeleton-content">
          <div class="skeleton-line skeleton-shimmer" style="width: 70%"></div>
          <div class="skeleton-line skeleton-shimmer" style="width: 50%"></div>
        </div>
      </div>
    </div>

    <!-- 统计卡片骨架屏 -->
    <div v-else-if="type === 'stat'" class="skeleton-stat">
      <div class="skeleton-stat-icon skeleton-shimmer"></div>
      <div class="skeleton-stat-content">
        <div class="skeleton-stat-value skeleton-shimmer"></div>
        <div class="skeleton-stat-label skeleton-shimmer"></div>
      </div>
    </div>

    <!-- 默认骨架屏 -->
    <div v-else class="skeleton-default">
      <div class="skeleton-line skeleton-shimmer" style="width: 100%"></div>
      <div class="skeleton-line skeleton-shimmer" style="width: 80%"></div>
      <div class="skeleton-line skeleton-shimmer" style="width: 60%"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 骨架屏组件
 * 用于数据加载时显示占位效果
 */
defineOptions({
  name: 'SkeletonLoader',
})

interface Props {
  type?: 'table' | 'card' | 'list' | 'stat' | 'default'
  rows?: number
  columns?: number
}

withDefaults(defineProps<Props>(), {
  type: 'default',
  rows: 5,
  columns: 4,
})
</script>

<style scoped lang="scss">
.skeleton-container {
  width: 100%;
}

/* 闪烁动画 */
.skeleton-shimmer {
  background: linear-gradient(
    90deg,
    var(--bg-color) 25%,
    var(--border-color-light) 50%,
    var(--bg-color) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* 表格骨架屏 */
.skeleton-table {
  .skeleton-header {
    display: flex;
    gap: 16px;
    padding: 14px 16px;
    background: var(--table-header-bg);
    border-radius: 8px 8px 0 0;
  }

  .skeleton-row {
    display: flex;
    gap: 16px;
    padding: 12px 16px;
    border-bottom: 1px solid var(--border-color-light);

    &:last-child {
      border-bottom: none;
    }
  }

  .skeleton-cell {
    flex: 1;
    height: 16px;
    border-radius: 4px;
  }
}

/* 卡片骨架屏 */
.skeleton-card {
  border-radius: 12px;
  border: 1px solid var(--border-color-light);
  overflow: hidden;

  .skeleton-card-header {
    height: 50px;
    background: var(--table-header-bg);
  }

  .skeleton-card-body {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
}

/* 列表骨架屏 */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .skeleton-list-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    border-radius: 8px;
    background: var(--bg-color);
  }

  .skeleton-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .skeleton-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
}

/* 统计卡片骨架屏 */
.skeleton-stat {
  display: flex;
  align-items: center;
  padding: 20px;

  .skeleton-stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 16px;
    margin-right: 16px;
  }

  .skeleton-stat-content {
    flex: 1;
  }

  .skeleton-stat-value {
    height: 28px;
    width: 60%;
    border-radius: 4px;
    margin-bottom: 8px;
  }

  .skeleton-stat-label {
    height: 14px;
    width: 40%;
    border-radius: 4px;
  }
}

/* 默认骨架屏 */
.skeleton-default {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  height: 16px;
  border-radius: 4px;
}
</style>