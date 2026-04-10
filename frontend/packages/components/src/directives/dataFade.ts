import type { Directive, DirectiveBinding } from 'vue'

/**
 * 数据加载平滑过渡指令
 * 使用方式: v-data-fade="{ loading: isLoading, delay: 50 }"
 *
 * 当 loading 从 true 变为 false 时，会自动添加淡入动画
 */

interface DataFadeOptions {
  /** 是否正在加载 */
  loading: boolean
  /** 动画延迟（毫秒），用于错开多个元素的动画时间 */
  delay?: number
  /** 动画持续时间（毫秒） */
  duration?: number
}

const dataFadeDirective: Directive<HTMLElement, DataFadeOptions> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<DataFadeOptions>) {
    const options = binding.value || { loading: true }
    const duration = options.duration || 300
    const delay = options.delay || 0

    // 设置初始状态
    el.style.transition = `opacity ${duration}ms ease-out ${delay}ms, transform ${duration}ms ease-out ${delay}ms`

    if (options.loading) {
      el.style.opacity = '0'
      el.style.transform = 'translateY(10px)'
    } else {
      el.style.opacity = '1'
      el.style.transform = 'translateY(0)'
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding<DataFadeOptions>) {
    const options = binding.value || { loading: false }
    const oldValue = binding.oldValue as DataFadeOptions | undefined

    // 只在 loading 状态从 true 变为 false 时触发动画
    if (oldValue?.loading && !options.loading) {
      // 触发重排后添加过渡
      requestAnimationFrame(() => {
        el.style.opacity = '1'
        el.style.transform = 'translateY(0)'
      })
    } else if (!oldValue?.loading && options.loading) {
      // loading 变为 true 时隐藏
      el.style.opacity = '0'
      el.style.transform = 'translateY(10px)'
    }
  },
}

/**
 * 列表项淡入指令
 * 用于列表数据的逐项淡入效果
 * 使用方式: v-list-fade="{ loading: isLoading, index: index }"
 */
interface ListFadeOptions {
  loading: boolean
  index: number
  baseDelay?: number
}

const listFadeDirective: Directive<HTMLElement, ListFadeOptions> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<ListFadeOptions>) {
    const options = binding.value || { loading: true, index: 0 }
    const baseDelay = options.baseDelay || 30
    const delay = options.index * baseDelay
    const duration = 300

    el.style.transition = `opacity ${duration}ms ease-out ${delay}ms, transform ${duration}ms ease-out ${delay}ms`

    if (options.loading) {
      el.style.opacity = '0'
      el.style.transform = 'translateX(-10px)'
    } else {
      el.style.opacity = '1'
      el.style.transform = 'translateX(0)'
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding<ListFadeOptions>) {
    const options = binding.value || { loading: false, index: 0 }
    const oldValue = binding.oldValue as ListFadeOptions | undefined

    if (oldValue?.loading && !options.loading) {
      requestAnimationFrame(() => {
        el.style.opacity = '1'
        el.style.transform = 'translateX(0)'
      })
    } else if (!oldValue?.loading && options.loading) {
      el.style.opacity = '0'
      el.style.transform = 'translateX(-10px)'
    }
  },
}

/**
 * 表格行淡入指令
 * 使用方式: v-table-fade="isLoading"
 */
const tableFadeDirective: Directive<HTMLElement, boolean> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<boolean>) {
    const isLoading = binding.value
    el.style.transition = 'opacity 0.3s ease-out, transform 0.3s ease-out'

    if (isLoading) {
      el.style.opacity = '0'
      el.style.transform = 'translateY(5px)'
    } else {
      el.style.opacity = '1'
      el.style.transform = 'translateY(0)'
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding<boolean>) {
    const isLoading = binding.value
    const wasLoading = binding.oldValue

    if (wasLoading && !isLoading) {
      requestAnimationFrame(() => {
        el.style.opacity = '1'
        el.style.transform = 'translateY(0)'
      })
    } else if (!wasLoading && isLoading) {
      el.style.opacity = '0'
      el.style.transform = 'translateY(5px)'
    }
  },
}

export { dataFadeDirective, listFadeDirective, tableFadeDirective }

export default {
  install(app: any) {
    app.directive('data-fade', dataFadeDirective)
    app.directive('list-fade', listFadeDirective)
    app.directive('table-fade', tableFadeDirective)
  },
}
