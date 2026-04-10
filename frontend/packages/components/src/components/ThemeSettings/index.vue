<!-- src/components/ThemeSettings/index.vue -->

<template>
  <div class="theme-settings">
    <!-- 实时预览区域 -->
    <div class="preview-section">
      <div class="preview-header">
        <span class="preview-label">{{ t('settings.realtimePreview') }}</span>
        <el-tag size="small" type="info">{{ t('settings.previewHint') }}</el-tag>
      </div>
      <div class="preview-container" :style="previewContainerStyle">
        <div class="preview-card" :style="previewCardStyle">
          <div class="preview-title">{{ t('settings.previewTitle') }}</div>
          <div class="preview-content">{{ t('settings.previewContent') }}</div>
          <div class="preview-actions">
            <el-button size="small" type="primary">{{ t('common.confirm') }}</el-button>
            <el-button size="small">{{ t('common.cancel') }}</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 深色/浅色模式切换 -->
    <section class="settings-section mode-section">
      <div class="mode-toggle-wrapper">
        <span class="section-label">{{ t('settings.displayMode') }}</span>
        <div class="mode-buttons">
          <button
            class="mode-btn"
            :class="{ active: currentMode === 'light' }"
            @click="handleModeChange('light')"
          >
            <Icon icon="sunny" class="mode-icon" />
            <span>{{ t('settings.lightMode') }}</span>
          </button>
          <button
            class="mode-btn"
            :class="{ active: currentMode === 'dark' }"
            @click="handleModeChange('dark')"
          >
            <Icon icon="moon" class="mode-icon" />
            <span>{{ t('settings.darkMode') }}</span>
          </button>
        </div>
      </div>
    </section>

    <!-- 预设主题选择 -->
    <section v-if="showPresets" class="settings-section">
      <div class="section-header">
        <h4 class="section-title">{{ t('settings.presetThemes') }}</h4>
        <el-tag size="small" effect="plain">
          {{ presetThemes.length }} {{ t('settings.available') }}
        </el-tag>
      </div>
      <PresetSelector
        :presets="presetThemes"
        :current-preset-id="currentPresetId"
        @select="handlePresetSelect"
      />
    </section>

    <!-- 颜色设置 -->
    <section v-if="showColors" class="settings-section">
      <div class="section-header">
        <h4 class="section-title">{{ t('settings.colorSettings') }}</h4>
        <el-button size="small" text type="primary" @click="resetColors">
          {{ t('settings.resetColors') }}
        </el-button>
      </div>
      <ColorSettings :colors="localConfig.colors" @change="handleColorChange" />
    </section>

    <!-- 字体设置 -->
    <section v-if="showFonts" class="settings-section">
      <div class="section-header">
        <h4 class="section-title">{{ t('settings.fontSettings') }}</h4>
      </div>
      <FontSettings :fonts="presetFonts" :font="localConfig.font" @change="handleFontChange" />
    </section>

    <!-- 动画设置 -->
    <section v-if="showAnimations" class="settings-section">
      <div class="section-header">
        <h4 class="section-title">{{ t('settings.animationSettings') }}</h4>
      </div>
      <div class="animation-control">
        <div class="animation-switch-row">
          <el-switch
            v-model="localConfig.animationsEnabled"
            :disabled="readonly"
            size="large"
            @change="handleAnimationChange"
          />
          <div class="animation-info">
            <span class="animation-label">{{ t('settings.enableAnimations') }}</span>
            <span class="animation-desc">
              {{
                localConfig.animationsEnabled
                  ? t('settings.animationsEnabledDesc')
                  : t('settings.animationsDisabledDesc')
              }}
            </span>
          </div>
        </div>
        <div class="animation-preview">
          <div class="preview-box animated" :class="{ paused: !localConfig.animationsEnabled }">
            <div class="preview-element"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- 操作按钮 -->
    <section class="settings-section actions-section">
      <div class="action-buttons">
        <el-button
          type="primary"
          :icon="CollectionTag"
          :disabled="readonly || isMaxPresetsReached"
          @click="openSaveDialog"
        >
          {{ t('settings.saveAsPreset') }}
        </el-button>
        <el-button :icon="RefreshRight" :disabled="readonly" @click="handleReset">
          {{ t('settings.resetToDefault') }}
        </el-button>
      </div>
      <p v-if="isMaxPresetsReached" class="max-presets-warning">
        <Icon icon="warning" class="warning-icon" />
        {{ t('settings.maxPresetsReached', { max: maxCustomPresets }) }}
      </p>
    </section>

    <!-- 自定义预设列表 -->
    <section v-if="customPresets.length > 0" class="settings-section">
      <div class="section-header">
        <h4 class="section-title">{{ t('settings.myPresets') }}</h4>
        <el-tag size="small" effect="plain">
          {{ customPresets.length }}/{{ maxCustomPresets }}
        </el-tag>
      </div>
      <CustomPresetList
        :presets="customPresets"
        :current-preset-id="currentPresetId"
        @select="handleCustomPresetSelect"
        @delete="handlePresetDelete"
      />
    </section>

    <!-- 保存预设对话框 -->
    <el-dialog
      v-model="saveDialogVisible"
      :title="t('settings.saveAsPreset')"
      width="420px"
      :close-on-click-modal="false"
      class="save-preset-dialog"
    >
      <el-form
        ref="presetFormRef"
        :model="presetForm"
        :rules="presetFormRules"
        label-position="top"
      >
        <el-form-item :label="t('settings.presetName')" prop="name">
          <el-input
            v-model="presetForm.name"
            :placeholder="t('settings.presetNamePlaceholder')"
            maxlength="50"
            show-word-limit
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('settings.presetNameEn')">
          <el-input
            v-model="presetForm.nameEn"
            :placeholder="t('settings.presetNameEnPlaceholder')"
            maxlength="50"
            show-word-limit
            clearable
          />
        </el-form-item>
        <!-- 预览当前颜色 -->
        <el-form-item :label="t('settings.colorPreview')">
          <div class="color-preview-row">
            <span
              v-for="(color, key) in localConfig.colors"
              :key="key"
              class="preview-dot"
              :style="{ backgroundColor: color }"
              :title="t(`settings.${key}Color`)"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button type="primary" :loading="saving" @click="handleSavePreset">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { CollectionTag, RefreshRight } from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'
import type { FormInstance, FormRules } from 'element-plus'
import PresetSelector from './components/PresetSelector.vue'
import ColorSettings from './components/ColorSettings.vue'
import FontSettings from './components/FontSettings.vue'
import CustomPresetList from './components/CustomPresetList.vue'
import type {
  ThemeSettingsProps,
  FullThemeConfig,
  ThemeColors,
  CustomPreset,
  FontOption,
  PresetTheme,
} from './types'
import {
  PRESET_THEMES,
  PRESET_FONTS,
  MAX_CUSTOM_PRESETS,
  DEFAULT_THEME_CONFIG,
  FONT_SIZE_CONFIG,
} from '@/config/themes'

defineOptions({
  name: 'ThemeSettings',
})

// Props with defaults
const props = withDefaults(defineProps<ThemeSettingsProps>(), {
  showPresets: true,
  showColors: true,
  showFonts: true,
  showAnimations: true,
  presetThemes: () => PRESET_THEMES,
  presetFonts: () => PRESET_FONTS,
  maxCustomPresets: MAX_CUSTOM_PRESETS,
  modelValue: () => ({
    presetId: DEFAULT_THEME_CONFIG.id,
    colors: DEFAULT_THEME_CONFIG.colors,
    font: {
      family: PRESET_FONTS[0]?.family || '',
      baseSize: FONT_SIZE_CONFIG.base.default,
      largeSize: FONT_SIZE_CONFIG.large.default,
      smallSize: FONT_SIZE_CONFIG.small.default,
    },
    animationsEnabled: true,
  }),
  customPresets: () => [],
  readonly: false,
  mode: 'light',
})

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: FullThemeConfig): void
  (e: 'update:mode', value: 'light' | 'dark'): void
  (e: 'preset-change', presetId: string): void
  (e: 'color-change', colors: ThemeColors): void
  (
    e: 'font-change',
    font: { family: string; baseSize: number; largeSize: number; smallSize: number }
  ): void
  (e: 'animation-change', enabled: boolean): void
  (e: 'preset-save', preset: CustomPreset): void
  (e: 'preset-delete', presetId: string): void
  (e: 'mode-change', mode: 'light' | 'dark'): void
}>()

const { t, locale } = useI18n()

// 当前显示模式
const currentMode = ref<'light' | 'dark'>(props.mode)

// Local reactive copy of config
const localConfig = reactive<FullThemeConfig>({
  presetId: props.modelValue.presetId,
  colors: { ...props.modelValue.colors },
  font: { ...props.modelValue.font },
  animationsEnabled: props.modelValue.animationsEnabled,
})

// Current preset ID (can be preset or custom)
const currentPresetId = computed(() => localConfig.presetId)

// Check if max presets reached
const isMaxPresetsReached = computed(() => props.customPresets.length >= props.maxCustomPresets)

// Save dialog state
const saveDialogVisible = ref(false)
const presetFormRef = ref<FormInstance>()
const saving = ref(false)
const presetForm = reactive({
  name: '',
  nameEn: '',
})

// Form validation rules
const presetFormRules = computed<FormRules>(() => ({
  name: [
    {
      required: true,
      message: t('settings.presetNameRequired'),
      trigger: 'blur',
    },
    {
      min: 2,
      max: 50,
      message: t('settings.presetNameLength'),
      trigger: 'blur',
    },
  ],
}))

// Preview container style
const previewContainerStyle = computed(() => ({
  backgroundColor: currentMode.value === 'dark' ? '#1e293b' : '#f8fafc',
  fontFamily: localConfig.font.family,
}))

// Preview card style
const previewCardStyle = computed(() => ({
  borderRadius: '8px',
  boxShadow: '0 4px 16px rgba(0, 0, 0, 0.12)',
  backgroundColor: currentMode.value === 'dark' ? '#334155' : '#ffffff',
}))

// Deep watch props.modelValue for external changes
watch(
  () => props.modelValue,
  (newValue) => {
    localConfig.presetId = newValue.presetId
    localConfig.colors = { ...newValue.colors }
    localConfig.font = { ...newValue.font }
    localConfig.animationsEnabled = newValue.animationsEnabled
  },
  { deep: true }
)

// Watch props.mode
watch(
  () => props.mode,
  (newMode) => {
    currentMode.value = newMode
  }
)

// Watch localConfig and emit update
watch(
  localConfig,
  (newValue) => {
    emit('update:modelValue', {
      presetId: newValue.presetId,
      colors: { ...newValue.colors },
      font: { ...newValue.font },
      animationsEnabled: newValue.animationsEnabled,
    })
  },
  { deep: true }
)

/**
 * Handle display mode change
 */
const handleModeChange = (mode: 'light' | 'dark') => {
  currentMode.value = mode
  emit('update:mode', mode)
  emit('mode-change', mode)
}

/**
 * Handle preset theme selection
 */
const handlePresetSelect = (presetId: string) => {
  const preset = props.presetThemes.find((p) => p.id === presetId)
  if (preset) {
    localConfig.presetId = presetId
    localConfig.colors = { ...preset.colors }
    emit('preset-change', presetId)
  }
}

/**
 * Handle custom preset selection
 */
const handleCustomPresetSelect = (presetId: string) => {
  const preset = props.customPresets.find((p) => p.id === presetId)
  if (preset) {
    localConfig.presetId = presetId
    localConfig.colors = { ...preset.colors }
    emit('preset-change', presetId)
  }
}

/**
 * Handle color change
 */
const handleColorChange = (colors: ThemeColors) => {
  localConfig.presetId = undefined
  localConfig.colors = { ...colors }
  emit('color-change', colors)
}

/**
 * Reset colors to default
 */
const resetColors = () => {
  const defaultPreset = props.presetThemes[0]
  if (defaultPreset) {
    localConfig.colors = { ...defaultPreset.colors }
    localConfig.presetId = defaultPreset.id
  }
}

/**
 * Handle font change
 */
const handleFontChange = (font: {
  family: string
  baseSize: number
  largeSize: number
  smallSize: number
}) => {
  localConfig.font = { ...font }
  emit('font-change', font)
}

/**
 * Handle animation change
 */
const handleAnimationChange = (enabled: string | number | boolean) => {
  const isEnabled = Boolean(enabled)
  localConfig.animationsEnabled = isEnabled
  emit('animation-change', isEnabled)
}

/**
 * Open save preset dialog
 */
const openSaveDialog = () => {
  presetForm.name = ''
  presetForm.nameEn = ''
  saveDialogVisible.value = true
}

/**
 * Handle save preset
 */
const handleSavePreset = async () => {
  if (!presetFormRef.value) return

  try {
    saving.value = true
    await presetFormRef.value.validate()

    const presetId = `custom-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

    const newPreset: CustomPreset = {
      id: presetId,
      name: presetForm.name,
      nameEn: presetForm.nameEn || presetForm.name,
      colors: { ...localConfig.colors },
      isPreset: false,
      createdAt: Date.now(),
    }

    emit('preset-save', newPreset)
    saveDialogVisible.value = false
    ElMessage.success(t('settings.presetSaved'))

    localConfig.presetId = presetId
  } catch {
    // Validation failed
  } finally {
    saving.value = false
  }
}

/**
 * Handle preset delete
 */
const handlePresetDelete = (presetId: string) => {
  emit('preset-delete', presetId)

  if (localConfig.presetId === presetId) {
    localConfig.presetId = undefined
  }
}

/**
 * Handle reset to default
 */
const handleReset = () => {
  const defaultPreset = props.presetThemes[0]
  if (defaultPreset) {
    localConfig.presetId = defaultPreset.id
    localConfig.colors = { ...defaultPreset.colors }
    localConfig.font = {
      family: props.presetFonts[0]?.family || '',
      baseSize: FONT_SIZE_CONFIG.base.default,
      largeSize: FONT_SIZE_CONFIG.large.default,
      smallSize: FONT_SIZE_CONFIG.small.default,
    }
    localConfig.animationsEnabled = true
    currentMode.value = 'light'
    emit('mode-change', 'light')
    ElMessage.success(t('settings.themeReset'))
  }
}

// Initialize mode from document
onMounted(() => {
  const htmlTheme = document.documentElement.getAttribute('data-theme')
  if (htmlTheme === 'dark' || htmlTheme === 'light') {
    currentMode.value = htmlTheme
  }
})
</script>

<style scoped lang="scss">
.theme-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;

  // 预览区域
  .preview-section {
    padding: 16px;
    background: linear-gradient(135deg, var(--bg-color-overlay) 0%, var(--bg-color-page) 100%);
    border-radius: 12px;
    border: 1px solid var(--border-color-light);

    .preview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .preview-label {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-color-primary);
      }
    }

    .preview-container {
      padding: 20px;
      border-radius: 8px;
      transition: background-color 0.3s ease;

      .preview-card {
        padding: 16px;
        transition: all 0.3s ease;

        .preview-title {
          font-size: 16px;
          font-weight: 600;
          color: var(--primary-color);
          margin-bottom: 8px;
        }

        .preview-content {
          font-size: 14px;
          color: var(--text-color-regular);
          margin-bottom: 12px;
          line-height: 1.6;
        }

        .preview-actions {
          display: flex;
          gap: 8px;
        }
      }
    }
  }

  // 设置区块
  .settings-section {
    padding: 16px 0;
    border-bottom: 1px solid var(--border-color-lighter);

    &:last-of-type {
      border-bottom: none;
      padding-bottom: 0;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .section-title {
        font-size: 15px;
        font-weight: 600;
        color: var(--text-color-primary);
        margin: 0;
      }
    }

    .section-label {
      font-size: 14px;
      font-weight: 500;
      color: var(--text-color-regular);
    }
  }

  // 深色/浅色模式切换
  .mode-section {
    .mode-toggle-wrapper {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .mode-buttons {
      display: flex;
      gap: 12px;

      .mode-btn {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 20px;
        border: 2px solid var(--border-color-light);
        border-radius: 8px;
        background: var(--bg-color-overlay);
        color: var(--text-color-regular);
        font-size: 14px;
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
        }

        .mode-icon {
          font-size: 18px;
        }
      }
    }
  }

  // 动画控制
  .animation-control {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .animation-switch-row {
      display: flex;
      align-items: center;
      gap: 16px;

      .animation-info {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .animation-label {
          font-size: 14px;
          font-weight: 500;
          color: var(--text-color-primary);
        }

        .animation-desc {
          font-size: 12px;
          color: var(--text-color-secondary);
        }
      }
    }

    .animation-preview {
      .preview-box {
        padding: 16px;
        background: var(--bg-color-page);
        border-radius: 8px;
        display: flex;
        justify-content: center;

        .preview-element {
          width: 40px;
          height: 40px;
          background: var(--primary-color);
          border-radius: 8px;
          animation: pulse 1.5s ease-in-out infinite;
        }

        &.paused .preview-element {
          animation: none;
        }
      }
    }
  }

  // 操作按钮
  .actions-section {
    .action-buttons {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .max-presets-warning {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-top: 12px;
      padding: 8px 12px;
      background: var(--warning-color-light-9);
      border-radius: 6px;
      font-size: 13px;
      color: var(--warning-color);

      .warning-icon {
        font-size: 16px;
      }
    }
  }

  // 颜色预览行
  .color-preview-row {
    display: flex;
    gap: 12px;

    .preview-dot {
      width: 32px;
      height: 32px;
      border-radius: 6px;
      border: 2px solid var(--border-color-light);
      transition: transform 0.2s ease;

      &:hover {
        transform: scale(1.1);
      }
    }
  }
}

// 保存预设对话框样式
.save-preset-dialog {
  :deep(.el-dialog__body) {
    padding-top: 16px;
  }
}

// 动画关键帧
@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

// 深色模式适配
[data-theme='dark'] .theme-settings {
  .preview-section {
    background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  }

  .mode-buttons .mode-btn {
    background: #334155;

    &.active {
      background: rgba(59, 130, 246, 0.2);
    }
  }
}
</style>
