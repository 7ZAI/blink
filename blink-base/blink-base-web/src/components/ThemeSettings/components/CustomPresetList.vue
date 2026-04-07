<!-- src/components/ThemeSettings/components/CustomPresetList.vue -->
<template>
  <div v-if="presets.length > 0" class="custom-preset-list">
    <h4 class="section-title">{{ t('settings.myPresets') }}</h4>
    <div class="preset-grid">
      <div
        v-for="preset in presets"
        :key="preset.id"
        class="preset-card"
        :class="{ active: currentPresetId === preset.id }"
        @click="handleSelect(preset.id)"
      >
        <div class="preset-header">
          <span class="preset-name">{{ locale === 'zh_cn' ? preset.name : preset.nameEn }}</span>
          <el-button
            type="danger"
            size="small"
            circle
            :icon="Delete"
            class="delete-btn"
            @click.stop="handleDelete(preset)"
          />
        </div>
        <div class="color-dots">
          <span
            class="color-dot"
            :style="{ backgroundColor: preset.colors.primary }"
            title="Primary"
          />
          <span
            class="color-dot"
            :style="{ backgroundColor: preset.colors.success }"
            title="Success"
          />
          <span
            class="color-dot"
            :style="{ backgroundColor: preset.colors.warning }"
            title="Warning"
          />
          <span
            class="color-dot"
            :style="{ backgroundColor: preset.colors.danger }"
            title="Danger"
          />
          <span
            class="color-dot"
            :style="{ backgroundColor: preset.colors.info }"
            title="Info"
          />
        </div>
        <div class="preset-date">
          {{ t('settings.created') }}: {{ formatDate(preset.createdAt) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import type { CustomPreset } from '../types'

defineOptions({
  name: 'CustomPresetList',
})

interface Props {
  presets: CustomPreset[]
  currentPresetId?: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', presetId: string): void
  (e: 'delete', presetId: string): void
}>()

const { t, locale } = useI18n()

/**
 * Handle preset selection
 */
const handleSelect = (presetId: string) => {
  emit('select', presetId)
}

/**
 * Handle preset deletion with confirmation
 */
const handleDelete = async (preset: CustomPreset) => {
  try {
    await ElMessageBox.confirm(
      t('settings.deletePresetConfirm', { name: locale.value === 'zh_cn' ? preset.name : preset.nameEn }),
      t('settings.deletePreset'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      },
    )
    emit('delete', preset.id)
  } catch {
    // User cancelled, do nothing
  }
}

/**
 * Format timestamp to readable date
 */
const formatDate = (timestamp: number): string => {
  const date = new Date(timestamp)
  return date.toLocaleDateString(locale.value === 'zh_cn' ? 'zh-CN' : 'en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
</script>

<style scoped lang="scss">
.custom-preset-list {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 16px;
  }

  .preset-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 12px;

    .preset-card {
      display: flex;
      flex-direction: column;
      padding: 12px;
      border: 2px solid var(--border-color-light);
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        border-color: var(--primary-color);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

        .delete-btn {
          opacity: 1;
        }
      }

      &.active {
        border-color: var(--primary-color);
        background: var(--primary-color-light-9);
        box-shadow: 0 2px 8px var(--primary-color-light-7);
      }

      .preset-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        .preset-name {
          font-size: 14px;
          font-weight: 500;
          color: var(--text-color-primary);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 100px;
        }

        .delete-btn {
          opacity: 0;
          transition: opacity 0.2s ease;
          flex-shrink: 0;

          &:hover {
            opacity: 1;
          }
        }
      }

      .color-dots {
        display: flex;
        gap: 6px;
        justify-content: center;
        margin-bottom: 10px;

        .color-dot {
          width: 20px;
          height: 20px;
          border-radius: 50%;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
          transition: transform 0.2s ease;

          &:hover {
            transform: scale(1.2);
          }
        }
      }

      .preset-date {
        font-size: 12px;
        color: var(--text-color-secondary);
        text-align: center;
      }
    }
  }
}

// Dark mode adjustments
:root.dark .custom-preset-list {
  .preset-card {
    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }

    &.active {
      box-shadow: 0 2px 8px rgba(var(--primary-color-rgb), 0.3);
    }

    .color-dots .color-dot {
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
    }
  }
}
</style>