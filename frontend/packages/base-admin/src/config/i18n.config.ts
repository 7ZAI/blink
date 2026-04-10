import i18n, { getCurrentLocale, setLocale } from '@/locales'

const t = (key: string): string => {
  return i18n.global.t(key)
}

const getErrorMessage = (code: string, defaultMsg?: string): string => {
  const errorCodeMap: Record<string, string> = {
    BLINK0000: 'message.success',
    BLINK0001: 'message.failed',
    SYS00001: 'message.failed',
    SYS00401: 'common.unauthorized',
    SYS00403: 'common.forbidden',
    BUSS0001: 'message.failed',
    BUSS0015: 'message.failed',
  }

  const key = errorCodeMap[code]
  if (key) {
    return t(key)
  }
  return defaultMsg || t('message.failed')
}

export type LocaleType = 'zh_cn' | 'en_us'

export { t, getCurrentLocale, setLocale, getErrorMessage }
