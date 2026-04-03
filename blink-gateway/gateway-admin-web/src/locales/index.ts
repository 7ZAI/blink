import { createI18n } from 'vue-i18n'
import zhCN from './zh-cn'
import enUS from './en-us'

const messages = {
  'zh-cn': zhCN,
  'en-us': enUS
}

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('language') || 'zh-cn',
  fallbackLocale: 'zh-cn',
  messages
})

export default i18n

export const setLocale = (locale: string) => {
  const normalizedLocale = locale.replace(/_/g, '-')
  localStorage.setItem('language', normalizedLocale)
  i18n.global.locale.value = normalizedLocale as any
}

export const getCurrentLocale = (): string => {
  return i18n.global.locale.value
}
