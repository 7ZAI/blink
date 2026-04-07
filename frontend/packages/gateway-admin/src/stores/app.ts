import { defineStore } from 'pinia'

const SIDEBAR_COLLAPSED_KEY = 'sidebarCollapsed'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true',
    language: localStorage.getItem('language') || 'zh-cn'
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(this.sidebarCollapsed))
    },
    setLanguage(lang: string) {
      this.language = lang
      localStorage.setItem('language', lang)
    }
  }
})
