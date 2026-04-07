/**
 * 主题状态管理 Composable
 *
 * 提供主题切换、颜色设置、字体设置等功能
 * 可独立使用，不依赖特定 store
 *
 * @example
 * ```ts
 * const { theme, colors, toggleTheme, setColors } = useThemeSettings({
 *   onThemeChange: (theme) => console.log('theme changed:', theme)
 * })
 * ```
 */

import { ref, watch, onMounted, type Ref, type ComputedRef, computed, reactive } from 'vue'

/**
 * 主题颜色接口
 */
export interface ThemeColors {
  primary: string
  success: string
  warning: string
  danger: string
  info: string
}

/**
 * 字体配置接口
 */
export interface FontConfig {
  family: string
  baseSize: number
  largeSize: number
  smallSize: number
}

/**
 * 预设主题接口
 */
export interface ThemePreset {
  id: string
  name: string
  nameEn?: string
  colors: ThemeColors
  font?: Partial<FontConfig>
}

/**
 * 主题状态配置
 */
export interface UseThemeSettingsOptions {
  /** 初始主题 */
  initialTheme?: 'light' | 'dark'
  /** 初始颜色 */
  initialColors?: Partial<ThemeColors>
  /** 初始字体配置 */
  initialFont?: Partial<FontConfig>
  /** 是否启用动画 */
  initialAnimationsEnabled?: boolean
  /** 预设主题列表 */
  presetThemes?: ThemePreset[]
  /** 预设字体列表 */
  presetFonts?: { family: string; label: string }[]
  /** 本地存储 key 前缀 */
  storageKey?: string
  /** 主题变化回调 */
  onThemeChange?: (theme: 'light' | 'dark') => void
  /** 颜色变化回调 */
  onColorsChange?: (colors: ThemeColors) => void
  /** 字体变化回调 */
  onFontChange?: (font: FontConfig) => void
}

/**
 * 主题状态返回值
 */
export interface UseThemeSettingsReturn {
  /** 当前主题 */
  theme: Ref<'light' | 'dark'>
  /** 颜色配置 */
  colors: Ref<ThemeColors>
  /** 字体配置 */
  font: Ref<FontConfig>
  /** 是否启用动画 */
  animationsEnabled: Ref<boolean>
  /** 当前预设 ID */
  currentPresetId: Ref<string | null>
  /** 自定义预设列表 */
  customPresets: Ref<ThemePreset[]>
  /** 预设主题列表 */
  presetThemes: ComputedRef<ThemePreset[]>
  /** 预设字体列表 */
  presetFonts: ComputedRef<{ family: string; label: string }[]>
  /** 切换主题 */
  toggleTheme: () => void
  /** 设置主题 */
  setTheme: (theme: 'light' | 'dark') => void
  /** 设置颜色 */
  setColors: (colors: Partial<ThemeColors>) => void
  /** 设置字体 */
  setFont: (font: Partial<FontConfig>) => void
  /** 设置动画开关 */
  setAnimationsEnabled: (enabled: boolean) => void
  /** 应用预设 */
  applyPreset: (presetId: string) => void
  /** 保存为预设 */
  saveAsPreset: (name: string) => boolean
  /** 删除预设 */
  deletePreset: (presetId: string) => void
  /** 重置为默认 */
  resetToDefault: () => void
  /** 初始化主题 */
  initTheme: () => void
}

/**
 * 默认颜色
 */
const DEFAULT_COLORS: ThemeColors = {
  primary: '#3b82f6',
  success: '#22c55e',
  warning: '#f59e0b',
  danger: '#ef4444',
  info: '#06b6d4',
}

/**
 * 默认字体配置
 */
const DEFAULT_FONT: FontConfig = {
  family: 'Noto Sans SC, -apple-system, BlinkMacSystemFont, sans-serif',
  baseSize: 14,
  largeSize: 16,
  smallSize: 12,
}

/**
 * 默认预设主题
 */
const DEFAULT_PRESET_THEMES: ThemePreset[] = [
  {
    id: 'default-blue',
    name: '默认蓝',
    nameEn: 'Default Blue',
    colors: { ...DEFAULT_COLORS },
  },
  {
    id: 'ocean',
    name: '海洋蓝',
    nameEn: 'Ocean Blue',
    colors: { ...DEFAULT_COLORS, primary: '#0ea5e9' },
  },
  {
    id: 'forest',
    name: '森林绿',
    nameEn: 'Forest Green',
    colors: { ...DEFAULT_COLORS, primary: '#22c55e' },
  },
  {
    id: 'sunset',
    name: '日落橙',
    nameEn: 'Sunset Orange',
    colors: { ...DEFAULT_COLORS, primary: '#f97316' },
  },
  {
    id: 'lavender',
    name: '薰衣草紫',
    nameEn: 'Lavender Purple',
    colors: { ...DEFAULT_COLORS, primary: '#a855f7' },
  },
  {
    id: 'rose',
    name: '玫瑰红',
    nameEn: 'Rose Red',
    colors: { ...DEFAULT_COLORS, primary: '#f43f5e' },
  },
]

/**
 * 默认预设字体
 */
const DEFAULT_PRESET_FONTS = [
  { family: 'Noto Sans SC, -apple-system, BlinkMacSystemFont, sans-serif', label: 'Noto Sans SC' },
  { family: 'LXGW WenKai, sans-serif', label: '霞鹜文楷' },
  { family: 'JetBrains Mono, monospace', label: 'JetBrains Mono' },
]

/**
 * 主题状态管理 Composable
 */
export function useThemeSettings(options: UseThemeSettingsOptions = {}): UseThemeSettingsReturn {
  const {
    initialTheme = 'light',
    initialColors = {},
    initialFont = {},
    initialAnimationsEnabled = true,
    presetThemes = DEFAULT_PRESET_THEMES,
    presetFonts = DEFAULT_PRESET_FONTS,
    storageKey = 'blink-theme',
    onThemeChange,
    onColorsChange,
    onFontChange,
  } = options

  // 状态
  const theme = ref<'light' | 'dark'>(initialTheme)
  const colors = ref<ThemeColors>({ ...DEFAULT_COLORS, ...initialColors })
  const font = ref<FontConfig>({ ...DEFAULT_FONT, ...initialFont })
  const animationsEnabled = ref(initialAnimationsEnabled)
  const currentPresetId = ref<string | null>(null)
  const customPresets = ref<ThemePreset[]>([])

  // 计算属性
  const computedPresetThemes = computed(() => presetThemes)
  const computedPresetFonts = computed(() => presetFonts)

  /**
   * 持久化状态到本地存储
   */
  const persistState = () => {
    if (!storageKey) return

    localStorage.setItem(`${storageKey}:theme`, theme.value)
    localStorage.setItem(`${storageKey}:colors`, JSON.stringify(colors.value))
    localStorage.setItem(`${storageKey}:font`, JSON.stringify(font.value))
    localStorage.setItem(`${storageKey}:animations`, String(animationsEnabled.value))
    if (currentPresetId.value) {
      localStorage.setItem(`${storageKey}:presetId`, currentPresetId.value)
    }
    localStorage.setItem(`${storageKey}:customPresets`, JSON.stringify(customPresets.value))
  }

  /**
   * 从本地存储恢复状态
   */
  const restoreState = () => {
    if (!storageKey) return

    const savedTheme = localStorage.getItem(`${storageKey}:theme`)
    if (savedTheme === 'light' || savedTheme === 'dark') {
      theme.value = savedTheme
    }

    const savedColors = localStorage.getItem(`${storageKey}:colors`)
    if (savedColors) {
      try {
        colors.value = { ...DEFAULT_COLORS, ...JSON.parse(savedColors) }
      } catch (e) {
        console.error('Failed to parse saved colors:', e)
      }
    }

    const savedFont = localStorage.getItem(`${storageKey}:font`)
    if (savedFont) {
      try {
        font.value = { ...DEFAULT_FONT, ...JSON.parse(savedFont) }
      } catch (e) {
        console.error('Failed to parse saved font:', e)
      }
    }

    const savedAnimations = localStorage.getItem(`${storageKey}:animations`)
    if (savedAnimations !== null) {
      animationsEnabled.value = savedAnimations === 'true'
    }

    const savedPresetId = localStorage.getItem(`${storageKey}:presetId`)
    if (savedPresetId) {
      currentPresetId.value = savedPresetId
    }

    const savedCustomPresets = localStorage.getItem(`${storageKey}:customPresets`)
    if (savedCustomPresets) {
      try {
        customPresets.value = JSON.parse(savedCustomPresets)
      } catch (e) {
        console.error('Failed to parse saved custom presets:', e)
      }
    }
  }

  /**
   * 应用 CSS 变量
   */
  const applyCssVariables = () => {
    const root = document.documentElement

    // 应用主题类
    root.classList.remove('light', 'dark')
    root.classList.add(theme.value)

    // 应用颜色变量
    Object.entries(colors.value).forEach(([key, value]) => {
      if (!value) return
      root.style.setProperty(`--${key}-color`, value)
      // 生成 RGB 值用于透明度计算
      const rgb = hexToRgb(value)
      if (rgb) {
        root.style.setProperty(`--${key}-color-rgb`, `${rgb.r}, ${rgb.g}, ${rgb.b}`)
      }
    })

    // 应用字体变量
    root.style.setProperty('--font-family', font.value.family)
    root.style.setProperty('--font-size-base', `${font.value.baseSize}px`)
    root.style.setProperty('--font-size-large', `${font.value.largeSize}px`)
    root.style.setProperty('--font-size-small', `${font.value.smallSize}px`)

    // 应用动画开关
    if (!animationsEnabled.value) {
      root.style.setProperty('--duration-fast', '0ms')
      root.style.setProperty('--duration-normal', '0ms')
      root.style.setProperty('--duration-slow', '0ms')
    } else {
      root.style.removeProperty('--duration-fast')
      root.style.removeProperty('--duration-normal')
      root.style.removeProperty('--duration-slow')
    }
  }

  /**
   * 十六进制转 RGB
   */
  const hexToRgb = (hex: string): { r: number; g: number; b: number } | null => {
    if (!hex) return null
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    if (!result || !result[1] || !result[2] || !result[3]) return null
    return {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16),
    }
  }

  /**
   * 切换主题
   */
  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    applyCssVariables()
    persistState()
    onThemeChange?.(theme.value)
  }

  /**
   * 设置主题
   */
  const setTheme = (newTheme: 'light' | 'dark') => {
    theme.value = newTheme
    applyCssVariables()
    persistState()
    onThemeChange?.(newTheme)
  }

  /**
   * 设置颜色
   */
  const setColors = (newColors: Partial<ThemeColors>) => {
    colors.value = { ...colors.value, ...newColors }
    applyCssVariables()
    persistState()
    onColorsChange?.(colors.value)
  }

  /**
   * 设置字体
   */
  const setFont = (newFont: Partial<FontConfig>) => {
    font.value = { ...font.value, ...newFont }
    applyCssVariables()
    persistState()
    onFontChange?.(font.value)
  }

  /**
   * 设置动画开关
   */
  const setAnimationsEnabled = (enabled: boolean) => {
    animationsEnabled.value = enabled
    applyCssVariables()
    persistState()
  }

  /**
   * 应用预设
   */
  const applyPreset = (presetId: string) => {
    const allPresets = [...presetThemes, ...customPresets.value]
    const preset = allPresets.find((p) => p.id === presetId)

    if (preset) {
      colors.value = { ...DEFAULT_COLORS, ...preset.colors }
      if (preset.font) {
        font.value = { ...DEFAULT_FONT, ...preset.font }
      }
      currentPresetId.value = presetId
      applyCssVariables()
      persistState()
      onColorsChange?.(colors.value)
    }
  }

  /**
   * 保存为预设
   */
  const saveAsPreset = (name: string): boolean => {
    const id = `custom-${Date.now()}`
    const newPreset: ThemePreset = {
      id,
      name,
      colors: { ...colors.value },
      font: { ...font.value },
    }

    customPresets.value.push(newPreset)
    currentPresetId.value = id
    persistState()

    return true
  }

  /**
   * 删除预设
   */
  const deletePreset = (presetId: string) => {
    const index = customPresets.value.findIndex((p) => p.id === presetId)
    if (index > -1) {
      customPresets.value.splice(index, 1)
      persistState()
    }
  }

  /**
   * 重置为默认
   */
  const resetToDefault = () => {
    theme.value = 'light'
    colors.value = { ...DEFAULT_COLORS }
    font.value = { ...DEFAULT_FONT }
    animationsEnabled.value = true
    currentPresetId.value = null
    applyCssVariables()
    persistState()
  }

  /**
   * 初始化主题
   */
  const initTheme = () => {
    restoreState()
    applyCssVariables()
  }

  // 监听状态变化
  watch(theme, () => {
    applyCssVariables()
  })

  return {
    theme,
    colors,
    font,
    animationsEnabled,
    currentPresetId,
    customPresets,
    presetThemes: computedPresetThemes,
    presetFonts: computedPresetFonts,
    toggleTheme,
    setTheme,
    setColors,
    setFont,
    setAnimationsEnabled,
    applyPreset,
    saveAsPreset,
    deletePreset,
    resetToDefault,
    initTheme,
  }
}