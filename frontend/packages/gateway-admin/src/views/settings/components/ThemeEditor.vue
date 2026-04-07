<template>
  <div class="theme-editor">
    <!-- 预设主题选择 -->
    <div class="section">
      <h4 class="section-title">{{ t('settings.presetThemes') }}</h4>
      <div class="preset-grid">
        <div
          v-for="preset in PRESET_THEMES"
          :key="preset.id"
          class="preset-card"
          :class="{ active: themeStore.currentPresetId === preset.id }"
          @click="handleApplyPreset(preset.id)"
        >
          <div
            class="preset-color"
            :style="{ backgroundColor: preset.colors.primary }"
          ></div>
          <span class="preset-name">{{ locale === 'zh_cn' ? preset.name : preset.nameEn }}</span>
        </div>
      </div>
    </div>

    <el-divider />

    <!-- 颜色设置 -->
    <div class="section">
      <h4 class="section-title">{{ t('settings.colorSettings') }}</h4>
      <el-form label-width="100px" class="color-form">
        <el-form-item :label="t('settings.primaryColor')">
          <div class="color-picker-wrapper">
            <el-color-picker
              v-model="localColors.primary"
              @change="handleColorChange"
            />
            <el-input
              v-model="localColors.primary"
              class="color-input"
              @change="handleColorChange"
            />
          </div>
        </el-form-item>
        <el-form-item :label="t('settings.successColor')">
          <div class="color-picker-wrapper">
            <el-color-picker
              v-model="localColors.success"
              @change="handleColorChange"
            />
            <el-input
              v-model="localColors.success"
              class="color-input"
              @change="handleColorChange"
            />
          </div>
        </el-form-item>
        <el-form-item :label="t('settings.warningColor')">
          <div class="color-picker-wrapper">
            <el-color-picker
              v-model="localColors.warning"
              @change="handleColorChange"
            />
            <el-input
              v-model="localColors.warning"
              class="color-input"
              @change="handleColorChange"
            />
          </div>
        </el-form-item>
        <el-form-item :label="t('settings.dangerColor')">
          <div class="color-picker-wrapper">
            <el-color-picker
              v-model="localColors.danger"
              @change="handleColorChange"
            />
            <el-input
              v-model="localColors.danger"
              class="color-input"
              @change="handleColorChange"
            />
          </div>
        </el-form-item>
        <el-form-item :label="t('settings.infoColor')">
          <div class="color-picker-wrapper">
            <el-color-picker
              v-model="localColors.info"
              @change="handleColorChange"
            />
            <el-input
              v-model="localColors.info"
              class="color-input"
              @change="handleColorChange"
            />
          </div>
        </el-form-item>
      </el-form>
    </div>

    <el-divider />

    <!-- 字体设置 -->
    <div class="section">
      <h4 class="section-title">{{ t('settings.fontSettings') }}</h4>
      <el-form label-width="100px" class="font-form">
        <el-form-item :label="t('settings.fontFamily')">
          <el-select v-model="localFont.family" @change="handleFontChange">
            <el-option
              v-for="font in PRESET_FONTS"
              :key="font.family"
              :label="font.label"
              :value="font.family"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.baseFontSize')">
          <el-slider
            v-model="localFont.baseSize"
            :min="FONT_SIZE_CONFIG.base.min"
            :max="FONT_SIZE_CONFIG.base.max"
            :step="1"
            show-input
            @change="handleFontSizeChange"
          />
        </el-form-item>
        <el-form-item :label="t('settings.largeFontSize')">
          <el-slider
            v-model="localFont.largeSize"
            :min="FONT_SIZE_CONFIG.large.min"
            :max="FONT_SIZE_CONFIG.large.max"
            :step="1"
            show-input
            @change="handleFontSizeChange"
          />
        </el-form-item>
        <el-form-item :label="t('settings.smallFontSize')">
          <el-slider
            v-model="localFont.smallSize"
            :min="FONT_SIZE_CONFIG.small.min"
            :max="FONT_SIZE_CONFIG.small.max"
            :step="1"
            show-input
            @change="handleFontSizeChange"
          />
        </el-form-item>
        <el-form-item :label="t('settings.fontPreview')">
          <div
            class="font-preview"
            :style="{
              fontFamily: localFont.family,
              fontSize: `${localFont.baseSize}px`,
            }"
          >
            {{ t('settings.fontPreviewText') }}
          </div>
        </el-form-item>
      </el-form>
    </div>

    <el-divider />

    <!-- 动画设置 -->
    <div class="section">
      <h4 class="section-title">{{ t('settings.animationSettings') }}</h4>
      <div class="animation-row">
        <span class="animation-label">{{ t('settings.enableAnimations') }}</span>
        <el-switch
          v-model="localAnimationsEnabled"
          @change="handleAnimationChange"
        />
        <span class="animation-hint">
          {{ localAnimationsEnabled ? t('settings.animationsEnabled') : t('settings.animationsDisabled') }}
        </span>
      </div>
    </div>

    <el-divider />

    <!-- 操作按钮 -->
    <div class="section actions">
      <el-button type="primary" @click="handleSaveAsPreset">
        {{ t('settings.saveAsPreset') }}
      </el-button>
      <el-button @click="handleReset">
        {{ t('settings.resetToDefault') }}
      </el-button>
    </div>

    <!-- 我的预设 -->
    <div v-if="themeStore.customPresets.length > 0" class="section">
      <el-divider />
      <h4 class="section-title">{{ t('settings.myPresets') }}</h4>
      <div class="custom-presets">
        <div
          v-for="preset in themeStore.customPresets"
          :key="preset.id"
          class="custom-preset-card"
          :class="{ active: themeStore.currentPresetId === preset.id }"
          @click="handleApplyPreset(preset.id)"
        >
          <div class="preset-header">
            <span class="preset-name">{{ preset.name }}</span>
            <el-button
              type="danger"
              text
              size="small"
              @click.stop="handleDeletePreset(preset)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <div class="preset-colors">
            <span
              v-for="(color, key) in preset.colors"
              :key="key"
              class="color-dot"
              :style="{ backgroundColor: color }"
            ></span>
          </div>
          <div class="preset-date">
            {{ t('settings.created') }}: {{ formatDate(preset.createdAt) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 保存预设对话框 -->
    <el-dialog
      v-model="showSaveDialog"
      :title="t('settings.saveAsPreset')"
      width="400px"
    >
      <el-form>
        <el-form-item :label="t('settings.presetName')">
          <el-input
            v-model="newPresetName"
            :placeholder="t('settings.presetNamePlaceholder')"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSaveDialog = false">
          {{ t('common.cancel') }}
        </el-button>
        <el-button type="primary" @click="confirmSavePreset">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useThemeStore, STORAGE_KEYS } from '@/stores/theme'
import { type ThemeColors, PRESET_THEMES, PRESET_FONTS, FONT_SIZE_CONFIG, MAX_CUSTOM_PRESETS } from '@/config/themes'

const { t, locale } = useI18n()
const themeStore = useThemeStore()

// 本地颜色状态
const localColors = reactive<ThemeColors>({
  primary: themeStore.colors.primary,
  success: themeStore.colors.success,
  warning: themeStore.colors.warning,
  danger: themeStore.colors.danger,
  info: themeStore.colors.info,
})

// 本地字体状态
const localFont = reactive({
  family: themeStore.font.family,
  baseSize: themeStore.font.baseSize,
  largeSize: themeStore.font.largeSize,
  smallSize: themeStore.font.smallSize,
})

// 本地动画状态
const localAnimationsEnabled = ref(themeStore.animationsEnabled)

// 保存对话框
const showSaveDialog = ref(false)
const newPresetName = ref('')

// 同步 store 状态到本地
watch(
  () => themeStore.colors,
  (newColors) => {
    Object.assign(localColors, newColors)
  },
  { deep: true }
)

watch(
  () => themeStore.font,
  (newFont) => {
    Object.assign(localFont, newFont)
  },
  { deep: true }
)

// 应用预设主题
function handleApplyPreset(presetId: string) {
  themeStore.applyPreset(presetId)
  ElMessage.success(t('message.operationSuccess'))
}

// 颜色变化处理
function handleColorChange() {
  // 验证颜色格式
  const hexPattern = /^#[0-9A-Fa-f]{6}$/
  const colors = { ...localColors }
  for (const [key, value] of Object.entries(colors)) {
    if (!hexPattern.test(value)) {
      ElMessage.warning(t('common.pleaseInput'))
      return
    }
  }
  themeStore.setColors(colors)
  themeStore.currentPresetId = null
  localStorage.removeItem(STORAGE_KEYS.CURRENT_PRESET_ID)
}

// 字体变化处理
function handleFontChange() {
  themeStore.setFont({ family: localFont.family })
}

// 字号变化处理
function handleFontSizeChange() {
  themeStore.setFont({
    family: localFont.family,
    baseSize: localFont.baseSize,
    largeSize: localFont.largeSize,
    smallSize: localFont.smallSize,
  })
}

// 动画开关变化处理
function handleAnimationChange(value: boolean) {
  themeStore.setAnimationsEnabled(value)
  ElMessage.success(t('message.operationSuccess'))
}

// 保存为预设
function handleSaveAsPreset() {
  newPresetName.value = ''
  showSaveDialog.value = true
}

// 确认保存预设
function confirmSavePreset() {
  if (!newPresetName.value.trim()) {
    ElMessage.warning(t('settings.presetNamePlaceholder'))
    return
  }

  const success = themeStore.saveAsPreset(newPresetName.value.trim())
  if (success) {
    ElMessage.success(t('settings.presetSaved'))
    showSaveDialog.value = false
  } else {
    ElMessage.warning(t('settings.maxPresetsReached', { max: MAX_CUSTOM_PRESETS }))
  }
}

// 删除预设
async function handleDeletePreset(preset: { id: string; name: string }) {
  try {
    await ElMessageBox.confirm(
      t('settings.deletePresetConfirm', { name: preset.name }),
      t('message.tips'),
      { type: 'warning' }
    )
    themeStore.deletePreset(preset.id)
    ElMessage.success(t('settings.presetDeleted'))
  } catch {
    // 用户取消
  }
}

// 重置为默认
function handleReset() {
  themeStore.resetToDefault()
  ElMessage.success(t('settings.themeReset'))
}

// 格式化日期
function formatDate(timestamp: number): string {
  return new Date(timestamp).toLocaleDateString()
}

// 初始化
onMounted(() => {
  themeStore.initTheme()
})
</script>

<style scoped lang="scss">
.theme-editor {
  .section {
    margin-bottom: 24px;

    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color-primary);
      margin-bottom: 16px;
    }
  }

  .preset-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 12px;

    .preset-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px;
      border: 2px solid var(--border-color-light);
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        border-color: var(--primary-color);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }

      &.active {
        border-color: var(--primary-color);
        background-color: rgba(var(--primary-color-rgb), 0.1);
      }

      .preset-color {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        margin-bottom: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      }

      .preset-name {
        font-size: 12px;
        color: var(--text-color-regular);
      }
    }
  }

  .color-form {
    max-width: 500px;

    .color-picker-wrapper {
      display: flex;
      align-items: center;
      gap: 12px;

      .color-input {
        width: 120px;
      }
    }
  }

  .font-form {
    max-width: 500px;

    .font-preview {
      padding: 12px 16px;
      background-color: var(--bg-color-page);
      border-radius: 8px;
      border: 1px solid var(--border-color-light);
      min-height: 60px;
      line-height: 1.6;
    }
  }

  .animation-form {
    max-width: 500px;
  }

  .animation-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .animation-label {
      font-size: 14px;
      color: var(--text-color-regular);
      min-width: 90px;
    }

    .animation-hint {
      font-size: 12px;
      color: var(--text-color-secondary);
      white-space: nowrap;
    }
  }

  .actions {
    display: flex;
    gap: 12px;
  }

  .custom-presets {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;

    .custom-preset-card {
      padding: 12px;
      border: 2px solid var(--border-color-light);
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        border-color: var(--primary-color);
      }

      &.active {
        border-color: var(--primary-color);
        background-color: rgba(var(--primary-color-rgb), 0.1);
      }

      .preset-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .preset-name {
          font-weight: 500;
          color: var(--text-color-primary);
        }
      }

      .preset-colors {
        display: flex;
        gap: 4px;
        margin-bottom: 8px;

        .color-dot {
          width: 16px;
          height: 16px;
          border-radius: 50%;
          border: 1px solid var(--border-color-light);
        }
      }

      .preset-date {
        font-size: 12px;
        color: var(--text-color-secondary);
      }
    }
  }
}
</style>