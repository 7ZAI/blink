<!-- src/components/ThemeSettings/index.vue -->

<template>
  <div class="theme-settings">
    <!-- Preset Themes Section -->
    <section v-if="showPresets" class="settings-section">
      <PresetSelector
        :presets="presetThemes"
        :current-preset-id="currentPresetId"
        @select="handlePresetSelect"
      >
        <template v-if="$slots.presetFooter" #footer>
          <slot name="presetFooter" />
        </template>
      </PresetSelector>
    </section>

    <!-- Color Settings Section -->
    <section v-if="showColors" class="settings-section">
      <ColorSettings
        :colors="localConfig.colors"
        @change="handleColorChange"
      />
      <div v-if="$slots.colorFooter" class="section-footer">
        <slot name="colorFooter" />
      </div>
    </section>

    <!-- Font Settings Section -->
    <section v-if="showFonts" class="settings-section">
      <FontSettings
        :fonts="presetFonts"
        :font="localConfig.font"
        @change="handleFontChange"
      />
      <div v-if="$slots.fontFooter" class="section-footer">
        <slot name="fontFooter" />
      </div>
    </section>

    <!-- Animation Settings Section -->
    <section v-if="showAnimations" class="settings-section">
      <div class="animation-settings">
        <h4 class="section-title">{{ t('settings.animationSettings') }}</h4>
        <div class="animation-switch-wrapper">
          <el-switch
            v-model="localConfig.animationsEnabled"
            :disabled="readonly"
            @change="handleAnimationChange"
          />
          <span class="animation-label">
            {{ t('settings.enableAnimations') }}
          </span>
          <span class="animation-status">
            {{
              localConfig.animationsEnabled
                ? t('settings.animationsEnabled')
                : t('settings.animationsDisabled')
            }}
          </span>
        </div>
      </div>
      <div v-if="$slots.animationFooter" class="section-footer">
        <slot name="animationFooter" />
      </div>
    </section>

    <!-- System Settings Section -->
    <section v-if="showSystem" class="settings-section">
      <SystemSettings
        :config="localConfig.system"
        @change="handleSystemChange"
      />
      <div v-if="$slots.systemFooter" class="section-footer">
        <slot name="systemFooter" />
      </div>
    </section>

    <!-- Action Buttons -->
    <section class="settings-section action-buttons">
      <div class="button-group">
        <el-button
          type="primary"
          :disabled="readonly || isMaxPresetsReached"
          @click="openSaveDialog"
        >
          {{ t('settings.saveAsPreset') }}
        </el-button>
        <el-button
          :disabled="readonly"
          @click="handleReset"
        >
          {{ t('settings.resetToDefault') }}
        </el-button>
      </div>
      <p v-if="isMaxPresetsReached" class="max-presets-warning">
        {{ t('settings.maxPresetsReached', { max: maxCustomPresets }) }}
      </p>
    </section>

    <!-- Custom Presets List -->
    <section v-if="customPresets.length > 0" class="settings-section">
      <CustomPresetList
        :presets="customPresets"
        :current-preset-id="currentPresetId"
        @select="handleCustomPresetSelect"
        @delete="handlePresetDelete"
      />
    </section>

    <!-- Save Preset Dialog -->
    <el-dialog
      v-model="saveDialogVisible"
      :title="t('settings.saveAsPreset')"
      width="400px"
      :close-on-click-modal="false"
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
          />
        </el-form-item>
        <el-form-item :label="t('settings.presetName') + ' (EN)'">
          <el-input
            v-model="presetForm.nameEn"
            placeholder="Enter preset name in English"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button type="primary" @click="handleSavePreset">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PresetSelector from './components/PresetSelector.vue'
import ColorSettings from './components/ColorSettings.vue'
import FontSettings from './components/FontSettings.vue'
import SystemSettings from './components/SystemSettings.vue'
import CustomPresetList from './components/CustomPresetList.vue'
import type {
  ThemeSettingsProps,
  FullThemeConfig,
  ThemeColors,
  SystemConfig,
  CustomPreset,
  FontOption,
  PresetTheme,
} from './types'
import {
  PRESET_THEMES,
  PRESET_FONTS,
  MAX_CUSTOM_PRESETS,
  DEFAULT_THEME_CONFIG,
  DEFAULT_SYSTEM_CONFIG,
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
  showSystem: true,
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
    system: DEFAULT_SYSTEM_CONFIG,
  }),
  customPresets: () => [],
  readonly: false,
})

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: FullThemeConfig): void
  (e: 'preset-change', presetId: string): void
  (e: 'color-change', colors: ThemeColors): void
  (e: 'font-change', font: { family: string; baseSize: number; largeSize: number; smallSize: number }): void
  (e: 'animation-change', enabled: boolean): void
  (e: 'system-change', config: SystemConfig): void
  (e: 'preset-save', preset: CustomPreset): void
  (e: 'preset-delete', presetId: string): void
}>()

const { t, locale } = useI18n()

// Local reactive copy of config
const localConfig = reactive<FullThemeConfig>({
  presetId: props.modelValue.presetId,
  colors: { ...props.modelValue.colors },
  font: { ...props.modelValue.font },
  animationsEnabled: props.modelValue.animationsEnabled,
  system: { ...props.modelValue.system },
})

// Current preset ID (can be preset or custom)
const currentPresetId = computed(() => localConfig.presetId)

// Check if max presets reached
const isMaxPresetsReached = computed(
  () => props.customPresets.length >= props.maxCustomPresets
)

// Save dialog state
const saveDialogVisible = ref(false)
const presetFormRef = ref<FormInstance>()
const presetForm = reactive({
  name: '',
  nameEn: '',
})

// Form validation rules (computed for i18n reactivity)
const presetFormRules = computed<FormRules>(() => ({
  name: [
    {
      required: true,
      message: t('settings.presetNamePlaceholder'),
      trigger: 'blur',
    },
    {
      min: 2,
      max: 50,
      message: 'Name must be 2-50 characters',
      trigger: 'blur',
    },
  ],
}))

// Deep watch props.modelValue for external changes
watch(
  () => props.modelValue,
  (newValue) => {
    localConfig.presetId = newValue.presetId
    localConfig.colors = { ...newValue.colors }
    localConfig.font = { ...newValue.font }
    localConfig.animationsEnabled = newValue.animationsEnabled
    localConfig.system = { ...newValue.system }
  },
  { deep: true }
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
      system: { ...newValue.system },
    })
  },
  { deep: true }
)

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
  // Clear presetId when colors are manually changed
  localConfig.presetId = undefined
  localConfig.colors = { ...colors }
  emit('color-change', colors)
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
const handleAnimationChange = (enabled: boolean) => {
  localConfig.animationsEnabled = enabled
  emit('animation-change', enabled)
}

/**
 * Handle system config change
 */
const handleSystemChange = (config: SystemConfig) => {
  localConfig.system = { ...config }
  emit('system-change', config)
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
    await presetFormRef.value.validate()

    // Generate unique ID
    const presetId = `custom-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

    // Create custom preset
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

    // Set current presetId to the newly saved preset
    localConfig.presetId = presetId
  } catch {
    // Validation failed, do nothing
  }
}

/**
 * Handle preset delete
 * Note: Success/failure message should be handled by parent component
 */
const handlePresetDelete = (presetId: string) => {
  emit('preset-delete', presetId)

  // If deleted preset is current, clear presetId
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
    localConfig.system = { ...DEFAULT_SYSTEM_CONFIG }
    ElMessage.success(t('settings.themeReset'))
  }
}
</script>

<style scoped lang="scss">
.theme-settings {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 20px;
  background-color: var(--bg-color-overlay);
  border-radius: 8px;

  .settings-section {
    padding-bottom: 24px;
    border-bottom: 1px solid var(--border-color-light);

    &:last-of-type {
      padding-bottom: 0;
      border-bottom: none;
    }
  }

  .section-footer {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed var(--border-color-lighter);
  }

  .animation-settings {
    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color-primary);
      margin-bottom: 16px;
    }

    .animation-switch-wrapper {
      display: flex;
      align-items: center;
      gap: 16px;

      .animation-label {
        font-size: 14px;
        color: var(--text-color-regular);
      }

      .animation-status {
        font-size: 12px;
        color: var(--text-color-secondary);
        padding: 4px 8px;
        border-radius: 4px;
        background-color: var(--bg-color-page);
      }
    }
  }

  .action-buttons {
    .button-group {
      display: flex;
      gap: 12px;
    }

    .max-presets-warning {
      margin-top: 8px;
      font-size: 12px;
      color: var(--text-color-secondary);
    }
  }

  // Dark mode adjustments
  @media (prefers-color-scheme: dark) {
    background-color: var(--bg-color-overlay);

    .settings-section {
      border-bottom-color: var(--border-color-light);
    }

    .section-footer {
      border-top-color: var(--border-color-lighter);
    }

    .animation-settings {
      .section-title {
        color: var(--text-color-primary);
      }

      .animation-status {
        background-color: var(--bg-color-page);
      }
    }
  }
}
</style>