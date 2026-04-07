import type { Directive, DirectiveBinding } from 'vue'

/**
 * 点击波纹效果指令
 * 使用方式: v-ripple
 */
interface RippleOptions {
  color?: string
  duration?: number
}

const rippleDirective: Directive<HTMLElement, RippleOptions> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<RippleOptions>) {
    const options = {
      color: binding.value?.color || 'rgba(59, 130, 246, 0.4)',
      duration: binding.value?.duration || 600,
    }

    el.style.position = 'relative'
    el.style.overflow = 'hidden'

    el.addEventListener('click', (e: MouseEvent) => {
      const rect = el.getBoundingClientRect()
      const x = e.clientX - rect.left
      const y = e.clientY - rect.top

      const ripple = document.createElement('span')
      ripple.style.cssText = `
        position: absolute;
        border-radius: 50%;
        background: ${options.color};
        transform: scale(0);
        animation: ripple-effect ${options.duration}ms ease-out;
        pointer-events: none;
        left: ${x}px;
        top: ${y}px;
        width: 10px;
        height: 10px;
        margin-left: -5px;
        margin-top: -5px;
        box-shadow: 0 0 10px ${options.color};
      `

      // 添加动画样式
      if (!document.querySelector('#ripple-style')) {
        const style = document.createElement('style')
        style.id = 'ripple-style'
        style.textContent = `
          @keyframes ripple-effect {
            0% {
              transform: scale(0);
              opacity: 1;
            }
            100% {
              transform: scale(40);
              opacity: 0;
            }
          }
        `
        document.head.appendChild(style)
      }

      el.appendChild(ripple)

      setTimeout(() => {
        ripple.remove()
      }, options.duration)
    })
  },
}

export default rippleDirective