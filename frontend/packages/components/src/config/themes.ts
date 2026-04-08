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
    family: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif',
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
}

/**
 * 字体选项（用于下拉选择）
 */
export type FontOption = ThemeFont

/**
 * 预设主题（包含颜色）
 */
export type PresetTheme = ThemeConfig

/**
 * 自定义预设（包含创建时间）
 */
export interface CustomPreset extends PresetTheme {
  createdAt: number
}