import { ref, nextTick, type Ref } from 'vue'

/**
 * 数据加载平滑过渡 Composable
 * 用于实现数据加载完成后的平滑显示效果
 *
 * 使用示例：
 * ```vue
 * <template>
 *   <div class="data-transition-wrapper" :class="transitionClass">
 *     <el-table v-loading="loading" :data="list">...</el-table>
 *   </div>
 * </template>
 *
 * <script setup>
 * const { transitionClass, finishTransition } = useTransition()
 *
 * const fetchData = async () => {
 *   loading.value = true
 *   try {
 *     const res = await api.getData()
 *     list.value = res.rows
 *   } finally {
 *     loading.value = false
 *     finishTransition()
 *   }
 * }
 * </script>
 * ```
 */
export function useTransition(options: { duration?: number } = {}) {
  const { duration = 300 } = options
  // 初始状态为空，让内容可见，避免首次加载时隐藏
  const transitionClass = ref('')

  /**
   * 开始加载状态
   */
  const startTransition = () => {
    transitionClass.value = 'data-loading'
  }

  /**
   * 数据加载完成，平滑显示
   * 在数据加载完成后调用
   */
  const finishTransition = async () => {
    await nextTick()
    requestAnimationFrame(() => {
      transitionClass.value = 'data-enter'
      setTimeout(() => {
        transitionClass.value = 'data-loaded'
      }, duration)
    })
  }

  /**
   * 重置过渡状态
   * 用于组件重新加载时重置状态
   */
  const resetTransition = () => {
    transitionClass.value = ''
  }

  /**
   * 包装异步数据加载函数，自动处理过渡效果
   * @param loader 数据加载函数
   * @param onSuccess 成功回调
   */
  const withTransition = async <R>(
    loader: () => Promise<R>,
    onSuccess?: (result: R) => void
  ): Promise<R | undefined> => {
    startTransition()
    try {
      const result = await loader()
      onSuccess?.(result)
      await finishTransition()
      return result
    } catch (error) {
      // 即使出错也要完成过渡，避免内容一直隐藏
      transitionClass.value = 'data-loaded'
      throw error
    }
  }

  return {
    transitionClass,
    startTransition,
    finishTransition,
    resetTransition,
    withTransition,
  }
}

/**
 * 简单的淡入过渡 Composable
 * 用于单个组件的淡入效果
 */
export function useFadeIn(delay: number = 0) {
  const isVisible = ref(false)
  const opacity = ref(0)

  const show = async () => {
    if (delay > 0) {
      await new Promise(resolve => setTimeout(resolve, delay))
    }
    isVisible.value = true
    requestAnimationFrame(() => {
      opacity.value = 1
    })
  }

  const hide = () => {
    opacity.value = 0
    setTimeout(() => {
      isVisible.value = false
    }, 300)
  }

  return {
    isVisible,
    opacity,
    show,
    hide,
  }
}

// 导出类型
export type TransitionState = 'data-loading' | 'data-enter' | 'data-loaded'