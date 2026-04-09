import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 系统配置 Store
 * 管理系统标题、Logo、页脚等配置
 */
export const useSystemConfigStore = defineStore('systemConfig', () => {
  // 默认系统标题
  const DEFAULT_TITLE = 'Blink Gateway'

  // 默认系统Logo - 网关管理系统专业设计
  // 设计理念：G字母 + 数据流动 + 网关通道
  const DEFAULT_LOGO = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">
  <defs>
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#3b82f6"/>
      <stop offset="100%" stop-color="#1d4ed8"/>
    </linearGradient>
  </defs>
  <circle cx="24" cy="24" r="22" fill="url(#bgGrad)"/>
  <circle cx="24" cy="24" r="20" fill="none" stroke="#60a5fa" stroke-width="1" opacity="0.5"/>
  <!-- G 字母 - 网关形态 -->
  <path d="M14 24 C14 17 19 12 26 12 C33 12 38 17 38 24 C38 31 33 36 26 36 L26 28 L34 28" fill="none" stroke="#fff" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
  <!-- 数据流入口箭头 -->
  <path d="M8 24 L14 20 L14 28 Z" fill="#fff"/>
  <!-- 数据流出口箭头 -->
  <path d="M40 24 L34 28 L34 20 Z" fill="#fff"/>
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