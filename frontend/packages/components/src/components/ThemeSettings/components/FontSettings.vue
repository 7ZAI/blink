<!-- src/components/ThemeSettings/components/FontSettings.vue -->
<template>
  <div class="font-settings">
    <!-- 字体选择 -->
    <div class="font-select-section">
      <span class="section-label">{{ t('settings.fontFamily') }}</span>
      <div class="font-select-wrapper">
        <el-select
          v-model="localFont.family"
          class="font-select"
          popper-class="font-select-popper"
          @change="handleFontChange"
        >
          <el-option
            v-for="font in fonts"
            :key="font.family"
            :label="font.label"
            :value="font.family"
          >
            <div class="font-option">
              <span class="font-preview" :style="{ fontFamily: font.family }">
                {{ font.label }}
              </span>
              <el-tag v-if="font.family === defaultFontFamily" size="small" type="info">
                {{ t('settings.default') }}
              </el-tag>
            </div>
          </el-option>
        </el-select>
      </div>
    </div>

    <!-- 字号调节 -->
    <div class="size-section">
      <div class="size-item">
        <div class="size-header">
          <span class="size-label">{{ t('settings.baseFontSize') }}</span>
          <span class="size-value">{{ localFont.baseSize }}px</span>
        </div>
        <div class="size-slider">
          <el-slider
            v-model="localFont.baseSize"
            :min="fontSizeConfig.base.min"
            :max="fontSizeConfig.base.max"
            :step="1"
            :show-tooltip="false"
            @change="handleFontChange"
          />
          <div class="size-marks">
            <span>{{ fontSizeConfig.base.min }}</span>
            <span>{{ fontSizeConfig.base.default }}</span>
            <span>{{ fontSizeConfig.base.max }}</span>
          </div>
        </div>
      </div>

      <div class="size-item">
        <div class="size-header">
          <span class="size-label">{{ t('settings.largeFontSize') }}</span>
          <span class="size-value">{{ localFont.largeSize }}px</span>
        </div>
        <div class="size-slider">
          <el-slider
            v-model="localFont.largeSize"
            :min="fontSizeConfig.large.min"
            :max="fontSizeConfig.large.max"
            :step="1"
            :show-tooltip="false"
            @change="handleFontChange"
          />
          <div class="size-marks">
            <span>{{ fontSizeConfig.large.min }}</span>
            <span>{{ fontSizeConfig.large.default }}</span>
            <span>{{ fontSizeConfig.large.max }}</span>
          </div>
        </div>
      </div>

      <div class="size-item">
        <div class="size-header">
          <span class="size-label">{{ t('settings.smallFontSize') }}</span>
          <span class="size-value">{{ localFont.smallSize }}px</span>
        </div>
        <div class="size-slider">
          <el-slider
            v-model="localFont.smallSize"
            :min="fontSizeConfig.small.min"
            :max="fontSizeConfig.small.max"
            :step="1"
            :show-tooltip="false"
            @change="handleFontChange"
          />
          <div class="size-marks">
            <span>{{ fontSizeConfig.small.min }}</span>
            <span>{{ fontSizeConfig.small.default }}</span>
            <span>{{ fontSizeConfig.small.max }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 实时预览 -->
    <div class="preview-section">
      <div class="preview-header">
        <span class="preview-label">{{ t('settings.fontPreview') }}</span>
        <el-button size="small" text type="primary" @click="resetFontSizes">
          {{ t('settings.resetFontSizes') }}
        </el-button>
      </div>
      <div class="preview-container" :style="previewContainerStyle">
        <div class="preview-sample">
          <div class="sample-large" :style="previewStyleLarge">
            {{ t('settings.sampleLarge') }}
          </div>
          <div class="sample-base" :style="previewStyleBase">
            {{ t('settings.sampleBase') }}
          </div>
          <div class="sample-small" :style="previewStyleSmall">
            {{ t('settings.sampleSmall') }}
          </div>
          <div class="sample-code" :style="previewStyleBase">
            <code>const theme = "beautiful";</code>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FontOption } from '../types'
import { FONT_SIZE_CONFIG } from '@/config/themes'

defineOptions({
  name: 'FontSettings',
})

interface FontConfig {
  family: string
  baseSize: number
  largeSize: number
  smallSize: number
}

interface Props {
  fonts: FontOption[]
  font: FontConfig
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'change', font: FontConfig): void
}>()

const { t } = useI18n()

// Font size configuration
const fontSizeConfig = FONT_SIZE_CONFIG

// Default font family
const defaultFontFamily = props.fonts[0]?.family || ''

// Local reactive copy of font config
const localFont = reactive<FontConfig>({
  family: props.font.family,
  baseSize: props.font.baseSize,
  largeSize: props.font.largeSize,
  smallSize: props.font.smallSize,
})

// Deep watch props.font for external changes
watch(
  () => props.font,
  (newFont) => {
    localFont.family = newFont.family
    localFont.baseSize = newFont.baseSize
    localFont.largeSize = newFont.largeSize
    localFont.smallSize = newFont.smallSize
  },
  { deep: true }
)

// Preview styles
const previewContainerStyle = computed(() => ({
  fontFamily: localFont.family,
}))

const previewStyleLarge = computed(() => ({
  fontFamily: localFont.family,
  fontSize: `${localFont.largeSize}px`,
}))

const previewStyleBase = computed(() => ({
  fontFamily: localFont.family,
  fontSize: `${localFont.baseSize}px`,
}))

const previewStyleSmall = computed(() => ({
  fontFamily: localFont.family,
  fontSize: `${localFont.smallSize}px`,
}))

// Handle font change
const handleFontChange = () => {
  emit('change', {
    family: localFont.family,
    baseSize: localFont.baseSize,
    largeSize: localFont.largeSize,
    smallSize: localFont.smallSize,
  })
}

// Reset font sizes to default
const resetFontSizes = () => {
  localFont.baseSize = fontSizeConfig.base.default
  localFont.largeSize = fontSizeConfig.large.default
  localFont.smallSize = fontSizeConfig.small.default
  handleFontChange()
}
</script>

<style scoped lang="scss">
.font-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;

  // 字体选择区域
  .font-select-section {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .section-label {
      font-size: 13px;
      font-weight: 500;
      color: var(--text-color-regular);
    }

    .font-select-wrapper {
      .font-select {
        width: 100%;
        max-width: 320px;

        :deep(.el-input__wrapper) {
          padding: 8px 12px;
        }
      }
    }
  }

  // 字号调节区域
  .size-section {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .size-item {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding: 12px 16px;
      background: var(--bg-color-page);
      border-radius: 8px;

      .size-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .size-label {
          font-size: 13px;
          font-weight: 500;
          color: var(--text-color-regular);
        }

        .size-value {
          font-size: 13px;
          font-weight: 600;
          color: var(--primary-color);
          padding: 2px 8px;
          background: var(--primary-color-light-9);
          border-radius: 4px;
        }
      }

      .size-slider {
        :deep(.el-slider) {
          .el-slider__runway {
            height: 6px;
            background: var(--border-color-light);
            border-radius: 3px;
          }

          .el-slider__bar {
            height: 6px;
            background: linear-gradient(
              90deg,
              var(--primary-color-light-5) 0%,
              var(--primary-color) 100%
            );
            border-radius: 3px;
          }

          .el-slider__button {
            width: 16px;
            height: 16px;
            border: 3px solid var(--primary-color);
            background: white;
          }
        }

        .size-marks {
          display: flex;
          justify-content: space-between;
          margin-top: 4px;
          font-size: 11px;
          color: var(--text-color-secondary);
        }
      }
    }
  }

  // 预览区域
  .preview-section {
    padding: 16px;
    background: var(--bg-color-overlay);
    border-radius: 10px;
    border: 1px solid var(--border-color-light);

    .preview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .preview-label {
        font-size: 13px;
        font-weight: 500;
        color: var(--text-color-secondary);
      }
    }

    .preview-container {
      padding: 16px;
      background: var(--bg-color-page);
      border-radius: 8px;

      .preview-sample {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .sample-large {
          font-weight: 600;
          color: var(--text-color-primary);
          line-height: 1.4;
        }

        .sample-base {
          font-weight: 400;
          color: var(--text-color-regular);
          line-height: 1.6;
        }

        .sample-small {
          font-weight: 400;
          color: var(--text-color-secondary);
          line-height: 1.4;
        }

        .sample-code {
          code {
            padding: 4px 8px;
            background: var(--bg-color-overlay);
            border-radius: 4px;
            color: var(--primary-color);
            font-family: 'JetBrains Mono', monospace;
          }
        }
      }
    }
  }
}

// 字体选择下拉选项样式
.font-option {
  display: flex;
  align-items: center;
  gap: 8px;

  .font-preview {
    flex: 1;
  }
}

// 深色模式适配
[data-theme='dark'] .font-settings {
  .size-section .size-item {
    background: #1e293b;
  }

  .preview-section {
    background: #1e293b;
    border-color: #334155;

    .preview-container {
      background: #0f172a;
    }
  }
}
</style>
