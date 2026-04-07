# ThemeSettings 组件实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建完整的主题设置组件，支持预设主题、颜色、字体、动画、系统配置（圆角/阴影/紧凑模式）及自定义预设管理。

**Architecture:** 主组件组合多个子组件，通过 v-model 与外部状态双向绑定，通过 props 控制功能模块显示。组件与 useThemeStore 解耦，可独立使用。

**Tech Stack:** Vue 3 + TypeScript + Element Plus

---

## 文件结构

```
src/components/ThemeSettings/
├── index.vue              # 主组件
├── components/
│   ├── PresetSelector.vue     # 预设主题选择器
│   ├── ColorSettings.vue      # 颜色配置面板
│   ├── FontSettings.vue       # 字体配置面板
│   ├── SystemSettings.vue     # 系统配置面板
│   └── CustomPresetList.vue   # 自定义预设列表
└── types.ts               # 类型定义
```

---

### Task 1: 扩展主题配置类型

**Files:**
- Modify: `src/config/themes.ts`

- [ ] **Step 1: 扩展 themes.ts，添加系统配置类型和默认值**

在 `src/config/themes.ts` 文件末尾添加：

```typescript
/**
 * 系统级配置
 */
export interface SystemConfig {
  /** 全局圆角 (0-24px) */
  borderRadius: number
  /** 阴影强度 */
  shadowIntensity: 'none' | 'light' | 'medium' | 'strong'
  /** 紧凑模式 */
  compactMode: boolean
  /** 内容宽度 */
  contentWidth: 'fluid' | 'fixed'
}

/**
 * 完整主题配置（用于 v-model）
 */
export interface FullThemeConfig {
  presetId?: string
  colors: ThemeColors
  font: {
    family: string
    baseSize: number
    largeSize: number
    smallSize: number
  }
  animationsEnabled: boolean
  system: SystemConfig
}

/**
 * 默认系统配置
 */
export const DEFAULT_SYSTEM_CONFIG: SystemConfig = {
  borderRadius: 8,
  shadowIntensity: 'medium',
  compactMode: false,
  contentWidth: 'fluid',
}

/**
 * 字体选项（用于下拉选择）
 */
export interface FontOption {
  family: string
  label: string
}

/**
 * 预设主题（包含颜色）
 */
export interface PresetTheme {
  id: string
  name: string
  nameEn: string
  colors: ThemeColors
  isPreset: boolean
}

/**
 * 自定义预设（包含创建时间）
 */
export interface CustomPreset extends PresetTheme {
  createdAt: number
}
```

- [ ] **Step 2: 提交类型扩展**

```bash
git add src/config/themes.ts
git commit -m "feat(themes): add SystemConfig and FullThemeConfig types

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 创建组件类型定义

**Files:**
- Create: `src/components/ThemeSettings/types.ts`

- [ ] **Step 1: 创建 types.ts 文件**

```typescript
// src/components/ThemeSettings/types.ts

import type {
  ThemeColors,
  SystemConfig,
  FullThemeConfig,
  PresetTheme,
  CustomPreset,
  FontOption,
} from '@/config/themes'

/**
 * ThemeSettings Props
 */
export interface ThemeSettingsProps {
  // 功能模块开关
  showPresets?: boolean
  showColors?: boolean
  showFonts?: boolean
  showAnimations?: boolean
  showSystem?: boolean

  // 配置项
  presetThemes?: PresetTheme[]
  presetFonts?: FontOption[]
  maxCustomPresets?: number

  // 状态控制
  modelValue?: FullThemeConfig
  customPresets?: CustomPreset[]
  readonly?: boolean
}

/**
 * ThemeSettings Emits
 */
export interface ThemeSettingsEmits {
  (e: 'update:modelValue', value: FullThemeConfig): void
  (e: 'preset-change', presetId: string): void
  (e: 'color-change', colors: ThemeColors): void
  (e: 'font-change', font: { family: string; baseSize: number; largeSize: number; smallSize: number }): void
  (e: 'animation-change', enabled: boolean): void
  (e: 'system-change', config: SystemConfig): void
  (e: 'preset-save', preset: CustomPreset): void
  (e: 'preset-delete', presetId: string): void
}

// 重导出类型
export type {
  ThemeColors,
  SystemConfig,
  FullThemeConfig,
  PresetTheme,
  CustomPreset,
  FontOption,
}
```

- [ ] **Step 2: 提交类型定义**

```bash
git add src/components/ThemeSettings/types.ts
git commit -m "feat(ThemeSettings): add component type definitions

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 创建预设主题选择器子组件

**Files:**
- Create: `src/components/ThemeSettings/components/PresetSelector.vue`

- [ ] **Step 1: 创建 PresetSelector.vue**

```vue
<!-- src/components/ThemeSettings/components/PresetSelector.vue -->
<template>
  <div class="preset-selector">
    <h4 class="section-title">{{ t('settings.presetThemes') }}</h4>
    <div class="preset-grid">
      <div
        v-for="preset in presets"
        :key="preset.id"
        class="preset-card"
        :class="{ active: currentPresetId === preset.id }"
        @click="handleSelect(preset.id)"
      >
        <div
          class="preset-color"
          :style="{ backgroundColor: preset.colors.primary }"
        />
        <span class="preset-name">{{ locale === 'zh_cn' ? preset.name : preset.nameEn }}</span>
      </div>
    </div>
    <div v-if="$slots.footer" class="preset-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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
</script>

<style scoped lang="scss">
.preset-selector {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 16px;
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
      }

      &.active {
        border-color: var(--primary-color);
        background: var(--primary-color-light-9);
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

  .preset-footer {
    margin-top: 12px;
  }
}
</style>
```

- [ ] **Step 2: 提交子组件**

```bash
git add src/components/ThemeSettings/components/PresetSelector.vue
git commit -m "feat(ThemeSettings): add PresetSelector component

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 创建颜色配置子组件

**Files:**
- Create: `src/components/ThemeSettings/components/ColorSettings.vue`

- [ ] **Step 1: 创建 ColorSettings.vue**

```vue
<!-- src/components/ThemeSettings/components/ColorSettings.vue -->
<template>
  <div class="color-settings">
    <h4 class="section-title">{{ t('settings.colorSettings') }}</h4>
    <el-form label-width="100px" class="color-form">
      <el-form-item :label="t('settings.primaryColor')">
        <div class="color-picker-wrapper">
          <el-color-picker v-model="localColors.primary" @change="handleChange" />
          <el-input v-model="localColors.primary" class="color-input" @change="handleChange" />
        </div>
      </el-form-item>
      <el-form-item :label="t('settings.successColor')">
        <div class="color-picker-wrapper">
          <el-color-picker v-model="localColors.success" @change="handleChange" />
          <el-input v-model="localColors.success" class="color-input" @change="handleChange" />
        </div>
      </el-form-item>
      <el-form-item :label="t('settings.warningColor')">
        <div class="color-picker-wrapper">
          <el-color-picker v-model="localColors.warning" @change="handleChange" />
          <el-input v-model="localColors.warning" class="color-input" @change="handleChange" />
        </div>
      </el-form-item>
      <el-form-item :label="t('settings.dangerColor')">
        <div class="color-picker-wrapper">
          <el-color-picker v-model="localColors.danger" @change="handleChange" />
          <el-input v-model="localColors.danger" class="color-input" @change="handleChange" />
        </div>
      </el-form-item>
      <el-form-item :label="t('settings.infoColor')">
        <div class="color-picker-wrapper">
          <el-color-picker v-model="localColors.info" @change="handleChange" />
          <el-input v-model="localColors.info" class="color-input" @change="handleChange" />
        </div>
      </el-form-item>
    </el-form>
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

const localColors = reactive<ThemeColors>({ ...props.colors })

watch(
  () => props.colors,
  (newColors) => {
    Object.assign(localColors, newColors)
  },
  { deep: true }
)

const handleChange = () => {
  emit('change', { ...localColors })
}
</script>

<style scoped lang="scss">
.color-settings {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 16px;
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
}
</style>
```

- [ ] **Step 2: 提交子组件**

```bash
git add src/components/ThemeSettings/components/ColorSettings.vue
git commit -m "feat(ThemeSettings): add ColorSettings component

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: 创建字体配置子组件

**Files:**
- Create: `src/components/ThemeSettings/components/FontSettings.vue`

- [ ] **Step 1: 创建 FontSettings.vue**

```vue
<!-- src/components/ThemeSettings/components/FontSettings.vue -->
<template>
  <div class="font-settings">
    <h4 class="section-title">{{ t('settings.fontSettings') }}</h4>
    <el-form label-width="100px" class="font-form">
      <el-form-item :label="t('settings.fontFamily')">
        <el-select v-model="localFont.family" @change="handleChange">
          <el-option
            v-for="font in fonts"
            :key="font.family"
            :label="font.label"
            :value="font.family"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.baseFontSize')">
        <el-slider
          v-model="localFont.baseSize"
          :min="12"
          :max="18"
          :step="1"
          show-input
          @change="handleChange"
        />
      </el-form-item>
      <el-form-item :label="t('settings.largeFontSize')">
        <el-slider
          v-model="localFont.largeSize"
          :min="16"
          :max="24"
          :step="1"
          show-input
          @change="handleChange"
        />
      </el-form-item>
      <el-form-item :label="t('settings.smallFontSize')">
        <el-slider
          v-model="localFont.smallSize"
          :min="10"
          :max="14"
          :step="1"
          show-input
          @change="handleChange"
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
          这是一段示例文字 ABC abc 123 这是一段示例文字
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FontOption } from '../types'

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
  font: FontConfig
  fonts: FontOption[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'change', font: FontConfig): void
}>()

const { t } = useI18n()

const localFont = reactive<FontConfig>({ ...props.font })

watch(
  () => props.font,
  (newFont) => {
    Object.assign(localFont, newFont)
  },
  { deep: true }
)

const handleChange = () => {
  emit('change', { ...localFont })
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
    max-width: 500px;

    .font-preview {
      padding: 12px 16px;
      background: var(--bg-color-page);
      border-radius: 8px;
      border: 1px solid var(--border-color-light);
      min-height: 60px;
      line-height: 1.6;
    }
  }
}
</style>
```

- [ ] **Step 2: 提交子组件**

```bash
git add src/components/ThemeSettings/components/FontSettings.vue
git commit -m "feat(ThemeSettings): add FontSettings component

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: 创建系统配置子组件

**Files:**
- Create: `src/components/ThemeSettings/components/SystemSettings.vue`

- [ ] **Step 1: 创建 SystemSettings.vue**

```vue
<!-- src/components/ThemeSettings/components/SystemSettings.vue -->
<template>
  <div class="system-settings">
    <h4 class="section-title">系统配置</h4>
    <el-form label-width="100px" class="system-form">
      <!-- 边框圆角 -->
      <el-form-item label="边框圆角">
        <el-slider
          v-model="localConfig.borderRadius"
          :min="0"
          :max="24"
          :step="2"
          show-input
          @change="handleChange"
        />
      </el-form-item>

      <!-- 阴影强度 -->
      <el-form-item label="阴影强度">
        <el-radio-group v-model="localConfig.shadowIntensity" @change="handleChange">
          <el-radio value="none">无</el-radio>
          <el-radio value="light">轻</el-radio>
          <el-radio value="medium">中</el-radio>
          <el-radio value="strong">强</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 紧凑模式 -->
      <el-form-item label="紧凑模式">
        <el-switch v-model="localConfig.compactMode" @change="handleChange" />
      </el-form-item>

      <!-- 内容宽度 -->
      <el-form-item label="内容宽度">
        <el-radio-group v-model="localConfig.contentWidth" @change="handleChange">
          <el-radio value="fluid">流式</el-radio>
          <el-radio value="fixed">固定</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
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

const localConfig = reactive<SystemConfig>({ ...props.config })

watch(
  () => props.config,
  (newConfig) => {
    Object.assign(localConfig, newConfig)
  },
  { deep: true }
)

const handleChange = () => {
  emit('change', { ...localConfig })
}
</script>

<style scoped lang="scss">
.system-settings {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 16px;
  }

  .system-form {
    max-width: 500px;
  }
}
</style>
```

- [ ] **Step 2: 提交子组件**

```bash
git add src/components/ThemeSettings/components/SystemSettings.vue
git commit -m "feat(ThemeSettings): add SystemSettings component

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: 创建自定义预设列表子组件

**Files:**
- Create: `src/components/ThemeSettings/components/CustomPresetList.vue`

- [ ] **Step 1: 创建 CustomPresetList.vue**

```vue
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
          <span class="preset-name">{{ preset.name }}</span>
          <el-button type="danger" text size="small" @click.stop="handleDelete(preset)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <div class="preset-colors">
          <span
            v-for="(color, key) in preset.colors"
            :key="key"
            class="color-dot"
            :style="{ backgroundColor: color }"
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

defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', presetId: string): void
  (e: 'delete', presetId: string): void
}>()

const { t } = useI18n()

const handleSelect = (presetId: string) => {
  emit('select', presetId)
}

const handleDelete = async (preset: CustomPreset) => {
  try {
    await ElMessageBox.confirm(
      t('settings.deletePresetConfirm', { name: preset.name }),
      t('message.tips'),
      { type: 'warning' }
    )
    emit('delete', preset.id)
  } catch {
    // 用户取消
  }
}

const formatDate = (timestamp: number): string => {
  return new Date(timestamp).toLocaleDateString()
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
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;

    .preset-card {
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
        background: var(--primary-color-light-9);
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
```

- [ ] **Step 2: 提交子组件**

```bash
git add src/components/ThemeSettings/components/CustomPresetList.vue
git commit -m "feat(ThemeSettings): add CustomPresetList component

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 8: 创建主组件

**Files:**
- Create: `src/components/ThemeSettings/index.vue`

- [ ] **Step 1: 创建主组件 index.vue**

```vue
<!-- src/components/ThemeSettings/index.vue -->
<template>
  <div class="theme-settings">
    <!-- 预设主题 -->
    <template v-if="showPresets">
      <PresetSelector
        :presets="presetThemes"
        :current-preset-id="config.presetId"
        @select="handlePresetSelect"
      >
        <template v-if="$slots['preset-footer']" #footer>
          <slot name="preset-footer" />
        </template>
      </PresetSelector>
      <el-divider />
    </template>

    <!-- 颜色设置 -->
    <template v-if="showColors">
      <ColorSettings :colors="config.colors" @change="handleColorChange" />
      <el-divider />
    </template>

    <!-- 字体设置 -->
    <template v-if="showFonts">
      <FontSettings :font="config.font" :fonts="presetFonts" @change="handleFontChange" />
      <el-divider />
    </template>

    <!-- 动画设置 -->
    <template v-if="showAnimations">
      <div class="animation-settings section">
        <h4 class="section-title">{{ t('settings.animationSettings') }}</h4>
        <div class="animation-row">
          <span class="animation-label">{{ t('settings.enableAnimations') }}</span>
          <el-switch v-model="localAnimationsEnabled" @change="handleAnimationChange" />
          <span class="animation-hint">
            {{ localAnimationsEnabled ? t('settings.animationsEnabled') : t('settings.animationsDisabled') }}
          </span>
        </div>
      </div>
      <el-divider />
    </template>

    <!-- 系统设置 -->
    <template v-if="showSystem">
      <SystemSettings :config="config.system" @change="handleSystemChange" />
      <el-divider />
    </template>

    <!-- 操作按钮 -->
    <div class="actions section">
      <el-button type="primary" @click="handleSaveAsPreset">
        {{ t('settings.saveAsPreset') }}
      </el-button>
      <el-button @click="handleReset">
        {{ t('settings.resetToDefault') }}
      </el-button>
    </div>

    <!-- 自定义预设列表 -->
    <CustomPresetList
      :presets="customPresets"
      :current-preset-id="config.presetId"
      @select="handlePresetSelect"
      @delete="handlePresetDelete"
    />

    <!-- 保存预设对话框 -->
    <el-dialog v-model="showSaveDialog" :title="t('settings.saveAsPreset')" width="400px">
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
        <el-button @click="showSaveDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="confirmSavePreset">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  PRESET_THEMES,
  PRESET_FONTS,
  DEFAULT_SYSTEM_CONFIG,
  FONT_SIZE_CONFIG,
  MAX_CUSTOM_PRESETS,
} from '@/config/themes'
import type { ThemeSettingsProps, ThemeSettingsEmits, FullThemeConfig, CustomPreset, ThemeColors, SystemConfig } from './types'

import PresetSelector from './components/PresetSelector.vue'
import ColorSettings from './components/ColorSettings.vue'
import FontSettings from './components/FontSettings.vue'
import SystemSettings from './components/SystemSettings.vue'
import CustomPresetList from './components/CustomPresetList.vue'

defineOptions({
  name: 'ThemeSettings',
})

const props = withDefaults(defineProps<ThemeSettingsProps>(), {
  showPresets: true,
  showColors: true,
  showFonts: true,
  showAnimations: true,
  showSystem: true,
  presetThemes: () => PRESET_THEMES,
  presetFonts: () => PRESET_FONTS,
  maxCustomPresets: 5,
  customPresets: () => [],
  readonly: false,
})

const emit = defineEmits<ThemeSettingsEmits>()

const { t } = useI18n()

// 默认配置
const defaultConfig: FullThemeConfig = {
  presetId: PRESET_THEMES[0]?.id,
  colors: { ...PRESET_THEMES[0]!.colors },
  font: {
    family: PRESET_FONTS[0]!.family,
    baseSize: FONT_SIZE_CONFIG.base.default,
    largeSize: FONT_SIZE_CONFIG.large.default,
    smallSize: FONT_SIZE_CONFIG.small.default,
  },
  animationsEnabled: true,
  system: { ...DEFAULT_SYSTEM_CONFIG },
}

// 本地配置状态
const config = ref<FullThemeConfig>(props.modelValue ? { ...props.modelValue } : { ...defaultConfig })
const localAnimationsEnabled = ref(config.value.animationsEnabled)

// 保存对话框
const showSaveDialog = ref(false)
const newPresetName = ref('')

// 监听外部配置变化
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal) {
      config.value = { ...newVal }
      localAnimationsEnabled.value = newVal.animationsEnabled
    }
  },
  { deep: true }
)

// 更新配置
const updateConfig = () => {
  emit('update:modelValue', { ...config.value })
}

// 预设选择
const handlePresetSelect = (presetId: string) => {
  const preset = PRESET_THEMES.find((p) => p.id === presetId) || props.customPresets.find((p) => p.id === presetId)
  if (preset) {
    config.value.presetId = presetId
    config.value.colors = { ...preset.colors }
    emit('preset-change', presetId)
    updateConfig()
  }
}

// 颜色变化
const handleColorChange = (colors: ThemeColors) => {
  config.value.colors = { ...colors }
  config.value.presetId = undefined
  emit('color-change', colors)
  updateConfig()
}

// 字体变化
const handleFontChange = (font: typeof config.value.font) => {
  config.value.font = { ...font }
  emit('font-change', font)
  updateConfig()
}

// 动画变化
const handleAnimationChange = (enabled: boolean) => {
  config.value.animationsEnabled = enabled
  emit('animation-change', enabled)
  updateConfig()
}

// 系统配置变化
const handleSystemChange = (system: SystemConfig) => {
  config.value.system = { ...system }
  emit('system-change', system)
  updateConfig()
}

// 保存为预设
const handleSaveAsPreset = () => {
  newPresetName.value = ''
  showSaveDialog.value = true
}

const confirmSavePreset = () => {
  if (!newPresetName.value.trim()) {
    ElMessage.warning(t('settings.presetNamePlaceholder'))
    return
  }

  if (props.customPresets.length >= props.maxCustomPresets) {
    ElMessage.warning(t('settings.maxPresetsReached', { max: props.maxCustomPresets }))
    return
  }

  const newPreset: CustomPreset = {
    id: `custom-${Date.now()}`,
    name: newPresetName.value.trim(),
    nameEn: newPresetName.value.trim(),
    colors: { ...config.value.colors },
    isPreset: false,
    createdAt: Date.now(),
  }

  config.value.presetId = newPreset.id
  emit('preset-save', newPreset)
  showSaveDialog.value = false
  ElMessage.success(t('settings.presetSaved'))
  updateConfig()
}

// 删除预设
const handlePresetDelete = (presetId: string) => {
  emit('preset-delete', presetId)
  if (config.value.presetId === presetId) {
    config.value.presetId = PRESET_THEMES[0]?.id
    config.value.colors = { ...PRESET_THEMES[0]!.colors }
  }
  ElMessage.success(t('settings.presetDeleted'))
}

// 重置
const handleReset = () => {
  config.value = { ...defaultConfig }
  localAnimationsEnabled.value = true
  emit('update:modelValue', { ...defaultConfig })
  ElMessage.success(t('settings.themeReset'))
}
</script>

<style scoped lang="scss">
.theme-settings {
  .section {
    margin-bottom: 24px;

    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color-primary);
      margin-bottom: 16px;
    }
  }

  .animation-settings {
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
  }

  .actions {
    display: flex;
    gap: 12px;
  }
}
</style>
```

- [ ] **Step 2: 提交主组件**

```bash
git add src/components/ThemeSettings/index.vue
git commit -m "feat(ThemeSettings): implement main component

- Support preset themes, colors, fonts, animations, system settings
- Support custom preset save/delete
- v-model binding for config
- Module visibility controlled by props

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 9: 更新组件导出和文档

**Files:**
- Modify: `src/components/index.ts`
- Modify: `docs/rules/frontend-rules.md`

- [ ] **Step 1: 更新组件导出**

```typescript
// src/components/index.ts
export { default as BlinkDialog } from './BlinkDialog/index.vue'
export * from './BlinkDialog/types'

export { default as BlinkTable } from './BlinkTable/index.vue'
export { default as BlinkTableColumn } from './BlinkTable/Column.vue'
export * from './BlinkTable/types'

export { default as ThemeSettings } from './ThemeSettings/index.vue'
export * from './ThemeSettings/types'
```

- [ ] **Step 2: 在 frontend-rules.md 末尾添加 ThemeSettings 使用规范**

```markdown
## 20. ThemeSettings 主题设置组件规范

### 20.1 基本用法

```vue
<template>
  <ThemeSettings
    v-model="themeConfig"
    :show-system="true"
    :custom-presets="customPresets"
    @preset-save="handleSavePreset"
    @preset-delete="handleDeletePreset"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ThemeSettings from '@/components/ThemeSettings/index.vue'

const themeConfig = ref<FullThemeConfig>({
  presetId: 'default-blue',
  colors: { primary: '#3b82f6', ... },
  font: { family: '...', baseSize: 14, ... },
  animationsEnabled: true,
  system: { borderRadius: 8, shadowIntensity: 'medium', ... }
})

const customPresets = ref<CustomPreset[]>([])
</script>
```

### 20.2 Props 配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modelValue | FullThemeConfig | - | v-model 绑定配置 |
| showPresets | boolean | true | 显示预设主题 |
| showColors | boolean | true | 显示颜色设置 |
| showFonts | boolean | true | 显示字体设置 |
| showAnimations | boolean | true | 显示动画设置 |
| showSystem | boolean | true | 显示系统配置 |
| presetThemes | PresetTheme[] | 内置预设 | 自定义预设主题列表 |
| presetFonts | FontOption[] | 内置字体 | 自定义字体选项 |
| maxCustomPresets | number | 5 | 最大自定义预设数量 |
| customPresets | CustomPreset[] | [] | 自定义预设列表 |

### 20.3 Events

| 事件 | 说明 |
|------|------|
| update:modelValue | 配置更新 |
| preset-change | 预设切换 |
| color-change | 颜色变更 |
| font-change | 字体变更 |
| animation-change | 动画开关 |
| system-change | 系统配置变更 |
| preset-save | 保存自定义预设 |
| preset-delete | 删除自定义预设 |
```

- [ ] **Step 3: 提交更新**

```bash
git add src/components/index.ts docs/rules/frontend-rules.md
git commit -m "feat: export ThemeSettings and add usage guide

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 10: 验证组件功能

- [ ] **Step 1: 验证各模块功能**

1. 预设主题选择正常
2. 颜色选择器正常
3. 字体设置正常
4. 动画开关正常
5. 系统配置正常
6. 自定义预设保存/删除正常

- [ ] **Step 2: 验证深色模式**

切换深色模式，检查组件样式适配

- [ ] **Step 3: 查看最终提交记录**

```bash
git log --oneline -15
```

---

## 完成标准

- [ ] 类型定义完整
- [ ] 5 个子组件正常工作
- [ ] 主组件组合正确
- [ ] v-model 双向绑定正常
- [ ] 自定义预设管理正常
- [ ] 深色模式适配
- [ ] 文档更新完成
- [ ] 代码已提交到 git