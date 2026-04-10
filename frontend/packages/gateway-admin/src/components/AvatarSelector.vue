<template>
  <div class="avatar-selector">
    <div class="current-avatar" @click="dialogVisible = true">
      <el-avatar :size="size" :src="currentAvatarUrl">
        <el-icon><User /></el-icon>
      </el-avatar>
      <div class="avatar-overlay">
        <el-icon><Edit /></el-icon>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="t('avatar.selectStyle')"
      width="700px"
      :close-on-click-modal="false"
    >
      <div class="avatar-preview">
        <el-avatar :size="80" :src="previewAvatarUrl">
          <el-icon><User /></el-icon>
        </el-avatar>
      </div>

      <el-divider />

      <div class="avatar-grid-container">
        <div class="avatar-grid">
          <div
            v-for="style in currentPageStyles"
            :key="style.value"
            class="avatar-item"
            :class="{ active: selectedAvatar === style.value }"
            @click="handleAvatarSelect(style.value)"
          >
            <el-avatar :size="40" :src="getAvatarUrl(style.value)" />
            <span class="avatar-name">{{ style.label }}</span>
          </div>
        </div>

        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="avatarStyles.length"
            layout="prev, pager, next"
            :pager-count="5"
            small
          />
          <span class="page-info">
            {{ t('pagination.total') }} {{ avatarStyles.length }} {{ t('avatar.styles') }}
          </span>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" @click="handleConfirm">{{ t('common.confirm') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 本地头像选择器组件
 * 使用 assets/avatar 目录下的 SVG 文件提供头像选择
 */
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { User, Edit } from '@element-plus/icons-vue'
import { AVATAR_STYLES, getAvatarUrl } from '@/utils/avatar'

interface Props {
  modelValue?: string
  size?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  size: 40,
})

const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()

const dialogVisible = ref(false)
const selectedAvatar = ref(props.modelValue || 'fun-emoji')
const currentPage = ref(1)
const pageSize = 24

const avatarStyles = AVATAR_STYLES

const currentPageStyles = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return avatarStyles.slice(start, end)
})

const currentAvatarUrl = computed(() => {
  return getAvatarUrl(props.modelValue)
})

const previewAvatarUrl = computed(() => {
  return getAvatarUrl(selectedAvatar.value)
})

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      selectedAvatar.value = val
    }
  },
  { immediate: true }
)

const handleAvatarSelect = (avatarName: string) => {
  selectedAvatar.value = avatarName
}

const handleConfirm = () => {
  emit('update:modelValue', selectedAvatar.value)
  dialogVisible.value = false
}
</script>

<style scoped lang="scss">
.avatar-selector {
  display: inline-block;
}

.current-avatar {
  position: relative;
  cursor: pointer;

  .avatar-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.2s;
    color: #fff;

    .el-icon {
      font-size: 16px;
    }
  }

  &:hover .avatar-overlay {
    opacity: 1;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.avatar-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.avatar-grid-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
  padding: 8px;
}

.avatar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;

  &:hover {
    background-color: var(--table-row-hover);
  }

  &.active {
    border-color: var(--primary-color);
    background-color: var(--sidebar-active-bg);
  }

  .avatar-name {
    font-size: 10px;
    color: var(--text-color-secondary);
    text-align: center;
    line-height: 1.2;
    max-width: 60px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.pagination-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding-top: 8px;
  border-top: 1px solid var(--border-color-light);

  .page-info {
    font-size: 12px;
    color: var(--text-color-secondary);
  }
}
</style>
