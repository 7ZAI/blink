/**
 * 组件预览路由配置
 */
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('./layout/MainLayout.vue'),
    redirect: '/overview',
    children: [
      {
        path: 'overview',
        name: 'Overview',
        component: () => import('./views/Overview.vue'),
        meta: { title: '组件概览', icon: 'HomeFilled' },
      },
      // 布局组件
      {
        path: 'layout',
        name: 'Layout',
        meta: { title: '布局组件', icon: 'Grid' },
        children: [
          {
            path: 'main-layout',
            name: 'MainLayout',
            component: () => import('./views/layout/MainLayoutDemo.vue'),
            meta: { title: '主布局' },
          },
          {
            path: 'sidebar',
            name: 'Sidebar',
            component: () => import('./views/layout/SidebarDemo.vue'),
            meta: { title: '侧边栏' },
          },
          {
            path: 'header',
            name: 'Header',
            component: () => import('./views/layout/HeaderDemo.vue'),
            meta: { title: '头部导航' },
          },
          {
            path: 'tabs-view',
            name: 'TabsView',
            component: () => import('./views/layout/TabsViewDemo.vue'),
            meta: { title: '标签页' },
          },
          {
            path: 'breadcrumb',
            name: 'Breadcrumb',
            component: () => import('./views/layout/BreadcrumbDemo.vue'),
            meta: { title: '面包屑' },
          },
        ],
      },
      // 功能组件
      {
        path: 'functional',
        name: 'Functional',
        meta: { title: '功能组件', icon: 'Tools' },
        children: [
          {
            path: 'theme-toggle',
            name: 'ThemeToggle',
            component: () => import('./views/functional/ThemeToggleDemo.vue'),
            meta: { title: '主题切换' },
          },
          {
            path: 'language-switch',
            name: 'LanguageSwitch',
            component: () => import('./views/functional/LanguageSwitchDemo.vue'),
            meta: { title: '语言切换' },
          },
          {
            path: 'fullscreen-toggle',
            name: 'FullscreenToggle',
            component: () => import('./views/functional/FullscreenToggleDemo.vue'),
            meta: { title: '全屏切换' },
          },
          {
            path: 'theme-settings',
            name: 'ThemeSettings',
            component: () => import('./views/functional/ThemeSettingsDemo.vue'),
            meta: { title: '主题设置' },
          },
          {
            path: 'captcha-slider',
            name: 'CaptchaSlider',
            component: () => import('./views/functional/CaptchaSliderDemo.vue'),
            meta: { title: '滑块验证码' },
          },
          {
            path: 'icon-selector',
            name: 'IconSelector',
            component: () => import('./views/functional/IconSelectorDemo.vue'),
            meta: { title: '图标选择器' },
          },
        ],
      },
      // 业务组件
      {
        path: 'business',
        name: 'Business',
        meta: { title: '业务组件', icon: 'Briefcase' },
        children: [
          {
            path: 'blink-dialog',
            name: 'BlinkDialog',
            component: () => import('./views/business/BlinkDialogDemo.vue'),
            meta: { title: '对话框' },
          },
          {
            path: 'blink-table',
            name: 'BlinkTable',
            component: () => import('./views/business/BlinkTableDemo.vue'),
            meta: { title: '表格' },
          },
          {
            path: 'user-dropdown',
            name: 'UserDropdown',
            component: () => import('./views/business/UserDropdownDemo.vue'),
            meta: { title: '用户下拉' },
          },
          {
            path: 'blink-task-dialog',
            name: 'BlinkTaskDialog',
            component: () => import('./views/business/BlinkTaskDialogDemo.vue'),
            meta: { title: '任务进度弹窗' },
          },
        ],
      },
      // 测试工具
      {
        path: 'testing',
        name: 'Testing',
        meta: { title: '测试工具', icon: 'Monitor' },
        children: [
          {
            path: 'coverage',
            name: 'Coverage',
            component: () => import('./views/testing/Coverage.vue'),
            meta: { title: '测试覆盖率' },
          },
          {
            path: 'component-test',
            name: 'ComponentTest',
            component: () => import('./views/testing/ComponentTest.vue'),
            meta: { title: '组件测试' },
          },
        ],
      },
    ],
  },
]

export default routes