<!-- src/components/ThemeSettings/components/SystemSettings.vue -->
<template>
  <div class="system-settings">
    <!-- 圆角设置 -->
    <div class="setting-item">
      <div class="setting-header">
        <span class="setting-label">{{ t('settings.borderRadius') }}</span>
        <span class="setting-value">{{ localConfig.borderRadius }}px</span>
      </div>
      <div class="setting-control">
        <el-slider
          v-model="localConfig.borderRadius"
          :min="0"
          :max="24"
          :step="1"
          :show-tooltip="false"
          @change="handleConfigChange"
        />
        <div class="slider-marks">
          <span>0</span>
          <span>8</span>
          <span>24</span>
        </div>
      </div>
      <div class="preview-row">
        <div class="radius-preview" :style="{ borderRadius: `${localConfig.borderRadius}px` }">
          <span>{{ t('settings.radiusPreview') }}</span>
        </div>
      </div>
    </div>

    <!-- 阴影强度 -->
    <div class="setting-item">
      <div class="setting-header">
        <span class="setting-label">{{ t('settings.shadowIntensity') }}</span>
      </div>
      <div class="setting-control">
        <div class="radio-group">
          <button
            v-for="option in shadowOptions"
            :key="option.value"
            class="radio-btn"
            :class="{ active: localConfig.shadowIntensity === option.value }"
            @click="setShadowIntensity(option.value)"
          >
            <span class="radio-label">{{ t(option.label) }}</span>
          </button>
        </div>
      </div>
      <div class="preview-row">
        <div class="shadow-preview" :style="shadowPreviewStyle">
          <span>{{ t('settings.shadowPreview') }}</span>
        </div>
      </div>
    </div>

    <!-- 紧凑模式 -->
    <div class="setting-item">
      <div class="setting-header">
        <span class="setting-label">{{ t('settings.compactMode') }}</span>
        <el-switch v-model="localConfig.compactMode" size="small" @change="handleConfigChange" />
      </div>
      <div class="setting-desc">
        {{ t('settings.compactModeDesc') }}
      </div>
      <div class="preview-row">
        <div class="compact-preview" :class="{ compact: localConfig.compactMode }">
          <div class="preview-item"></div>
          <div class="preview-item"></div>
          <div class="preview-item"></div>
        </div>
      </div>
    </div>

    <!-- 内容宽度 -->
    <div class="setting-item">
      <div class="setting-header">
        <span class="setting-label">{{ t('settings.contentWidth') }}</span>
      </div>
      <div class="setting-control">
        <div class="radio-group">
          <button
            class="radio-btn"
            :class="{ active: localConfig.contentWidth === 'fluid' }"
            @click="setContentWidth('fluid')"
          >
            <Icon icon="arrows-horizontal" class="btn-icon" />
            <span class="radio-label">{{ t('settings.contentFluid') }}</span>
          </button>
          <button
            class="radio-btn"
            :class="{ active: localConfig.contentWidth === 'fixed' }"
            @click="setContentWidth('fixed')"
          >
            <Icon icon="layout-width" class="btn-icon" />
            <span class="radio-label">{{ t('settings.contentFixed') }}</span>
          </button>
        </div>
      </div>
      <div class="preview-row">
        <div class="width-preview-wrapper">
          <div
            class="width-preview"
            :style="{ width: localConfig.contentWidth === 'fixed' ? '50%' : '100%' }"
          >
            <span>
              {{
                localConfig.contentWidth === 'fluid'
                  ? t('settings.fluidLabel')
                  : t('settings.fixedLabel')
              }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import type { SystemConfig } from '../types'

defineOptions({
  name: 'SystemSettings',
})

interface Props {
  config: SystemConfig
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'change', config: SystemConfig): void
}>()

const { t } = useI18n()

// Shadow options
const shadowOptions: Array<{ value: SystemConfig['shadowIntensity']; label: string }> = [
  { value: 'none', label: 'settings.shadowNone' },
  { value: 'light', label: 'settings.shadowLight' },
  { value: 'medium', label: 'settings.shadowMedium' },
  { value: 'strong', label: 'settings.shadowStrong' },
]

// Local reactive copy of config
const localConfig = reactive<SystemConfig>({
  borderRadius: props.config.borderRadius,
  shadowIntensity: props.config.shadowIntensity,
  compactMode: props.config.compactMode,
  contentWidth: props.config.contentWidth,
})

// Deep watch props.config for external changes
watch(
  () => props.config,
  (newConfig) => {
    localConfig.borderRadius = newConfig.borderRadius
    localConfig.shadowIntensity = newConfig.shadowIntensity
    localConfig.compactMode = newConfig.compactMode
    localConfig.contentWidth = newConfig.contentWidth
  },
  { deep: true }
)

// Shadow preview style
const shadowPreviewStyle = computed(() => {
  const intensityMap: Record<SystemConfig['shadowIntensity'], string> = {
    none: 'none',
    light: '0 2px 8px rgba(0, 0, 0, 0.08)',
    medium: '0 4px 16px rgba(0, 0, 0, 0.12)',
    strong: '0 8px 24px rgba(0, 0, 0, 0.18)',
  }
  return {
    boxShadow: intensityMap[localConfig.shadowIntensity],
  }
})

// Set shadow intensity
const setShadowIntensity = (value: SystemConfig['shadowIntensity']) => {
  localConfig.shadowIntensity = value
  handleConfigChange()
}

// Set content width
const setContentWidth = (value: SystemConfig['contentWidth']) => {
  localConfig.contentWidth = value
  handleConfigChange()
}

// Handle config change
const handleConfigChange = () => {
  emit('change', { ...localConfig })
}
</script>

<style scoped lang="scss">
.system-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .setting-item {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
    background: var(--bg-color-page);
    border-radius: 10px;
    transition: background-color 0.2s ease;

    &:hover {
      background: var(--bg-color-overlay);
    }

    .setting-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .setting-label {
        font-size: 14px;
        font-weight: 500;
        color: var(--text-color-primary);
      }

      .setting-value {
        font-size: 13px;
        font-weight: 600;
        color: var(--primary-color);
        padding: 2px 8px;
        background: var(--primary-color-light-9);
        border-radius: 4px;
      }
    }

    .setting-desc {
      font-size: 12px;
      color: var(--text-color-secondary);
      line-height: 1.5;
    }

    .setting-control {
      :deep(.el-slider) {
        .el-slider__runway {
          height: 6px;
          background: var(--border-color-light);
          border-radius: 3px;
        }

        .el-slider__bar {
          height: 6px;
          background: var(--primary-color);
          border-radius: 3px;
        }

        .el-slider__button {
          width: 16px;
          height: 16px;
          border: 3px solid var(--primary-color);
          background: white;
        }
      }

      .slider-marks {
        display: flex;
        justify-content: space-between;
        margin-top: 4px;
        font-size: 11px;
        color: var(--text-color-secondary);
      }

      .radio-group {
        display: flex;
        gap: 8px;

        .radio-btn {
          display: flex;
          align-items: center;
          gap: 6px;
          padding: 8px 16px;
          border: 2px solid var(--border-color-light);
          border-radius: 8px;
          background: var(--bg-color-overlay);
          color: var(--text-color-regular);
          font-size: 13px;
          cursor: pointer;
          transition: all 0.2s ease;

          &:hover {
            border-color: var(--primary-color-light-5);
            background: var(--primary-color-light-9);
          }

          &.active {
            border-color: var(--primary-color);
            background: var(--primary-color-light-9);
            color: var(--primary-color);

            .btn-icon {
              color: var(--primary-color);
            }
          }

          .btn-icon {
            font-size: 16px;
            color: var(--text-color-secondary);
          }

          .radio-label {
            font-weight: 500;
          }
        }
      }
    }

    .preview-row {
      display: flex;
      justify-content: center;
      padding: 12px;
      background: var(--bg-color-overlay);
      border-radius: 6px;

      // 圆角预览
      .radius-preview {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 12px 24px;
        background: var(--primary-color);
        color: white;
        font-size: 13px;
        font-weight: 500;
        transition: border-radius 0.3s ease;
      }

      // 阴影预览
      .shadow-preview {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 16px 32px;
        background: var(--bg-color-page);
        border: 1px solid var(--border-color-light);
        font-size: 13px;
        color: var(--text-color-regular);
        transition: box-shadow 0.3s ease;
      }

      // 紧凑模式预览
      .compact-preview {
        display: flex;
        gap: 16px;
        transition: gap 0.3s ease;

        &.compact {
          gap: 8px;
        }

        .preview-item {
          width: 40px;
          height: 40px;
          background: var(--primary-color-light-5);
          border-radius: 6px;
        }
      }

      // 内容宽度预览
      .width-preview-wrapper {
        width: 100%;
        max-width: 200px;
        background: var(--bg-color-page);
        padding: 4px;
        border-radius: 6px;

        .width-preview {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 8px 12px;
          background: var(--primary-color-light-5);
          border-radius: 4px;
          font-size: 12px;
          color: var(--primary-color);
          font-weight: 500;
          transition: width 0.3s ease;
        }
      }
    }
  }
}

// 深色模式适配
[data-theme='dark'] .system-settings {
  .setting-item {
    background: #1e293b;

    &:hover {
      background: #334155;
    }

    .setting-control .radio-group .radio-btn {
      background: #334155;
      border-color: #475569;

      &.active {
        background: rgba(59, 130, 246, 0.2);
      }
    }

    .preview-row {
      background: #0f172a;
    }
  }
}
</style>
