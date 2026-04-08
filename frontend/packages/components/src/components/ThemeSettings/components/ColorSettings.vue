<!-- src/components/ThemeSettings/components/ColorSettings.vue -->
<template>
  <div class="color-settings">
    <div class="color-grid">
      <div
        v-for="(config, key) in colorConfigs"
        :key="key"
        class="color-item"
      >
        <div class="color-label-row">
          <span
            class="color-dot"
            :style="{ backgroundColor: localColors[key] }"
          />
          <span class="color-label">{{ t(config.label) }}</span>
        </div>
        <div class="color-input-row">
          <el-color-picker
            v-model="localColors[key]"
            :predefine="predefineColors"
            size="large"
            @change="handleColorChange(key)"
          />
          <el-input
            v-model="localColors[key]"
            :placeholder="config.placeholder"
            class="color-text-input"
            @change="handleColorChange(key)"
          >
            <template #suffix>
              <span class="hex-badge">HEX</span>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <!-- 颜色对比预览 -->
    <div class="contrast-preview">
      <div class="preview-header">
        <span class="preview-label">{{ t('settings.contrastPreview') }}</span>
      </div>
      <div class="preview-grid">
        <div
          class="preview-button primary"
          :style="{ backgroundColor: localColors.primary }"
        >
          {{ t('settings.primaryBtn') }}
        </div>
        <div
          class="preview-button success"
          :style="{ backgroundColor: localColors.success }"
        >
          {{ t('settings.successBtn') }}
        </div>
        <div
          class="preview-button warning"
          :style="{ backgroundColor: localColors.warning }"
        >
          {{ t('settings.warningBtn') }}
        </div>
        <div
          class="preview-button danger"
          :style="{ backgroundColor: localColors.danger }"
        >
          {{ t('settings.dangerBtn') }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ThemeColors } from '../types'

defineOptions({
  name: 'ColorSettings',
})

interface Props {
  colors: ThemeColors
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'change', colors: ThemeColors): void
}>()

const { t } = useI18n()

// Color configurations
const colorConfigs = {
  primary: {
    label: 'settings.primaryColor',
    placeholder: '#3b82f6',
  },
  success: {
    label: 'settings.successColor',
    placeholder: '#10b981',
  },
  warning: {
    label: 'settings.warningColor',
    placeholder: '#f59e0b',
  },
  danger: {
    label: 'settings.dangerColor',
    placeholder: '#ef4444',
  },
  info: {
    label: 'settings.infoColor',
    placeholder: '#6366f1',
  },
}

// Predefined colors for quick selection
const predefineColors = [
  '#3b82f6', '#8b5cf6', '#f97316', '#10b981', '#ef4444', '#06b6d4',
  '#64748b', '#1e293b', '#6366f1', '#f43f5e', '#22c55e', '#fbbf24',
  '#0d9488', '#14b8a6', '#2563eb', '#7c3aed', '#db2777', '#ea580c',
]

// Local reactive copy of colors
const localColors = reactive<ThemeColors>({
  primary: props.colors.primary,
  success: props.colors.success,
  warning: props.colors.warning,
  danger: props.colors.danger,
  info: props.colors.info,
})

// Deep watch props.colors for external changes
watch(
  () => props.colors,
  (newColors) => {
    localColors.primary = newColors.primary
    localColors.success = newColors.success
    localColors.warning = newColors.warning
    localColors.danger = newColors.danger
    localColors.info = newColors.info
  },
  { deep: true }
)

// Handle color change
const handleColorChange = (key: keyof ThemeColors) => {
  let colorValue = localColors[key]

  // Validate and fix hex color format
  if (colorValue) {
    // Remove invalid characters
    colorValue = colorValue.replace(/[^0-9A-Fa-f#]/g, '')

    // Add # prefix if missing
    if (!colorValue.startsWith('#') && /^[0-9A-Fa-f]{6}$/.test(colorValue)) {
      colorValue = `#${colorValue}`
    }

    // Validate 6-digit hex
    if (/^#[0-9A-Fa-f]{6}$/.test(colorValue)) {
      localColors[key] = colorValue.toLowerCase()
    }
  }

  emit('change', { ...localColors })
}
</script>

<style scoped lang="scss">
.color-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;

  // 颜色网格
  .color-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;

    .color-item {
      display: flex;
      flex-direction: column;
      gap: 10px;
      padding: 12px;
      background: var(--bg-color-page);
      border-radius: 8px;
      transition: background-color 0.2s ease;

      &:hover {
        background: var(--bg-color-overlay);
      }

      .color-label-row {
        display: flex;
        align-items: center;
        gap: 8px;

        .color-dot {
          width: 24px;
          height: 24px;
          border-radius: 6px;
          border: 2px solid var(--border-color-light);
          transition: transform 0.2s ease;
        }

        .color-label {
          font-size: 13px;
          font-weight: 500;
          color: var(--text-color-regular);
        }
      }

      .color-input-row {
        display: flex;
        align-items: center;
        gap: 10px;

        :deep(.el-color-picker) {
          --el-color-picker-size: 36px;

          .el-color-picker__trigger {
            border: 2px solid var(--border-color-light);
            border-radius: 8px;
            transition: all 0.2s ease;

            &:hover {
              border-color: var(--primary-color);
            }
          }

          &.is-active .el-color-picker__trigger {
            border-color: var(--primary-color);
          }
        }

        .color-text-input {
          flex: 1;

          :deep(.el-input__wrapper) {
            padding: 4px 12px;
            border-radius: 6px;
          }

          :deep(.el-input__suffix) {
            display: flex;
            align-items: center;
          }

          .hex-badge {
            font-size: 10px;
            padding: 2px 4px;
            background: var(--bg-color-page);
            border-radius: 3px;
            color: var(--text-color-secondary);
            font-weight: 600;
          }
        }
      }
    }
  }

  // 对比预览
  .contrast-preview {
    padding: 16px;
    background: var(--bg-color-overlay);
    border-radius: 10px;
    border: 1px solid var(--border-color-light);

    .preview-header {
      margin-bottom: 12px;

      .preview-label {
        font-size: 13px;
        font-weight: 500;
        color: var(--text-color-secondary);
      }
    }

    .preview-grid {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;

      .preview-button {
        padding: 8px 16px;
        border-radius: 6px;
        color: white;
        font-size: 13px;
        font-weight: 500;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
        transition: all 0.2s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
        }
      }
    }
  }
}

// 深色模式适配
[data-theme='dark'] .color-settings {
  .color-grid .color-item {
    background: #1e293b;

    &:hover {
      background: #334155;
    }

    .color-dot {
      border-color: #475569;
    }
  }

  .contrast-preview {
    background: #1e293b;
    border-color: #334155;
  }
}
</style>