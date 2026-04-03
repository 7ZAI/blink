// src/utils/fontLoader.ts

/**
 * 已加载的字体集合
 */
const loadedFonts = new Set<string>()

/**
 * 加载 Google Fonts
 * @param googleFontsName Google Fonts API 名称，如 "Noto+Sans+SC:wght@400;500;600;700"
 */
export function loadGoogleFont(googleFontsName: string): Promise<void> {
  // 如果已经加载过，直接返回
  if (loadedFonts.has(googleFontsName)) {
    return Promise.resolve()
  }

  return new Promise((resolve, reject) => {
    // 检查是否已存在该 link 标签
    const existingLink = document.querySelector(
      `link[href*="${googleFontsName.split(':')[0]}"]`
    )
    if (existingLink) {
      loadedFonts.add(googleFontsName)
      resolve()
      return
    }

    const link = document.createElement('link')
    link.href = `https://fonts.googleapis.com/css2?family=${googleFontsName}&display=swap`
    link.rel = 'stylesheet'
    link.crossOrigin = 'anonymous'

    link.onload = () => {
      loadedFonts.add(googleFontsName)
      resolve()
    }

    link.onerror = () => {
      console.warn(`Failed to load font: ${googleFontsName}`)
      // 即使加载失败也 resolve，避免阻塞
      resolve()
    }

    document.head.appendChild(link)
  })
}

/**
 * 预加载所有预设字体
 */
export async function preloadFonts(
  fonts: Array<{ googleFontsName?: string }>
): Promise<void> {
  const loadPromises = fonts
    .filter((f) => f.googleFontsName)
    .map((f) => loadGoogleFont(f.googleFontsName!))

  await Promise.allSettled(loadPromises)
}

/**
 * 检查字体是否已加载
 */
export function isFontLoaded(fontName: string): boolean {
  return loadedFonts.has(fontName)
}