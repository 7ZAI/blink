<!-- src/components/ThemeSettings/components/PresetSelector.vue -->
<template>
  <div class="preset-selector">
    <div class="preset-grid">
      <div
        v-for="preset in presets"
        :key="preset.id"
        class="preset-card"
        :class="{ active: currentPresetId === preset.id }"
        @click="handleSelect(preset.id)"
      >
        <!-- 渐变背景预览 -->
        <div
          class="preset-preview"
          :style="getPresetPreviewStyle(preset)"
        >
          <div class="preview-inner">
            <span class="preview-dot primary" :style="{ backgroundColor: preset.colors.primary }"></span>
            <span class="preview-dot success" :style="{ backgroundColor: preset.colors.success }"></span>
            <span class="preview-dot warning" :style="{ backgroundColor: preset.colors.warning }"></span>
          </div>
        </div>
        <!-- 预设名称 -->
        <div class="preset-info">
          <span class="preset-name">{{ locale === 'zh_cn' ? preset.name : preset.nameEn }}</span>
          <span class="preset-hint">{{ getPresetHint(preset) }}</span>
        </div>
        <!-- 选中标记 -->
        <div v-if="currentPresetId === preset.id" class="active-badge">
          <Icon icon="check" class="check-icon" />
        </div>
      </div>
    </div>
    <div v-if="$slots.footer" class="preset-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import type { PresetTheme } from '../types'

defineOptions({
  name: 'PresetSelector',
})

interface Props {
  presets: PresetTheme[]
  currentPresetId?: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', presetId: string): void
}>()

const { t, locale } = useI18n()

const handleSelect = (presetId: string) => {
  emit('select', presetId)
}

/**
 * 获取预设预览样式 - 使用渐变背景
 */
const getPresetPreviewStyle = (preset: PresetTheme) => {
  const { primary, success } = preset.colors
  const isActive = props.currentPresetId === preset.id
  return {
    background: `linear-gradient(135deg, ${primary} 0%, ${primary}40 50%, ${success}30 100%)`,
    boxShadow: isActive
      ? `0 0 0 2px ${primary}, 0 4px 12px ${primary}40`
      : `0 2px 8px rgba(0, 0, 0, 0.1)`,
  }
}

/**
 * 获取预设提示文字
 */
const getPresetHint = (preset: PresetTheme) => {
  // 根据主色调给出风格提示
  const hue = getHueFromHex(preset.colors.primary)
  if (hue >= 200 && hue <= 260) return t('settings.styleModern')
  if (hue >= 80 && hue <= 160) return t('settings.styleNatural')
  if (hue >= 0 && hue <= 40 || hue >= 320) return t('settings.styleWarm')
  if (hue >= 260 && hue <= 320) return t('settings.styleCreative')
  return t('settings.styleClassic')
}

/**
 * 从十六进制颜色获取色相值
 */
const getHueFromHex = (hex: string): number => {
  const r = parseInt(hex.slice(1, 3), 16) / 255
  const g = parseInt(hex.slice(3, 5), 16) / 255
  const b = parseInt(hex.slice(5, 7), 16) / 255

  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const d = max - min

  if (d === 0) return 0

  let h = 0
  if (max === r) {
    h = ((g - b) / d + (g < b ? 6 : 0)) / 6
  } else if (max === g) {
    h = ((b - r) / d + 2) / 6
  } else {
    h = ((r - g) / d + 4) / 6
  }

  return Math.round(h * 360)
}
</script>

<style scoped lang="scss">
.preset-selector {
  .preset-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;

    .preset-card {
      position: relative;
      display: flex;
      flex-direction: column;
      padding: 12px;
      border: 2px solid var(--border-color-light);
      border-radius: 10px;
      background: var(--bg-color-overlay);
      cursor: pointer;
      transition: all 0.25s ease;
      overflow: hidden;

      &:hover {
        border-color: var(--primary-color-light-5);
        transform: translateY(-2px);

        .preset-preview {
          transform: scale(1.05);
        }
      }

      &.active {
        border-color: var(--primary-color);

        .preset-info .preset-name {
          color: var(--primary-color);
          font-weight: 600;
        }
      }

      // 渐变预览区域
      .preset-preview {
        position: relative;
        height: 56px;
        border-radius: 6px;
        margin-bottom: 10px;
        transition: all 0.25s ease;
        display: flex;
        align-items: center;
        justify-content: center;

        .preview-inner {
          display: flex;
          gap: 6px;
          padding: 8px;
          background: rgba(255, 255, 255, 0.9);
          border-radius: 20px;

          .preview-dot {
            width: 16px;
            height: 16px;
            border-radius: 50%;
            border: 2px solid rgba(255, 255, 255, 0.5);
          }
        }
      }

      // 预设信息
      .preset-info {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .preset-name {
          font-size: 13px;
          font-weight: 500;
          color: var(--text-color-primary);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .preset-hint {
          font-size: 11px;
          color: var(--text-color-secondary);
        }
      }

      // 选中标记
      .active-badge {
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

  .preset-footer {
    margin-top: 12px;
  }
}

// 深色模式适配
[data-theme='dark'] .preset-selector {
  .preset-card {
    background: #1e293b;
    border-color: #334155;

    &:hover {
      border-color: var(--primary-color-light-5);
    }

    .preset-preview .preview-inner {
      background: rgba(30, 41, 59, 0.9);
    }
  }
}
</style>