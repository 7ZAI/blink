/**
 * 侧边栏状态管理 Composable
 */

import { ref, watch, type Ref, type ComputedRef, computed } from 'vue'

export interface UseSidebarStateOptions {
  defaultWidth?: number
  minWidth?: number
  maxWidth?: number
  collapseThreshold?: number
  storageKey?: string
  resizable?: boolean
  onCollapseChange?: (collapsed: boolean) => void
  onWidthChange?: (width: number) => void
}

export interface UseSidebarStateReturn {
  sidebarWidth: Ref<number>
  isCollapsed: Ref<boolean>
  isResizing: Ref<boolean>
  collapsed: ComputedRef<boolean>
  toggleSidebar: () => void
  setCollapsed: (collapsed: boolean) => void
  setWidth: (width: number) => void
  startResize: (e: MouseEvent) => void
  resetSidebar: () => void
}

export function useSidebarState(options: UseSidebarStateOptions = {}): UseSidebarStateReturn {
  const {
    defaultWidth = 220,
    minWidth = 60,
    maxWidth = 400,
    collapseThreshold = 100,
    storageKey = 'blink-layout-sidebar',
    resizable = true,
    onCollapseChange,
    onWidthChange,
  } = options

  const sidebarWidth = ref(defaultWidth)
  const isCollapsed = ref(false)
  const isResizing = ref(false)

  const collapsed = computed(() => isCollapsed.value)

  const persistSidebarState = () => {
    if (!storageKey) return
    localStorage.setItem(`${storageKey}:width`, String(sidebarWidth.value))
    localStorage.setItem(`${storageKey}:collapsed`, String(isCollapsed.value))
  }

  const restoreSidebarState = () => {
    if (!storageKey) return
    const savedWidth = localStorage.getItem(`${storageKey}:width`)
    const savedCollapsed = localStorage.getItem(`${storageKey}:collapsed`)

    if (savedCollapsed !== null) {
      isCollapsed.value = savedCollapsed === 'true'
      if (isCollapsed.value && savedWidth === null) {
        sidebarWidth.value = minWidth
      }
    }

    if (savedWidth !== null) {
      const width = parseInt(savedWidth)
      if (width >= minWidth && width <= maxWidth) {
        sidebarWidth.value = width
      }
    }
  }

  const setCollapsed = (value: boolean) => {
    isCollapsed.value = value
    if (value) {
      sidebarWidth.value = minWidth
    } else {
      sidebarWidth.value = defaultWidth
    }
    persistSidebarState()
  }

  const setWidth = (width: number) => {
    if (width < minWidth) width = minWidth
    if (width > maxWidth) width = maxWidth
    sidebarWidth.value = width
    isCollapsed.value = width <= collapseThreshold
    persistSidebarState()
  }

  const toggleSidebar = () => {
    setCollapsed(!isCollapsed.value)
  }

  const resetSidebar = () => {
    sidebarWidth.value = defaultWidth
    isCollapsed.value = false
    persistSidebarState()
  }

  const startResize = (e: MouseEvent) => {
    if (!resizable) return

    isResizing.value = true
    const startX = e.clientX
    const startWidth = sidebarWidth.value

    document.body.classList.add('resizing-sidebar')

    const handleMouseMove = (e: MouseEvent) => {
      const delta = e.clientX - startX
      let newWidth = startWidth + delta
      if (newWidth < minWidth) newWidth = minWidth
      if (newWidth > maxWidth) newWidth = maxWidth
      sidebarWidth.value = newWidth
      isCollapsed.value = newWidth <= collapseThreshold
    }

    const handleMouseUp = () => {
      isResizing.value = false
      document.body.classList.remove('resizing-sidebar')
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
      persistSidebarState()
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  watch(isCollapsed, (value) => {
    onCollapseChange?.(value)
  })

  watch(sidebarWidth, (value) => {
    onWidthChange?.(value)
  })

  restoreSidebarState()

  return {
    sidebarWidth,
    isCollapsed,
    isResizing,
    collapsed,
    toggleSidebar,
    setCollapsed,
    setWidth,
    startResize,
    resetSidebar,
  }
}