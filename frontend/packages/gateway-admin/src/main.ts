import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Element Plus 全量导入 - 因为 @blink/components 中使用了大量 Element Plus 组件
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// Element Plus 样式
import 'element-plus/dist/index.css'
// 导入 @blink/components 组件库样式
import '@blink/components/dist/components.css'
import './styles/tailwind.css'
import './styles/index.scss'
import App from './App.vue'
import router from './router'
import i18n from './locales'
import { useThemeStore } from './stores/theme'
import { BlinkIcon, AuthButton, dataFadeDirective, rippleDirective } from '@blink/components'

// 在应用挂载前先应用主题，避免加载遮罩颜色闪烁
const savedTheme = (localStorage.getItem('theme') as 'light' | 'dark') || 'light'
document.documentElement.setAttribute('data-theme', savedTheme)
if (savedTheme === 'dark') {
  document.documentElement.classList.add('dark')
}

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册 BlinkIcon 组件
app.component('BlinkIcon', BlinkIcon)

// 注册 AuthButton 权限按钮组件
app.component('AuthButton', AuthButton)

const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(i18n)
app.use(ElementPlus)

// 注册 @blink/components 指令
app.directive('data-fade', dataFadeDirective)
app.directive('ripple', rippleDirective)

// 初始化主题
const themeStore = useThemeStore(pinia)
themeStore.setTheme(themeStore.theme)

app.mount('#app')

// 移除加载遮罩
requestAnimationFrame(() => {
  const loadingEl = document.getElementById('app-loading')
  if (loadingEl) {
    loadingEl.classList.add('fade-out')
    setTimeout(() => {
      loadingEl.remove()
    }, 400)
  }
})
