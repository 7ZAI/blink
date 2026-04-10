import { ref } from 'vue'

/**
 * 防止重复提交 composable
 * @param delay 防抖延迟时间（毫秒），默认 1000ms
 */
export function useSubmitGuard(delay = 1000) {
  const isSubmitting = ref(false)
  let lastSubmitTime = 0

  /**
   * 执行提交操作，带防止重复提交保护
   * @param fn 异步提交函数
   * @returns 返回 fn 的结果，如果正在提交则返回 undefined
   */
  async function submitGuard<T>(fn: () => Promise<T>): Promise<T | undefined> {
    const now = Date.now()

    // 检查是否在防抖延迟期内
    if (now - lastSubmitTime < delay) {
      return undefined
    }

    // 如果正在提交中，直接返回
    if (isSubmitting.value) {
      return undefined
    }

    isSubmitting.value = true
    lastSubmitTime = now

    try {
      const result = await fn()
      return result
    } finally {
      // 延迟重置状态，确保用户体验
      setTimeout(() => {
        isSubmitting.value = false
      }, delay)
    }
  }

  /**
   * 手动重置提交状态
   */
  function reset() {
    isSubmitting.value = false
    lastSubmitTime = 0
  }

  /**
   * 立即开始提交状态（用于手动控制 loading）
   */
  function startSubmit() {
    isSubmitting.value = true
    lastSubmitTime = Date.now()
  }

  /**
   * 立即结束提交状态
   */
  function endSubmit() {
    isSubmitting.value = false
  }

  return {
    isSubmitting,
    submitGuard,
    reset,
    startSubmit,
    endSubmit,
  }
}
