<!-- src/components/ThemeSettings/components/CustomPresetList.vue -->
<template>
  <div class="custom-preset-list">
    <div class="preset-grid">
      <div
        v-for="preset in presets"
        :key="preset.id"
        class="preset-card"
        :class="{ active: currentPresetId === preset.id }"
        @click="handleSelect(preset.id)"
      >
        <!-- 颜色条 -->
        <div class="color-bar">
          <span
            class="color-segment"
            :style="{ backgroundColor: preset.colors.primary, width: '40%' }"
          />
          <span
            class="color-segment"
            :style="{ backgroundColor: preset.colors.success, width: '20%' }"
          />
          <span
            class="color-segment"
            :style="{ backgroundColor: preset.colors.warning, width: '20%' }"
          />
          <span
            class="color-segment"
            :style="{ backgroundColor: preset.colors.danger, width: '20%' }"
          />
        </div>

        <!-- 预设信息 -->
        <div class="preset-content">
          <div class="preset-header">
            <span class="preset-name">
              {{ locale === 'zh_cn' ? preset.name : preset.nameEn }}
            </span>
            <el-tooltip :content="t('settings.deletePreset')" placement="top">
              <el-button
                type="danger"
                size="small"
                circle
                :icon="Delete"
                class="delete-btn"
                @click.stop="handleDelete(preset)"
              />
            </el-tooltip>
          </div>

          <!-- 颜色点 -->
          <div class="color-dots">
            <el-tooltip
              v-for="(color, key) in preset.colors"
              :key="key"
              :content="t(`settings.${key}Color`)"
              placement="top"
            >
              <span
                class="color-dot"
                :style="{ backgroundColor: color }"
              />
            </el-tooltip>
          </div>

          <!-- 创建时间 -->
          <div class="preset-meta">
            <Icon icon="calendar" class="meta-icon" />
            <span>{{ formatDate(preset.createdAt) }}</span>
          </div>
        </div>

        <!-- 选中标记 -->
        <div v-if="currentPresetId === preset.id" class="active-indicator">
          <Icon icon="check" class="check-icon" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'
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
    // User cancelled
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
  .preset-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 12px;

    .preset-card {
      position: relative;
      display: flex;
      flex-direction: column;
      background: var(--bg-color-overlay);
      border: 2px solid var(--border-color-light);
      border-radius: 10px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.25s ease;

      &:hover {
        border-color: var(--primary-color-light-5);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);

        .delete-btn {
          opacity: 1;
        }
      }

      &.active {
        border-color: var(--primary-color);
        box-shadow: 0 0 0 3px var(--primary-color-light-9);

        .preset-content .preset-header .preset-name {
          color: var(--primary-color);
        }
      }

      // 颜色条
      .color-bar {
        display: flex;
        height: 6px;

        .color-segment {
          transition: flex 0.2s ease;
        }
      }

      // 预设内容
      .preset-content {
        display: flex;
        flex-direction: column;
        gap: 10px;
        padding: 12px;

        .preset-header {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .preset-name {
            font-size: 14px;
            font-weight: 600;
            color: var(--text-color-primary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            max-width: 120px;
          }

          .delete-btn {
            opacity: 0;
            transition: opacity 0.2s ease;

            &:hover {
              opacity: 1 !important;
            }
          }
        }

        // 颜色点
        .color-dots {
          display: flex;
          gap: 6px;

          .color-dot {
            width: 18px;
            height: 18px;
            border-radius: 4px;
            border: 2px solid var(--border-color-lighter);
            transition: transform 0.2s ease;

            &:hover {
              transform: scale(1.2);
            }
          }
        }

        // 元信息
        .preset-meta {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          color: var(--text-color-secondary);

          .meta-icon {
            font-size: 12px;
          }
        }
      }

      // 选中标记
      .active-indicator {
        position: absolute;
        top: 8px;
        right: 8px;
        width: 20px;
        height: 20px;
        background: var(--primary-color);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        .check-icon {
          color: white;
          font-size: 12px;
        }
      }
    }
  }
}

// 深色模式适配
[data-theme='dark'] .custom-preset-list {
  .preset-card {
    background: #1e293b;
    border-color: #334155;

    &:hover {
      border-color: var(--primary-color-light-5);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }

    .preset-content {
      .color-dots .color-dot {
        border-color: #475569;
      }
    }
  }
}
</style>