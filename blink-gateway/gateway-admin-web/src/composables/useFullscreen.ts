import { useFullscreen as useVueUseFullscreen } from '@vueuse/core'

export const useFullscreen = () => {
  const { isFullscreen, toggle } = useVueUseFullscreen()

  return {
    isFullscreen,
    toggleFullscreen: toggle
  }
}