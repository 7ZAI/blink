/**
 * API 请求配置
 * 支持网关模式和直连模式切换
 */

export type ApiMode = 'gateway' | 'direct'

export interface ApiConfig {
  /** 请求模式: gateway - 通过网关, direct - 直连base-app */
  mode: ApiMode
  /** 网关地址 */
  gatewayUrl: string
  /** 直连base-app地址 */
  directUrl: string
  /** API基础路径 */
  basePath: string
  /** 是否启用代理（开发环境） */
  enableProxy: boolean
}

/** 默认配置 */
const defaultConfig: ApiConfig = {
  // 默认直连模式，可通过修改此值切换
  mode: (import.meta.env.VITE_API_MODE as ApiMode) || 'direct',
  // 网关地址
  gatewayUrl: import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8002',
  // 直连base-app地址
  directUrl: import.meta.env.VITE_DIRECT_URL || 'http://localhost:8001',
  // API基础路径
  basePath: '/base',
  // 是否启用代理（开发环境通常启用）
  enableProxy: import.meta.env.DEV,
}

/** 当前使用的配置 */
let currentConfig: ApiConfig = { ...defaultConfig }

/**
 * 获取当前API配置
 */
export function getApiConfig(): ApiConfig {
  return currentConfig
}

/**
 * 设置API配置
 */
export function setApiConfig(config: Partial<ApiConfig>): void {
  currentConfig = { ...currentConfig, ...config }
}

/**
 * 切换请求模式
 * @param mode 'gateway' | 'direct'
 */
export function switchApiMode(mode: ApiMode): void {
  currentConfig.mode = mode
  console.log(`[API Config] 已切换到${mode === 'gateway' ? '网关' : '直连'}模式`)
}

/**
 * 获取当前模式下的基础URL
 * 开发环境返回相对路径（配合vite代理使用）
 * 生产环境返回完整URL
 */
export function getBaseUrl(): string {
  const { mode, enableProxy, basePath } = currentConfig

  // 开发环境启用代理时，返回相对路径
  if (enableProxy) {
    return basePath
  }

  // 生产环境或禁用代理时，返回完整URL
  return mode === 'gateway'
    ? `${currentConfig.gatewayUrl}${basePath}`
    : `${currentConfig.directUrl}/base`
}

/**
 * 获取当前模式名称
 */
export function getCurrentModeName(): string {
  return currentConfig.mode === 'gateway' ? '网关模式' : '直连模式'
}

/**
 * 是否是网关模式
 */
export function isGatewayMode(): boolean {
  return currentConfig.mode === 'gateway'
}

/**
 * 是否是直连模式
 */
export function isDirectMode(): boolean {
  return currentConfig.mode === 'direct'
}
