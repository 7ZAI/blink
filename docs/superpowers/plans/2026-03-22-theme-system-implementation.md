# 主题系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 blink-base-web 添加完整的主题定制能力，包括预设主题切换、自定义颜色/字体配置、用户自定义预设保存。

**Architecture:** 使用 CSS 变量动态切换主题，通过 Pinia Store 管理主题状态，Element Plus 组件自动响应变量变化。预设主题和用户自定义预设存储在 localStorage。

**Tech Stack:** Vue 3 + TypeScript + Pinia + Element Plus + CSS Variables + Google Fonts

---

## 文件结构

```
src/
├── config/
│   └── themes.ts              # 新建：预设主题配置
├── stores/
│   └── theme.ts               # 修改：扩展主题管理功能（保持向后兼容）
├── views/settings/
│   ├── index.vue              # 修改：集成主题编辑器
│   └── components/
│       └── ThemeEditor.vue    # 新建：主题编辑器组件
├── locales/
│   ├── zh-cn.ts               # 修改：添加主题相关中文
│   └── en-us.ts               # 修改：添加主题相关英文
└── utils/
    └── fontLoader.ts          # 新建：字体加载工具
```

**注意：** CSS 变量已在 `src/styles/index.scss` 中定义，主题组件使用的变量如 `--text-color-primary`、`--border-color-light` 等均已存在。

---

### Task 1: 创建预设主题配置文件

**Files:**
- Create: `src/config/themes.ts`

- [ ] **Step 1: 创建预设主题配置文件**

```typescript
// src/config/themes.ts

/**
 * 主题颜色配置
 */
export interface ThemeColors {
  primary: string
  success: string
  warning: string
  danger: string
  info: string
}

/**
 * 字体配置
 */
export interface ThemeFont {
  family: string
  label: string
  googleFontsName?: string // Google Fonts API 名称
}

/**
 * 完整主题配置
 */
export interface ThemeConfig {
  id: string
  name: string
  nameEn: string
  colors: ThemeColors
  isPreset: boolean
}

/**
 * 预设字体列表
 */
export const PRESET_FONTS: ThemeFont[] = [
  {
    family: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif',
    label: '系统默认',
    googleFontsName: undefined,
  },
  {
    family: '"Noto Sans SC", sans-serif',
    label: '思源黑体',
    googleFontsName: 'Noto+Sans+SC:wght@400;500;600;700',
  },
  {
    family: '"Noto Serif SC", serif',
    label: '思源宋体',
    googleFontsName: 'Noto+Serif+SC:wght@400;600;700',
  },
  {
    family: '"LXGW WenKai", cursive',
    label: '霞鹜文楷',
    googleFontsName: 'LXGW+WenKai:wght@400;700',
  },
  {
    family: '"JetBrains Mono", monospace',
    label: '等宽字体',
    googleFontsName: 'JetBrains+Mono:wght@400;500;600',
  },
]

/**
 * 预设主题列表
 */
export const PRESET_THEMES: ThemeConfig[] = [
  {
    id: 'default-blue',
    name: '默认蓝',
    nameEn: 'Default Blue',
    colors: {
      primary: '#3b82f6',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#6366f1',
    },
    isPreset: true,
  },
  {
    id: 'tech-purple',
    name: '科技紫',
    nameEn: 'Tech Purple',
    colors: {
      primary: '#8b5cf6',
      success: '#06b6d4',
      warning: '#fbbf24',
      danger: '#f43f5e',
      info: '#3b82f6',
    },
    isPreset: true,
  },
  {
    id: 'vibrant-orange',
    name: '活力橙',
    nameEn: 'Vibrant Orange',
    colors: {
      primary: '#f97316',
      success: '#22c55e',
      warning: '#fbbf24',
      danger: '#ef4444',
      info: '#0ea5e9',
    },
    isPreset: true,
  },
  {
    id: 'fresh-green',
    name: '清新绿',
    nameEn: 'Fresh Green',
    colors: {
      primary: '#10b981',
      success: '#06b6d4',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#8b5cf6',
    },
    isPreset: true,
  },
  {
    id: 'elegant-red',
    name: '优雅红',
    nameEn: 'Elegant Red',
    colors: {
      primary: '#ef4444',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#b91c1c',
      info: '#6366f1',
    },
    isPreset: true,
  },
  {
    id: 'cyber-cyan',
    name: '赛博朋克',
    nameEn: 'Cyberpunk',
    colors: {
      primary: '#06b6d4',
      success: '#10b981',
      warning: '#f97316',
      danger: '#f43f5e',
      info: '#8b5cf6',
    },
    isPreset: true,
  },
  {
    id: 'minimal-gray',
    name: '极简灰',
    nameEn: 'Minimal Gray',
    colors: {
      primary: '#64748b',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#6366f1',
    },
    isPreset: true,
  },
  {
    id: 'deep-night',
    name: '深邃夜',
    nameEn: 'Deep Night',
    colors: {
      primary: '#1e293b',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#6366f1',
    },
    isPreset: true,
  },
]

/**
 * 默认主题配置
 */
export const DEFAULT_THEME_CONFIG: ThemeConfig = PRESET_THEMES[0]

/**
 * 字号范围配置
 */
export const FONT_SIZE_CONFIG = {
  base: { min: 12, max: 18, default: 14 },
  large: { min: 16, max: 24, default: 18 },
  small: { min: 10, max: 14, default: 12 },
}

/**
 * 用户自定义预设限制
 */
export const MAX_CUSTOM_PRESETS = 10
```

- [ ] **Step 2: 提交代码**

```bash
git add src/config/themes.ts
git commit -m "feat(theme): 添加预设主题配置文件

- 8套预设主题方案
- 5种预设字体
- 字号范围配置

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 创建字体加载工具

**Files:**
- Create: `src/utils/fontLoader.ts`

- [ ] **Step 1: 创建字体加载工具**

```typescript
// src/utils/fontLoader.ts

/**
 * 已加载的字体集合
 */
const loadedFonts = new Set<string>()

/**
 * 加载 Google Fonts
 * @param googleFontsName Google Fonts API 名称，如 "Noto+Sans+SC:wght@400;500;600;700"
 */
export function loadGoogleFont(googleFontsName: string): Promise<void> {
  // 如果已经加载过，直接返回
  if (loadedFonts.has(googleFontsName)) {
    return Promise.resolve()
  }

  return new Promise((resolve, reject) => {
    // 检查是否已存在该 link 标签
    const existingLink = document.querySelector(
      `link[href*="${googleFontsName.split(':')[0]}"]`
    )
    if (existingLink) {
      loadedFonts.add(googleFontsName)
      resolve()
      return
    }

    const link = document.createElement('link')
    link.href = `https://fonts.googleapis.com/css2?family=${googleFontsName}&display=swap`
    link.rel = 'stylesheet'
    link.crossOrigin = 'anonymous'

    link.onload = () => {
      loadedFonts.add(googleFontsName)
      resolve()
    }

    link.onerror = () => {
      console.warn(`Failed to load font: ${googleFontsName}`)
      // 即使加载失败也 resolve，避免阻塞
      resolve()
    }

    document.head.appendChild(link)
  })
}

/**
 * 预加载所有预设字体
 */
export async function preloadFonts(
  fonts: Array<{ googleFontsName?: string }>
): Promise<void> {
  const loadPromises = fonts
    .filter((f) => f.googleFontsName)
    .map((f) => loadGoogleFont(f.googleFontsName!))

  await Promise.allSettled(loadPromises)
}

/**
 * 检查字体是否已加载
 */
export function isFontLoaded(fontName: string): boolean {
  return loadedFonts.has(fontName)
}
```

- [ ] **Step 2: 提交代码**

```bash
git add src/utils/fontLoader.ts
git commit -m "feat(theme): 添加字体加载工具

- 支持 Google Fonts 动态加载
- 防止重复加载
- 支持预加载所有字体

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 扩展主题 Store

**Files:**
- Modify: `src/stores/theme.ts`

- [ ] **Step 1: 扩展主题 Store**

```typescript
// src/stores/theme.ts
import { defineStore } from 'pinia'
import { ref, watch, computed } from 'vue'
import {
  type ThemeConfig,
  type ThemeColors,
  PRESET_THEMES,
  DEFAULT_THEME_CONFIG,
  PRESET_FONTS,
  FONT_SIZE_CONFIG,
  MAX_CUSTOM_PRESETS,
} from '@/config/themes'
import { loadGoogleFont } from '@/utils/fontLoader'

export type ThemeMode = 'light' | 'dark'

/**
 * 用户自定义主题（包含创建时间）
 */
export interface CustomThemePreset extends ThemeConfig {
  createdAt: number
}

/**
 * 当前应用的主题状态
 */
export interface AppliedTheme {
  colors: ThemeColors
  font: {
    family: string
    baseSize: number
    largeSize: number
    smallSize: number
  }
}

// LocalStorage keys
const STORAGE_KEYS = {
  THEME_MODE: 'theme',
  THEME_COLORS: 'themeColors',
  THEME_FONT: 'themeFont',
  CUSTOM_PRESETS: 'themeCustomPresets',
  CURRENT_PRESET_ID: 'themeCurrentPresetId',
}

/**
 * 颜色变浅
 */
function lighten(hex: string, amount: number): string {
  const num = parseInt(hex.replace('#', ''), 16)
  const r = Math.min(255, Math.floor((num >> 16) + (255 - (num >> 16)) * amount))
  const g = Math.min(255, Math.floor(((num >> 8) & 0x00ff) + (255 - ((num >> 8) & 0x00ff)) * amount))
  const b = Math.min(255, Math.floor((num & 0x0000ff) + (255 - (num & 0x0000ff)) * amount))
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`
}

/**
 * 颜色加深
 */
function darken(hex: string, amount: number): string {
  const num = parseInt(hex.replace('#', ''), 16)
  const r = Math.max(0, Math.floor((num >> 16) * (1 - amount)))
  const g = Math.max(0, Math.floor(((num >> 8) & 0x00ff) * (1 - amount)))
  const b = Math.max(0, Math.floor((num & 0x0000ff) * (1 - amount)))
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`
}

/**
 * 生成颜色派生变量
 */
function generateColorVars(color: string, name: string): Record<string, string> {
  return {
    [`--el-color-${name}`]: color,
    [`--el-color-${name}-light-3`]: lighten(color, 0.3),
    [`--el-color-${name}-light-5`]: lighten(color, 0.5),
    [`--el-color-${name}-light-7`]: lighten(color, 0.7),
    [`--el-color-${name}-light-8`]: lighten(color, 0.8),
    [`--el-color-${name}-light-9`]: lighten(color, 0.9),
    [`--el-color-${name}-dark-2`]: darken(color, 0.2),
  }
}

export const useThemeStore = defineStore('theme', () => {
  // ========== 状态 ==========

  // 深色/浅色模式
  const mode = ref<ThemeMode>((localStorage.getItem(STORAGE_KEYS.THEME_MODE) as ThemeMode) || 'light')

  // 当前主题颜色
  const colors = ref<ThemeColors>(loadColorsFromStorage())

  // 当前字体配置
  const font = ref(loadFontFromStorage())

  // 用户自定义预设列表
  const customPresets = ref<CustomThemePreset[]>(loadCustomPresetsFromStorage())

  // 当前选中的预设ID
  const currentPresetId = ref<string | null>(
    localStorage.getItem(STORAGE_KEYS.CURRENT_PRESET_ID) || null
  )

  // ========== 计算属性 ==========

  // 当前主题是否为预设主题
  const isPresetTheme = computed(() => {
    return PRESET_THEMES.some((t) => t.id === currentPresetId.value)
  })

  // ========== 初始化 ==========

  /**
   * 从 localStorage 加载颜色配置
   */
  function loadColorsFromStorage(): ThemeColors {
    const stored = localStorage.getItem(STORAGE_KEYS.THEME_COLORS)
    if (stored) {
      try {
        return JSON.parse(stored)
      } catch {
        // ignore
      }
    }
    return { ...DEFAULT_THEME_CONFIG.colors }
  }

  /**
   * 从 localStorage 加载字体配置
   */
  function loadFontFromStorage() {
    const stored = localStorage.getItem(STORAGE_KEYS.THEME_FONT)
    if (stored) {
      try {
        return JSON.parse(stored)
      } catch {
        // ignore
      }
    }
    return {
      family: PRESET_FONTS[0].family,
      baseSize: FONT_SIZE_CONFIG.base.default,
      largeSize: FONT_SIZE_CONFIG.large.default,
      smallSize: FONT_SIZE_CONFIG.small.default,
    }
  }

  /**
   * 从 localStorage 加载自定义预设
   */
  function loadCustomPresetsFromStorage(): CustomThemePreset[] {
    const stored = localStorage.getItem(STORAGE_KEYS.CUSTOM_PRESETS)
    if (stored) {
      try {
        return JSON.parse(stored)
      } catch {
        // ignore
      }
    }
    return []
  }

  // ========== Actions ==========

  /**
   * 设置主题模式（深色/浅色）
   */
  function setMode(newMode: ThemeMode) {
    mode.value = newMode
    localStorage.setItem(STORAGE_KEYS.THEME_MODE, newMode)
    applyMode(newMode)
  }

  /**
   * 切换主题模式
   */
  function toggleMode() {
    setMode(mode.value === 'light' ? 'dark' : 'light')
  }

  /**
   * 应用主题模式到 DOM
   */
  function applyMode(newMode: ThemeMode) {
    if (newMode === 'dark') {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  /**
   * 设置主题颜色
   */
  function setColors(newColors: ThemeColors) {
    colors.value = { ...newColors }
    localStorage.setItem(STORAGE_KEYS.THEME_COLORS, JSON.stringify(newColors))
    applyColors(newColors)
  }

  /**
   * 应用颜色到 DOM
   */
  function applyColors(newColors: ThemeColors) {
    const root = document.documentElement

    // 应用主色
    const primaryVars = generateColorVars(newColors.primary, 'primary')
    Object.entries(primaryVars).forEach(([key, value]) => {
      root.style.setProperty(key, value)
    })

    // 更新项目自定义变量
    root.style.setProperty('--primary-color', newColors.primary)
    root.style.setProperty('--primary-color-light', lighten(newColors.primary, 0.3))
    root.style.setProperty('--primary-color-dark', darken(newColors.primary, 0.2))
    root.style.setProperty('--primary-color-rgb', hexToRgb(newColors.primary))

    // 应用功能色
    const colorTypes: Array<keyof ThemeColors> = ['success', 'warning', 'danger', 'info']
    colorTypes.forEach((type) => {
      const vars = generateColorVars(newColors[type], type)
      Object.entries(vars).forEach(([key, value]) => {
        root.style.setProperty(key, value)
      })
    })
  }

  /**
   * HEX 转 RGB 字符串
   */
  function hexToRgb(hex: string): string {
    const num = parseInt(hex.replace('#', ''), 16)
    return `${(num >> 16) & 255}, ${(num >> 8) & 255}, ${num & 255}`
  }

  /**
   * 设置字体
   */
  async function setFont(newFont: {
    family: string
    baseSize?: number
    largeSize?: number
    smallSize?: number
  }) {
    // 查找字体配置
    const fontConfig = PRESET_FONTS.find((f) => f.family === newFont.family)

    // 如果是 Google Fonts，先加载字体
    if (fontConfig?.googleFontsName) {
      await loadGoogleFont(fontConfig.googleFontsName)
    }

    font.value = {
      family: newFont.family,
      baseSize: newFont.baseSize ?? font.value.baseSize,
      largeSize: newFont.largeSize ?? font.value.largeSize,
      smallSize: newFont.smallSize ?? font.value.smallSize,
    }

    localStorage.setItem(STORAGE_KEYS.THEME_FONT, JSON.stringify(font.value))
    applyFont(font.value)
  }

  /**
   * 应用字体到 DOM
   */
  function applyFont(newFont: {
    family: string
    baseSize: number
    largeSize: number
    smallSize: number
  }) {
    const root = document.documentElement

    root.style.setProperty('--el-font-family', newFont.family)
    root.style.setProperty('--font-family', newFont.family)
    root.style.setProperty('--el-font-size-base', `${newFont.baseSize}px`)
    root.style.setProperty('--font-size-base', `${newFont.baseSize}px`)
    root.style.setProperty('--el-font-size-large', `${newFont.largeSize}px`)
    root.style.setProperty('--font-size-large', `${newFont.largeSize}px`)
    root.style.setProperty('--el-font-size-small', `${newFont.smallSize}px`)
    root.style.setProperty('--font-size-small', `${newFont.smallSize}px`)
  }

  /**
   * 应用预设主题
   */
  function applyPreset(presetId: string) {
    // 先查找预设主题
    const preset = PRESET_THEMES.find((t) => t.id === presetId)
    if (preset) {
      setColors(preset.colors)
      currentPresetId.value = presetId
      localStorage.setItem(STORAGE_KEYS.CURRENT_PRESET_ID, presetId)
      return
    }

    // 查找自定义预设
    const customPreset = customPresets.value.find((t) => t.id === presetId)
    if (customPreset) {
      setColors(customPreset.colors)
      currentPresetId.value = presetId
      localStorage.setItem(STORAGE_KEYS.CURRENT_PRESET_ID, presetId)
    }
  }

  /**
   * 保存当前配置为自定义预设
   */
  function saveAsPreset(name: string): boolean {
    if (customPresets.value.length >= MAX_CUSTOM_PRESETS) {
      return false
    }

    const newPreset: CustomThemePreset = {
      id: `custom-${Date.now()}`,
      name,
      nameEn: name,
      colors: { ...colors.value },
      isPreset: false,
      createdAt: Date.now(),
    }

    customPresets.value.push(newPreset)
    localStorage.setItem(STORAGE_KEYS.CUSTOM_PRESETS, JSON.stringify(customPresets.value))
    currentPresetId.value = newPreset.id
    localStorage.setItem(STORAGE_KEYS.CURRENT_PRESET_ID, newPreset.id)

    return true
  }

  /**
   * 删除自定义预设
   */
  function deletePreset(presetId: string) {
    const index = customPresets.value.findIndex((t) => t.id === presetId)
    if (index !== -1) {
      customPresets.value.splice(index, 1)
      localStorage.setItem(STORAGE_KEYS.CUSTOM_PRESETS, JSON.stringify(customPresets.value))

      // 如果删除的是当前使用的预设，重置为默认
      if (currentPresetId.value === presetId) {
        applyPreset(DEFAULT_THEME_CONFIG.id)
      }
    }
  }

  /**
   * 重置为默认主题
   */
  function resetToDefault() {
    applyPreset(DEFAULT_THEME_CONFIG.id)
    setFont({
      family: PRESET_FONTS[0].family,
      baseSize: FONT_SIZE_CONFIG.base.default,
      largeSize: FONT_SIZE_CONFIG.large.default,
      smallSize: FONT_SIZE_CONFIG.small.default,
    })
  }

  /**
   * 初始化主题（应用存储的配置）
   */
  function initTheme() {
    applyMode(mode.value)
    applyColors(colors.value)
    applyFont(font.value)
  }

  // 监听模式变化
  watch(mode, (val) => {
    applyMode(val)
  })

  return {
    // 状态
    mode,
    colors,
    font,
    customPresets,
    currentPresetId,
    isPresetTheme,

    // Actions
    setMode,
    toggleMode,
    setColors,
    setFont,
    applyPreset,
    saveAsPreset,
    deletePreset,
    resetToDefault,
    initTheme,

    // 向后兼容别名（保持与现有代码的兼容性）
    theme: mode,
    setTheme: setMode,
  }
})

// 导出 Storage Keys 供其他组件使用
export { STORAGE_KEYS }
```

- [ ] **Step 2: 提交代码**

```bash
git add src/stores/theme.ts
git commit -m "feat(theme): 扩展主题 Store 功能

- 添加颜色配置管理
- 添加字体配置管理
- 支持用户自定义预设保存/删除
- CSS 变量动态生成和应用

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 添加国际化配置

**Files:**
- Modify: `src/locales/zh-cn.ts`
- Modify: `src/locales/en-us.ts`

- [ ] **Step 1: 添加中文国际化配置**

在 `src/locales/zh-cn.ts` 的 `settings` 对象中添加：

```typescript
settings: {
  // ... 现有配置保持不变 ...
  basicSettings: '基本设置',
  sidebarSettings: '侧边栏设置',
  theme: '主题',
  lightTheme: '浅色主题',
  darkTheme: '深色主题',
  language: '语言',
  sidebarWidth: '侧边栏宽度',
  chinese: '简体中文',
  to: '至',
  startDate: '开始日期',
  endDate: '结束日期',

  // 新增主题设置相关
  themeSettings: '主题设置',
  presetThemes: '预设主题',
  colorSettings: '颜色设置',
  fontSettings: '字体设置',
  primaryColor: '主题色',
  successColor: '成功色',
  warningColor: '警告色',
  dangerColor: '危险色',
  infoColor: '信息色',
  fontFamily: '字体',
  baseFontSize: '基础字号',
  largeFontSize: '大字号',
  smallFontSize: '小字号',
  saveAsPreset: '保存为预设',
  resetToDefault: '重置为默认',
  myPresets: '我的预设',
  presetName: '预设名称',
  presetNamePlaceholder: '请输入预设名称',
  maxPresetsReached: '最多只能保存 {max} 个自定义预设',
  presetSaved: '预设保存成功',
  presetDeleted: '预设已删除',
  themeReset: '主题已重置',
  fontPreview: '预览文字',
  deletePreset: '删除预设',
  deletePresetConfirm: '确定要删除预设「{name}」吗？',
  noCustomPresets: '暂无自定义预设',
  created: '创建于',
},
```

- [ ] **Step 2: 添加英文国际化配置**

在 `src/locales/en-us.ts` 的 `settings` 对象中添加：

```typescript
settings: {
  // ... existing config remains the same ...
  basicSettings: 'Basic Settings',
  sidebarSettings: 'Sidebar Settings',
  theme: 'Theme',
  lightTheme: 'Light Theme',
  darkTheme: 'Dark Theme',
  language: 'Language',
  sidebarWidth: 'Sidebar Width',
  chinese: 'Simplified Chinese',
  to: 'to',
  startDate: 'Start Date',
  endDate: 'End Date',

  // New theme settings
  themeSettings: 'Theme Settings',
  presetThemes: 'Preset Themes',
  colorSettings: 'Color Settings',
  fontSettings: 'Font Settings',
  primaryColor: 'Primary Color',
  successColor: 'Success Color',
  warningColor: 'Warning Color',
  dangerColor: 'Danger Color',
  infoColor: 'Info Color',
  fontFamily: 'Font Family',
  baseFontSize: 'Base Font Size',
  largeFontSize: 'Large Font Size',
  smallFontSize: 'Small Font Size',
  saveAsPreset: 'Save as Preset',
  resetToDefault: 'Reset to Default',
  myPresets: 'My Presets',
  presetName: 'Preset Name',
  presetNamePlaceholder: 'Enter preset name',
  maxPresetsReached: 'Maximum {max} custom presets allowed',
  presetSaved: 'Preset saved successfully',
  presetDeleted: 'Preset deleted',
  themeReset: 'Theme reset to default',
  fontPreview: 'Preview Text',
  deletePreset: 'Delete Preset',
  deletePresetConfirm: 'Are you sure you want to delete preset "{name}"?',
  noCustomPresets: 'No custom presets',
  created: 'Created',
},
```

- [ ] **Step 3: 提交代码**

```bash
git add src/locales/zh-cn.ts src/locales/en-us.ts
git commit -m "feat(theme): 添加主题设置国际化配置

- 中文和英文翻译
- 颜色、字体、预设相关文案

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: 创建主题编辑器组件

**Files:**
- Create: `src/views/settings/components/ThemeEditor.vue`

- [ ] **Step 1: 创建主题编辑器组件**

```vue
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
            这是一段示例文字 ABC abc 123 这是一段示例文字
          </div>
        </el-form-item>
      </el-form>
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
import { type ThemeColors, PRESET_THEMES, PRESET_FONTS, FONT_SIZE_CONFIG } from '@/config/themes'

const { t } = useI18n()
const themeStore = useThemeStore()

// 获取当前语言
const locale = localStorage.getItem('locale') || 'zh_cn'

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
  themeStore.setColors({ ...localColors })
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
    baseSize: localFont.baseSize,
    largeSize: localFont.largeSize,
    smallSize: localFont.smallSize,
  })
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
    ElMessage.warning(t('settings.maxPresetsReached', { max: 10 }))
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
```

- [ ] **Step 2: 提交代码**

```bash
git add src/views/settings/components/ThemeEditor.vue
git commit -m "feat(theme): 创建主题编辑器组件

- 预设主题选择卡片
- 颜色选择器配置
- 字体和字号配置
- 用户自定义预设管理

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: 更新设置页面

**Files:**
- Modify: `src/views/settings/index.vue`

- [ ] **Step 1: 更新设置页面，集成主题编辑器**

将设置页面改造为 Tab 形式，添加主题设置 Tab：

```vue
<template>
  <div class="settings-page p-4">
    <el-card shadow="never" class="rounded-lg">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <span class="text-base font-semibold">{{ t('header.settings') }}</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="settings-tabs pt-4">
        <!-- 主题设置 -->
        <el-tab-pane :label="t('settings.themeSettings')" name="theme">
          <ThemeEditor />
        </el-tab-pane>

        <!-- 基础设置 -->
        <el-tab-pane :label="t('settings.basicSettings')" name="basic">
          <el-form label-width="120px" class="settings-form max-w-[600px] px-4">
            <el-form-item :label="t('settings.theme')">
              <div class="theme-options flex gap-6">
                <!-- 浅色主题选项 -->
                <div
                  class="theme-option flex flex-col items-center gap-2 p-4 border-2 rounded-lg cursor-pointer transition-all"
                  :class="{ 'border-primary bg-primary-light': themeStore.theme === 'light' }"
                  @click="setTheme('light')"
                >
                  <div class="theme-preview light-theme w-20 h-[60px] rounded shadow-md"></div>
                  <span>{{ t('settings.lightTheme') }}</span>
                </div>
                <!-- 暗黑主题选项 -->
                <div
                  class="theme-option flex flex-col items-center gap-2 p-4 border-2 rounded-lg cursor-pointer transition-all"
                  :class="{ 'border-primary bg-primary-light': themeStore.theme === 'dark' }"
                  @click="setTheme('dark')"
                >
                  <div class="theme-preview dark-theme w-20 h-[60px] rounded shadow-md"></div>
                  <span>{{ t('settings.darkTheme') }}</span>
                </div>
              </div>
            </el-form-item>

            <!-- 语言设置 -->
            <el-form-item :label="t('settings.language')">
              <el-select v-model="currentLocale" @change="handleLocaleChange">
                <el-option :label="t('settings.chinese')" value="zh_cn" />
                <el-option label="English" value="en_us" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 侧边栏设置 -->
        <el-tab-pane :label="t('settings.sidebarSettings')" name="sidebar">
          <el-form label-width="120px" class="settings-form max-w-[600px] px-4">
            <el-form-item :label="t('settings.sidebarWidth')">
              <el-slider
                v-model="sidebarWidth"
                :min="150"
                :max="400"
                :step="10"
                show-input
                @change="saveSidebarWidth"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useThemeStore } from '@/stores/theme'
import { setLocale, getCurrentLocale } from '@/locales'
import ThemeEditor from './components/ThemeEditor.vue'

defineOptions({
  name: 'Settings',
})

const { t } = useI18n()
const themeStore = useThemeStore()

const activeTab = ref('theme')
const currentLocale = ref(getCurrentLocale())
const sidebarWidth = ref(220)

const setTheme = (theme: 'light' | 'dark') => {
  themeStore.setMode(theme)
  ElMessage.success(t('message.operationSuccess'))
}

const handleLocaleChange = (locale: string) => {
  if (locale !== currentLocale.value) {
    setLocale(locale)
    currentLocale.value = locale
    ElMessage.success(t('message.operationSuccess'))
  }
}

const saveSidebarWidth = () => {
  localStorage.setItem('sidebarWidth', String(sidebarWidth.value))
  ElMessage.success(t('message.operationSuccess'))
}

onMounted(() => {
  const savedWidth = localStorage.getItem('sidebarWidth')
  if (savedWidth) {
    sidebarWidth.value = parseInt(savedWidth)
  }
})
</script>

<style scoped lang="scss">
/* 设置页面样式 */
.settings-page {
  /* 设置标签页 */
  .settings-tabs {
    @apply pt-4;

    /* 设置表单 */
    .settings-form {
      @apply max-w-[600px] px-4;
    }

    /* 主题选项 */
    .theme-options {
      @apply flex gap-6;

      .theme-option {
        @apply flex flex-col items-center gap-2 p-4 border-2 rounded-lg cursor-pointer transition-all;
        border-color: var(--border-color-light);

        &:hover {
          @apply border-primary;
        }

        /* 主题预览 */
        .theme-preview {
          @apply w-20 h-[60px] rounded shadow-md;

          &.light-theme {
            background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
          }

          &.dark-theme {
            background: linear-gradient(135deg, #1f1f1f 0%, #141414 100%);
          }
        }
      }
    }
  }
}
</style>
```

- [ ] **Step 2: 提交代码**

```bash
git add src/views/settings/index.vue
git commit -m "feat(theme): 集成主题编辑器到设置页面

- 添加主题设置 Tab
- 调整 Tab 顺序，主题设置优先

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: 初始化主题应用

**Files:**
- Modify: `src/main.ts`

- [ ] **Step 1: 在 main.ts 中初始化主题**

确保在应用启动时调用 `initTheme()`：

```typescript
// 在现有代码中，找到 themeStore 相关代码并修改

const themeStore = useThemeStore(pinia)
// 初始化主题（应用存储的颜色、字体配置）
themeStore.initTheme()
```

- [ ] **Step 2: 提交代码**

```bash
git add src/main.ts
git commit -m "feat(theme): 初始化主题应用

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 8: 最终测试和验证

- [ ] **Step 1: 启动开发服务器验证功能**

```bash
cd blink-base/blink-base-web
npm run dev
```

验证清单：
1. 访问设置页面，确认主题设置 Tab 显示正确
2. 点击预设主题卡片，页面颜色即时变化
3. 使用颜色选择器修改颜色，确认生效
4. 切换字体，确认字体加载并应用
5. 调整字号，确认字号变化
6. 保存当前配置为自定义预设
7. 删除自定义预设
8. 重置为默认主题
9. 刷新页面，确认主题配置保持

- [ ] **Step 2: 提交最终代码**

```bash
git add -A
git commit -m "feat(theme): 主题系统实现完成

- 8套预设主题方案
- 自定义主题色、功能色配置
- 字体和字号配置
- 用户自定义预设保存（最多10个）
- CSS 变量动态切换

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 文件清单汇总

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/config/themes.ts` | 新建 | 预设主题配置 |
| `src/utils/fontLoader.ts` | 新建 | 字体加载工具 |
| `src/stores/theme.ts` | 修改 | 扩展主题管理功能 |
| `src/views/settings/components/ThemeEditor.vue` | 新建 | 主题编辑器组件 |
| `src/views/settings/index.vue` | 修改 | 集成主题编辑器 |
| `src/locales/zh-cn.ts` | 修改 | 中文国际化 |
| `src/locales/en-us.ts` | 修改 | 英文国际化 |
| `src/main.ts` | 修改 | 初始化主题 |

## 验收标准

1. 用户可以在设置页面选择预设主题，页面即时切换
2. 用户可以自定义主题色、功能色（5种颜色）
3. 用户可以选择字体（5种预设字体）
4. 用户可以调整字号（基础、大、小三个档位）
5. 用户可以将当前配置保存为自定义预设
6. 自定义预设可以删除
7. 主题配置在刷新页面后保持
8. 深色/浅色模式下主题表现正常
9. 最多支持 10 个自定义预设