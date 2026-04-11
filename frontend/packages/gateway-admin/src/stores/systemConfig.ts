import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 系统配置 Store
 * 管理系统标题、Logo、页脚等配置
 */
export const useSystemConfigStore = defineStore('systemConfig', () => {
  // 默认系统标题
  const DEFAULT_TITLE = 'Blink Gateway'

  // 默认系统Logo - Blink Gateway 专业设计
  // 设计理念：双层菱形网关 + 数据流动 + B标识
  const DEFAULT_LOGO = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
  <defs>
    <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#6366f1"/>
      <stop offset="100%" stop-color="#3b82f6"/>
    </linearGradient>
  </defs>
  <rect x="4" y="4" width="40" height="40" rx="10" fill="url(#grad)"/>
  <g fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M24 8 L36 20 L36 28 L24 40 L12 28 L12 20 L24 8"/>
    <path d="M24 14 L30 20 L30 28 L24 34 L18 28 L18 20 L24 14" stroke-opacity="0.6"/>
  </g>
  <circle cx="24" cy="24" r="6" fill="#fff"/>
  <path d="M21 21 L21 27 L26 27" fill="none" stroke="#6366f1" stroke-width="2" stroke-linecap="round"/>
  <path d="M2 24 L8 24 L8 20 L12 24 L8 28 L8 24" fill="#a5b4fc"/>
  <path d="M46 24 L40 24 L40 20 L36 24 L40 28 L40 24" fill="#a5b4fc"/>
</svg>`

  // 默认页脚
  const DEFAULT_FOOTER = '© 2026 Blink Gateway Admin'

  // 系统标题
  const systemTitle = ref(DEFAULT_TITLE)

  // 系统Logo (SVG或HTML代码)
  const systemLogo = ref(DEFAULT_LOGO)

  // 页脚信息
  const systemFooter = ref(DEFAULT_FOOTER)

  // 用户默认头像
  const defaultAvatar = ref('adventurer-neutral')

  // 配置是否已加载
  const loaded = ref(false)

  /**
   * 更新系统标题
   */
  const setSystemTitle = (title: string) => {
    systemTitle.value = title
  }

  /**
   * 更新系统Logo
   */
  const setSystemLogo = (logo: string) => {
    systemLogo.value = logo
  }

  /**
   * 更新页脚信息
   */
  const setSystemFooter = (footer: string) => {
    systemFooter.value = footer
  }

  /**
   * 更新用户默认头像
   */
  const setDefaultAvatar = (avatar: string) => {
    defaultAvatar.value = avatar
  }

  /**
   * 重置为默认配置
   * 当获取配置失败时调用
   */
  const resetToDefault = () => {
    systemTitle.value = DEFAULT_TITLE
    systemLogo.value = DEFAULT_LOGO
    systemFooter.value = DEFAULT_FOOTER
    defaultAvatar.value = 'adventurer-neutral'
  }

  return {
    systemTitle,
    systemLogo,
    systemFooter,
    defaultAvatar,
    loaded,
    setSystemTitle,
    setSystemLogo,
    setSystemFooter,
    setDefaultAvatar,
    resetToDefault,
  }
})
