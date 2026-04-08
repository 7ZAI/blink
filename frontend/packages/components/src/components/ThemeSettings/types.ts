// src/components/ThemeSettings/types.ts

import type {
  ThemeColors,
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

  // 配置项
  presetThemes?: PresetTheme[]
  presetFonts?: FontOption[]
  maxCustomPresets?: number

  // 状态控制
  modelValue?: FullThemeConfig
  customPresets?: CustomPreset[]
  readonly?: boolean
  mode?: 'light' | 'dark'
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
  (e: 'preset-save', preset: CustomPreset): void
  (e: 'preset-delete', presetId: string): void
}

// 重导出类型
export type {
  ThemeColors,
  FullThemeConfig,
  PresetTheme,
  CustomPreset,
  FontOption,
}