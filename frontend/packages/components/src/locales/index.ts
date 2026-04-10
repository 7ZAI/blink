import { createI18n } from 'vue-i18n'
import zhCn from './zh-cn'
import enUs from './en-us'

const messages = {
  zh_cn: {
    ...zhCn,
  },
  en_us: {
    ...enUs,
  },
}

const getStoredLocale = (): string => {
  const stored = localStorage.getItem('locale') || 'zh_cn'
  return stored.replace(/-/g, '_')
}

const i18n = createI18n({
  legacy: false,
  locale: getStoredLocale(),
  fallbackLocale: 'zh_cn',
  messages,
})

export default i18n

export const setLocale = (locale: string) => {
  const normalizedLocale = locale.replace(/-/g, '_')
  localStorage.setItem('locale', normalizedLocale)
  i18n.global.locale.value = normalizedLocale as any
}

export const getCurrentLocale = (): string => {
  return i18n.global.locale.value
}
