import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Element Plus 样式（组件已通过 unplugin-vue-components 按需引入）
import 'element-plus/dist/index.css'
// Element Plus 图标（全局注册用于 BlinkIcon 组件）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 引入 Tailwind CSS
import './styles/tailwind.css'
import './styles/index.scss'
// 引入本地字体
import './assets/fonts/fonts.css'
import App from './App.vue'
import router from './router'
import i18n from './locales'
import { useThemeStore } from './stores/theme'
import { useUserStore } from './stores/user'
import BlinkIcon from './components/BlinkIcon/index.vue'
import { DictSelect, DictTag } from './components/Dict'
import dataFadeDirectives from './directives/dataFade'
import rippleDirective from './directives/ripple'
import authDirective from './directives/auth'
import AuthButton from './components/AuthButton.vue'

// 在应用挂载前先应用主题，避免加载遮罩颜色闪烁
// 使用 Tailwind CSS 的 dark class 模式
const savedTheme = (localStorage.getItem('theme') as 'light' | 'dark') || 'light'
if (savedTheme === 'dark') {
  document.documentElement.classList.add('dark')
}

// 更新加载遮罩的背景色
const updateLoadingMaskTheme = (theme: 'light' | 'dark') => {
  const loadingEl = document.getElementById('app-loading')
  if (loadingEl) {
    if (theme === 'dark') {
      loadingEl.style.background = 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)'
    } else {
      loadingEl.style.background = 'linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%)'
    }
  }
}
updateLoadingMaskTheme(savedTheme)

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.component('BlinkIcon', BlinkIcon)
// 注册字典组件为全局组件
app.component('DictSelect', DictSelect)
app.component('DictTag', DictTag)
// 注册权限按钮组件
app.component('AuthButton', AuthButton)
app.use(dataFadeDirectives)
app.use(authDirective)
app.directive('ripple', rippleDirective)

const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(i18n)

const themeStore = useThemeStore(pinia)
// 初始化主题（应用存储的颜色、字体配置）
themeStore.initTheme()

const userStore = useUserStore(pinia)

userStore.restoreUserInfo().then(() => {
  app.mount('#app')

  // 平滑过渡：先显示app内容，再淡出加载遮罩
  requestAnimationFrame(() => {
    const appEl = document.getElementById('app')
    const loadingEl = document.getElementById('app-loading')

    if (appEl) {
      appEl.classList.add('app-ready')
    }

    // 延迟淡出加载遮罩，确保内容已渲染
    setTimeout(() => {
      if (loadingEl) {
        loadingEl.classList.add('fade-out')
        // 动画完成后移除加载遮罩
        setTimeout(() => {
          loadingEl.remove()
        }, 400)
      }
    }, 100)
  })

  // 未登录时跳转到登录页
  if (!userStore.isLoggedIn) {
    const currentPath = window.location.pathname
    if (currentPath !== '/login' && !currentPath.startsWith('/redirect')) {
      router.push('/login')
    }
  }
})
