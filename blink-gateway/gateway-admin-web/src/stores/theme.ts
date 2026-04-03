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
  ANIMATIONS_ENABLED: 'themeAnimationsEnabled',
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

  // 动画开关
  const animationsEnabled = ref<boolean>(
    localStorage.getItem(STORAGE_KEYS.ANIMATIONS_ENABLED) !== 'false'
  )

  // ========== 计算属性 ==========

  // 当前主题是否为预设主题
  const isPresetTheme = computed(() => {
    return PRESET_THEMES.some((t) => t.id === currentPresetId.value)
  })

  // ========== 初始化函数 ==========

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
      family: PRESET_FONTS[0]!.family,
      baseSize: FONT_SIZE_CONFIG.base.default,
      largeSize: FONT_SIZE_CONFIG.large.default,
      smallSize: FONT_SIZE_CONFIG.small.default,
    }
  }

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

  function setMode(newMode: ThemeMode) {
    mode.value = newMode
    localStorage.setItem(STORAGE_KEYS.THEME_MODE, newMode)
    applyMode(newMode)
  }

  function toggleMode() {
    setMode(mode.value === 'light' ? 'dark' : 'light')
  }

  function applyMode(newMode: ThemeMode) {
    if (newMode === 'dark') {
      document.documentElement.classList.add('dark')
      document.documentElement.setAttribute('data-theme', 'dark')
    } else {
      document.documentElement.classList.remove('dark')
      document.documentElement.setAttribute('data-theme', 'light')
    }
  }

  function setAnimationsEnabled(enabled: boolean) {
    animationsEnabled.value = enabled
    localStorage.setItem(STORAGE_KEYS.ANIMATIONS_ENABLED, String(enabled))
    applyAnimations(enabled)
  }

  function applyAnimations(enabled: boolean) {
    const root = document.documentElement
    if (enabled) {
      root.style.setProperty('--duration-fast', '150ms')
      root.style.setProperty('--duration-normal', '300ms')
      root.style.setProperty('--duration-slow', '500ms')
      root.classList.remove('animations-disabled')
    } else {
      root.style.setProperty('--duration-fast', '0ms')
      root.style.setProperty('--duration-normal', '0ms')
      root.style.setProperty('--duration-slow', '0ms')
      root.classList.add('animations-disabled')
    }
  }

  function toggleAnimations() {
    setAnimationsEnabled(!animationsEnabled.value)
  }

  function setColors(newColors: ThemeColors) {
    colors.value = { ...newColors }
    localStorage.setItem(STORAGE_KEYS.THEME_COLORS, JSON.stringify(newColors))
    applyColors(newColors)
  }

  function applyColors(newColors: ThemeColors) {
    const root = document.documentElement

    const primaryVars = generateColorVars(newColors.primary, 'primary')
    Object.entries(primaryVars).forEach(([key, value]) => {
      root.style.setProperty(key, value)
    })

    root.style.setProperty('--primary-color', newColors.primary)
    root.style.setProperty('--primary-color-light', lighten(newColors.primary, 0.3))
    root.style.setProperty('--primary-color-dark', darken(newColors.primary, 0.2))
    root.style.setProperty('--primary-color-rgb', hexToRgb(newColors.primary))

    const colorTypes: Array<keyof ThemeColors> = ['success', 'warning', 'danger', 'info']
    colorTypes.forEach((type) => {
      const vars = generateColorVars(newColors[type], type)
      Object.entries(vars).forEach(([key, value]) => {
        root.style.setProperty(key, value)
      })
    })
  }

  function hexToRgb(hex: string): string {
    const num = parseInt(hex.replace('#', ''), 16)
    return `${(num >> 16) & 255}, ${(num >> 8) & 255}, ${num & 255}`
  }

  function setFont(newFont: {
    family: string
    baseSize?: number
    largeSize?: number
    smallSize?: number
  }) {
    font.value = {
      family: newFont.family,
      baseSize: newFont.baseSize ?? font.value.baseSize,
      largeSize: newFont.largeSize ?? font.value.largeSize,
      smallSize: newFont.smallSize ?? font.value.smallSize,
    }

    localStorage.setItem(STORAGE_KEYS.THEME_FONT, JSON.stringify(font.value))
    applyFont(font.value)
  }

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

  function applyPreset(presetId: string) {
    const preset = PRESET_THEMES.find((t) => t.id === presetId)
    if (preset) {
      setColors(preset.colors)
      currentPresetId.value = presetId
      localStorage.setItem(STORAGE_KEYS.CURRENT_PRESET_ID, presetId)
      return
    }

    const customPreset = customPresets.value.find((t) => t.id === presetId)
    if (customPreset) {
      setColors(customPreset.colors)
      currentPresetId.value = presetId
      localStorage.setItem(STORAGE_KEYS.CURRENT_PRESET_ID, presetId)
    }
  }

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

  function deletePreset(presetId: string) {
    const index = customPresets.value.findIndex((t) => t.id === presetId)
    if (index !== -1) {
      customPresets.value.splice(index, 1)
      localStorage.setItem(STORAGE_KEYS.CUSTOM_PRESETS, JSON.stringify(customPresets.value))

      if (currentPresetId.value === presetId) {
        applyPreset(DEFAULT_THEME_CONFIG.id)
      }
    }
  }

  function resetToDefault() {
    applyPreset(DEFAULT_THEME_CONFIG.id)
    setFont({
      family: PRESET_FONTS[0]!.family,
      baseSize: FONT_SIZE_CONFIG.base.default,
      largeSize: FONT_SIZE_CONFIG.large.default,
      smallSize: FONT_SIZE_CONFIG.small.default,
    })
  }

  function initTheme() {
    applyMode(mode.value)
    applyColors(colors.value)
    applyFont(font.value)
    applyAnimations(animationsEnabled.value)
  }

  watch(mode, (val) => {
    applyMode(val)
  })

  return {
    mode,
    colors,
    font,
    customPresets,
    currentPresetId,
    animationsEnabled,
    isPresetTheme,

    setMode,
    toggleMode,
    setColors,
    setFont,
    applyPreset,
    saveAsPreset,
    deletePreset,
    resetToDefault,
    initTheme,
    setAnimationsEnabled,
    toggleAnimations,

    // 向后兼容别名
    theme: mode,
    setTheme: setMode,
    toggleTheme: toggleMode,
  }
})

export { STORAGE_KEYS }