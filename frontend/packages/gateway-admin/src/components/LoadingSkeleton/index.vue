<template>
  <div class="loading-skeleton" :class="[`skeleton-${type}`]">
    <!-- Card skeleton -->
    <template v-if="type === 'card'">
      <div v-for="i in rows" :key="i" class="skeleton-card">
        <div class="skeleton-line skeleton-title"></div>
        <div class="skeleton-line skeleton-text"></div>
        <div class="skeleton-line skeleton-text short"></div>
      </div>
    </template>

    <!-- Table skeleton -->
    <template v-else-if="type === 'table'">
      <div class="skeleton-table-header">
        <div v-for="i in 5" :key="i" class="skeleton-cell"></div>
      </div>
      <div v-for="i in rows" :key="i" class="skeleton-table-row">
        <div v-for="j in 5" :key="j" class="skeleton-cell"></div>
      </div>
    </template>

    <!-- List skeleton -->
    <template v-else-if="type === 'list'">
      <div v-for="i in rows" :key="i" class="skeleton-list-item">
        <div class="skeleton-avatar"></div>
        <div class="skeleton-content">
          <div class="skeleton-line skeleton-title"></div>
          <div class="skeleton-line skeleton-text short"></div>
        </div>
      </div>
    </template>

    <!-- Text skeleton (default) -->
    <template v-else>
      <div v-for="i in rows" :key="i" class="skeleton-line" :class="{ short: i === rows }"></div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'LoadingSkeleton' })

interface Props {
  type?: 'card' | 'table' | 'list' | 'text'
  rows?: number
}

withDefaults(defineProps<Props>(), {
  type: 'text',
  rows: 3
})
</script>

<style scoped lang="scss">
.loading-skeleton {
  width: 100%;
}

// Shimmer animation
.skeleton-line,
.skeleton-cell,
.skeleton-avatar {
  background: linear-gradient(
    90deg,
    var(--bg-color-page) 25%,
    var(--border-color-light) 50%,
    var(--bg-color-page) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

// Text skeleton
.skeleton-line {
  height: 16px;
  margin-bottom: 12px;

  &.short {
    width: 60%;
  }
}

.skeleton-title {
  height: 20px;
  width: 40%;
  margin-bottom: 16px;
}

.skeleton-text {
  height: 14px;
}

// Card skeleton
.skeleton-card {
  padding: 20px;
  background-color: var(--card-bg);
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
}

// Table skeleton
.skeleton-table-header {
  display: flex;
  gap: 16px;
  padding: 12px 16px;
  background-color: var(--bg-color-page);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.skeleton-table-row {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);

  &:last-child {
    border-bottom: none;
  }
}

.skeleton-cell {
  flex: 1;
  height: 16px;
}

// List skeleton
.skeleton-list-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);

  &:last-child {
    border-bottom: none;
  }
}

.skeleton-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
}
</style>