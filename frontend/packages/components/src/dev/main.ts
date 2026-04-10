/**
 * 组件预览开发入口
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import routes from './routes'

// 导入组件库样式
import '../styles/index.scss'

// 导入组件库（用于演示）
import * as Components from '../lib-index'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const app = createApp(App)

// 注册所有组件（过滤掉非组件导出）
Object.entries(Components).forEach(([name, component]) => {
  // 只注册 Vue 组件（具有 name 属性或是函数组件）
  if (
    component &&
    typeof component === 'object' &&
    'name' in component &&
    typeof (component as any).name === 'string'
  ) {
    app.component(name, component as any)
  }
})

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')