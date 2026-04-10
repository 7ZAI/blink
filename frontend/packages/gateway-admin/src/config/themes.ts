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
    family:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif',
    label: '系统默认',
  },
  {
    family: '"Noto Sans SC", sans-serif',
    label: '思源黑体',
  },
  {
    family: '"Noto Serif SC", serif',
    label: '思源宋体',
  },
  {
    family: '"LXGW WenKai", cursive',
    label: '霞鹜文楷',
  },
  {
    family: '"JetBrains Mono", monospace',
    label: '等宽字体',
  },
]

/**
 * 预设主题列表 - 现代简约风格
 */
export const PRESET_THEMES: ThemeConfig[] = [
  {
    id: 'default-blue',
    name: '清新蓝',
    nameEn: 'Fresh Blue',
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
    id: 'warm-brown',
    name: '暖棕调',
    nameEn: 'Warm Brown',
    colors: {
      primary: '#78716c',
      success: '#059669',
      warning: '#d97706',
      danger: '#dc2626',
      info: '#4f46e5',
    },
    isPreset: true,
  },
  {
    id: 'forest-green',
    name: '森林绿',
    nameEn: 'Forest Green',
    colors: {
      primary: '#059669',
      success: '#0d9488',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#6366f1',
    },
    isPreset: true,
  },
  {
    id: 'ocean-teal',
    name: '海洋青',
    nameEn: 'Ocean Teal',
    colors: {
      primary: '#0d9488',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#6366f1',
    },
    isPreset: true,
  },
  {
    id: 'soft-indigo',
    name: '柔和靛',
    nameEn: 'Soft Indigo',
    colors: {
      primary: '#6366f1',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#ef4444',
      info: '#3b82f6',
    },
    isPreset: true,
  },
  {
    id: 'rose-pink',
    name: '玫瑰粉',
    nameEn: 'Rose Pink',
    colors: {
      primary: '#f43f5e',
      success: '#10b981',
      warning: '#f59e0b',
      danger: '#b91c1c',
      info: '#6366f1',
    },
    isPreset: true,
  },
  {
    id: 'amber-gold',
    name: '琥珀金',
    nameEn: 'Amber Gold',
    colors: {
      primary: '#f59e0b',
      success: '#10b981',
      warning: '#d97706',
      danger: '#ef4444',
      info: '#6366f1',
    },
    isPreset: true,
  },
]

/**
 * 默认主题配置
 */
export const DEFAULT_THEME_CONFIG: ThemeConfig = PRESET_THEMES[0]!

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
