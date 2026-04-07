<!-- src/components/ThemeSettings/components/FontSettings.vue -->
<template>
  <div class="font-settings">
    <h4 class="section-title">{{ t('settings.fontSettings') }}</h4>
    <el-form label-position="left" label-width="100px" class="font-form">
      <el-form-item :label="t('settings.fontFamily')">
        <el-select
          v-model="localFont.family"
          class="font-select"
          @change="handleFontChange"
        >
          <el-option
            v-for="font in fonts"
            :key="font.family"
            :label="font.label"
            :value="font.family"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('settings.baseFontSize')">
        <div class="size-slider-wrapper">
          <el-slider
            v-model="localFont.baseSize"
            :min="fontSizeConfig.base.min"
            :max="fontSizeConfig.base.max"
            :step="1"
            show-input
            :show-input-controls="false"
            input-size="small"
            @change="handleFontChange"
          />
          <span class="size-unit">px</span>
        </div>
      </el-form-item>

      <el-form-item :label="t('settings.largeFontSize')">
        <div class="size-slider-wrapper">
          <el-slider
            v-model="localFont.largeSize"
            :min="fontSizeConfig.large.min"
            :max="fontSizeConfig.large.max"
            :step="1"
            show-input
            :show-input-controls="false"
            input-size="small"
            @change="handleFontChange"
          />
          <span class="size-unit">px</span>
        </div>
      </el-form-item>

      <el-form-item :label="t('settings.smallFontSize')">
        <div class="size-slider-wrapper">
          <el-slider
            v-model="localFont.smallSize"
            :min="fontSizeConfig.small.min"
            :max="fontSizeConfig.small.max"
            :step="1"
            show-input
            :show-input-controls="false"
            input-size="small"
            @change="handleFontChange"
          />
          <span class="size-unit">px</span>
        </div>
      </el-form-item>
    </el-form>

    <!-- Font Preview Section -->
    <div class="font-preview-section">
      <div class="preview-label">{{ t('settings.fontPreview') }}</div>
      <div class="preview-box">
        <div class="preview-large" :style="previewStyleLarge">
          {{ previewTextLarge }}
        </div>
        <div class="preview-base" :style="previewStyleBase">
          {{ previewTextBase }}
        </div>
        <div class="preview-small" :style="previewStyleSmall">
          {{ previewTextSmall }}
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

const { t, locale } = useI18n()

// Font size configuration from themes.ts
const fontSizeConfig = FONT_SIZE_CONFIG

// Preview texts based on locale
const previewTextLarge = computed(() =>
  locale.value === 'zh_cn' ? '这是大字号预览文本' : 'This is large font preview text'
)
const previewTextBase = computed(() =>
  locale.value === 'zh_cn'
    ? '这是基础字号预览文本，用于正文显示效果。'
    : 'This is base font preview text, used for body content display.'
)
const previewTextSmall = computed(() =>
  locale.value === 'zh_cn' ? '这是小字号预览文本' : 'This is small font preview text'
)

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

// Preview styles computed from font settings
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

// Handle font change and emit event
const handleFontChange = () => {
  emit('change', {
    family: localFont.family,
    baseSize: localFont.baseSize,
    largeSize: localFont.largeSize,
    smallSize: localFont.smallSize,
  })
}
</script>

<style scoped lang="scss">
.font-settings {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 16px;
  }

  .font-form {
    .el-form-item {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  .font-select {
    width: 100%;
    max-width: 280px;
  }

  .size-slider-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    max-width: 320px;

    :deep(.el-slider) {
      flex: 1;

      .el-slider__runway {
        background-color: var(--border-color-light);
      }

      .el-slider__bar {
        background-color: var(--primary-color);
      }

      .el-slider__button {
        border-color: var(--primary-color);
      }
    }

    :deep(.el-input-number) {
      width: 60px;

      .el-input__wrapper {
        padding: 0 8px;
      }
    }

    .size-unit {
      font-size: 12px;
      color: var(--text-color-secondary);
      min-width: 20px;
    }
  }

  .font-preview-section {
    margin-top: 20px;
    padding: 16px;
    background-color: var(--bg-color-overlay);
    border: 1px solid var(--border-color-light);
    border-radius: 8px;

    .preview-label {
      font-size: 12px;
      color: var(--text-color-secondary);
      margin-bottom: 12px;
    }

    .preview-box {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .preview-large {
        font-weight: 600;
        color: var(--text-color-primary);
        line-height: 1.5;
      }

      .preview-base {
        font-weight: 400;
        color: var(--text-color-regular);
        line-height: 1.6;
      }

      .preview-small {
        font-weight: 400;
        color: var(--text-color-secondary);
        line-height: 1.4;
      }
    }
  }

  // Dark mode adjustments using CSS variables
  @media (prefers-color-scheme: dark) {
    .section-title {
      color: var(--text-color-primary);
    }

    .font-preview-section {
      background-color: var(--bg-color-overlay);
      border-color: var(--border-color-light);
    }
  }
}
</style>