<template>
  <el-dialog
    v-model="visible"
    :title="t('common.globalSearch')"
    width="500px"
    :show-close="false"
    class="global-search-dialog"
    @open="handleOpen"
  >
    <el-input
      ref="inputRef"
      v-model="searchQuery"
      :placeholder="t('common.searchPlaceholder')"
      prefix-icon="Search"
      clearable
      size="large"
      @keyup.escape="visible = false"
    />

    <div class="search-results">
      <template v-if="searchResults.length > 0">
        <div
          v-for="result in searchResults"
          :key="`${result.type}-${result.id}`"
          class="search-result-item"
          @click="handleSelect(result)"
        >
          <el-icon class="result-icon">
            <Connection v-if="result.type === 'channel'" />
            <Guide v-if="result.type === 'route'" />
          </el-icon>
          <div class="result-content">
            <div class="result-name">{{ result.name }}</div>
            <div class="result-type">{{ t(`${result.type}.title`) }}</div>
          </div>
        </div>
      </template>
      <div v-else-if="searchQuery" class="no-results">
        {{ t('common.noResults') }}
      </div>
    </div>

    <template #footer>
      <span class="shortcut-hint">
        <kbd>ESC</kbd> {{ t('common.toClose') }}
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useSearch, type SearchResult } from '@/composables/useSearch'

defineOptions({ name: 'GlobalSearch' })

const router = useRouter()
const { t } = useI18n()
const { searchQuery, searchResults, loadSearchData } = useSearch()

const visible = ref(false)
const inputRef = ref()

const handleOpen = () => {
  loadSearchData()
  setTimeout(() => {
    inputRef.value?.focus()
  }, 100)
}

const handleSelect = (result: SearchResult) => {
  visible.value = false
  router.push(result.path)
}

const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    visible.value = !visible.value
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped lang="scss">
.global-search-dialog {
  :deep(.el-dialog__header) {
    padding: 16px 16px 0;
    margin-bottom: 0;
  }

  :deep(.el-dialog__body) {
    padding: 12px 16px;
  }

  :deep(.el-dialog__footer) {
    padding: 0 16px 12px;
  }
}

.search-results {
  margin-top: 12px;
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: var(--bg-color-page);
  }
}

.result-icon {
  font-size: 20px;
  color: var(--text-color-secondary);
}

.result-content {
  flex: 1;
}

.result-name {
  font-weight: 500;
  color: var(--text-color-primary);
}

.result-type {
  font-size: 12px;
  color: var(--text-color-secondary);
}

.no-results {
  padding: 24px;
  text-align: center;
  color: var(--text-color-secondary);
}

.shortcut-hint {
  font-size: 12px;
  color: var(--text-color-secondary);

  kbd {
    display: inline-block;
    padding: 2px 6px;
    font-size: 11px;
    font-family: inherit;
    background-color: var(--bg-color-page);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-sm);
  }
}
</style>