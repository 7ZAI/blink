import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLoginConfig } from '@/api/auth'

/**
 * 系统配置 Store
 * 管理系统标题、Logo、页脚等配置
 */
export const useSystemConfigStore = defineStore('systemConfig', () => {
  // 系统标题
  const systemTitle = ref('Blink管理系统')

  // 系统Logo (SVG或HTML代码)
  const systemLogo = ref('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 32" fill="#3b82f6"><path transform="translate(-1, 0)" d="M7 2L2 14h5l-2 12 9-16h-5l2-8z"/></svg>')

  // 页脚信息
  const systemFooter = ref('© 2026 Blink Admin')

  // 用户默认头像
  const defaultAvatar = ref('adventurer-neutral')

  // 配置是否已加载
  const loaded = ref(false)

  /**
   * 加载系统配置
   */
  const loadSystemConfig = async () => {
    if (loaded.value) return

    try {
      const result = await getLoginConfig()
      if (result) {
        if (result.systemTitle) {
          systemTitle.value = result.systemTitle
        }
        if (result.systemLogo) {
          systemLogo.value = result.systemLogo
        }
        if (result.systemFooter) {
          systemFooter.value = result.systemFooter
        }
        if (result.defaultAvatar) {
          defaultAvatar.value = result.defaultAvatar
        }
        loaded.value = true
      }
    } catch (error) {
      console.error('Failed to load system config:', error)
    }
  }

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

  return {
    systemTitle,
    systemLogo,
    systemFooter,
    defaultAvatar,
    loaded,
    loadSystemConfig,
    setSystemTitle,
    setSystemLogo,
    setSystemFooter,
    setDefaultAvatar,
  }
})