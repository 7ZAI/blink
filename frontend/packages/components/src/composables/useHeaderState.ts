/**
 * 头部状态管理 Composable
 */

import { ref, onMounted, onUnmounted, type Ref, type ComputedRef, computed } from 'vue'

export interface UseHeaderStateOptions {
  currentTheme?: 'light' | 'dark'
  currentLanguage?: string
  showFullscreen?: boolean
  onThemeToggle?: () => void
  onLanguageChange?: (lang: string) => void
  onUserCommand?: (command: string) => void
}

export interface UseHeaderStateReturn {
  isFullscreen: Ref<boolean>
  theme: ComputedRef<'light' | 'dark'>
  language: ComputedRef<string>
  toggleFullscreen: () => void
  handleThemeToggle: () => void
  handleLanguageChange: (lang: string) => void
  handleUserCommand: (command: string) => void
}

export function useHeaderState(options: UseHeaderStateOptions = {}): UseHeaderStateReturn {
  const {
    currentTheme = 'light',
    currentLanguage = 'zh_cn',
    showFullscreen = true,
    onThemeToggle,
    onLanguageChange,
    onUserCommand,
  } = options

  const isFullscreen = ref(false)
  const theme = computed(() => currentTheme)
  const language = computed(() => currentLanguage)

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen()
      isFullscreen.value = true
    } else {
      document.exitFullscreen()
      isFullscreen.value = false
    }
  }

  const handleThemeToggle = () => {
    onThemeToggle?.()
  }

  const handleLanguageChange = (lang: string) => {
    onLanguageChange?.(lang)
  }

  const handleUserCommand = (command: string) => {
    onUserCommand?.(command)
  }

  onMounted(() => {
    if (showFullscreen) {
      document.addEventListener('fullscreenchange', () => {
        isFullscreen.value = !!document.fullscreenElement
      })
      isFullscreen.value = !!document.fullscreenElement
    }
  })

  return {
    isFullscreen,
    theme,
    language,
    toggleFullscreen,
    handleThemeToggle,
    handleLanguageChange,
    handleUserCommand,
  }
}
