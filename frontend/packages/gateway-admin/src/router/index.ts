import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useDictStore } from '@/stores/dict'

// 简约现代清新风格的路由加载样式
const injectLoadingStyle = () => {
  const style = document.createElement('style')
  style.textContent = `
    /* 简约进度条 - 使用主题色 */
    .route-loading-bar {
      position: fixed;
      top: 0;
      left: 0;
      height: 2px;
      background: var(--primary-color, #10b981);
      z-index: 9999;
      transition: width 0.3s ease-out, opacity 0.2s ease-out;
      box-shadow: 0 0 8px var(--primary-color, #10b981);
      border-radius: 0 2px 2px 0;
    }
    .route-loading-bar::after {
      content: '';
      position: absolute;
      right: 0;
      top: 0;
      height: 100%;
      width: 40px;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3));
      animation: loading-shimmer 1s ease-in-out infinite;
    }
    @keyframes loading-shimmer {
      0%, 100% { opacity: 0; }
      50% { opacity: 1; }
    }
    .route-loading-bar.complete {
      width: 100%;
      opacity: 0;
    }

    /* 全屏加载遮罩 - 简约清新风格 */
    .route-loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.95);
      z-index: 9998;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      transition: opacity 0.3s ease-out;
    }
    .dark .route-loading-overlay {
      background: rgba(15, 23, 42, 0.95);
    }
    .route-loading-overlay.fade-out {
      opacity: 0;
    }

    /* 简约动画 logo */
    .route-loading-logo {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 32px;
    }
    .route-loading-logo-icon {
      width: 48px;
      height: 48px;
      background: var(--primary-color, #10b981);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      animation: logo-pulse 2s ease-in-out infinite;
    }
    .route-loading-logo-icon svg {
      width: 24px;
      height: 24px;
      color: white;
    }
    @keyframes logo-pulse {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.05); }
    }
    .route-loading-logo-text {
      font-size: 24px;
      font-weight: 600;
      color: var(--el-text-color-primary, #1f2937);
      letter-spacing: -0.5px;
    }
    .dark .route-loading-logo-text {
      color: #f1f5f9;
    }

    /* 简约加载动画 - 三个小圆点 */
    .route-loading-dots {
      display: flex;
      gap: 8px;
    }
    .route-loading-dot {
      width: 8px;
      height: 8px;
      background: var(--primary-color, #10b981);
      border-radius: 50%;
      animation: dot-bounce 1.4s ease-in-out infinite;
    }
    .route-loading-dot:nth-child(1) { animation-delay: 0s; }
    .route-loading-dot:nth-child(2) { animation-delay: 0.2s; }
    .route-loading-dot:nth-child(3) { animation-delay: 0.4s; }
    @keyframes dot-bounce {
      0%, 80%, 100% {
        transform: scale(0.6);
        opacity: 0.5;
      }
      40% {
        transform: scale(1);
        opacity: 1;
      }
    }

    /* 加载提示文字 */
    .route-loading-text {
      margin-top: 16px;
      font-size: 14px;
      color: var(--el-text-color-secondary, #6b7280);
      letter-spacing: 0.5px;
    }
    .dark .route-loading-text {
      color: #94a3b8;
    }
  `
  document.head.appendChild(style)
}

// 显示路由加载进度条
const showRouteLoadingBar = () => {
  let bar = document.querySelector('.route-loading-bar') as HTMLDivElement
  if (!bar) {
    bar = document.createElement('div')
    bar.className = 'route-loading-bar'
    bar.style.width = '0%'
    document.body.appendChild(bar)
  } else {
    bar.className = 'route-loading-bar'
    bar.style.width = '0%'
    bar.style.opacity = '1'
  }

  // 模拟进度
  setTimeout(() => {
    bar.style.width = '30%'
  }, 100)
  setTimeout(() => {
    bar.style.width = '60%'
  }, 300)

  return bar
}

// 隐藏路由加载进度条
const hideRouteLoadingBar = () => {
  const bar = document.querySelector('.route-loading-bar')
  if (bar) {
    ;(bar as HTMLDivElement).style.width = '100%'
    setTimeout(() => {
      bar.classList.add('complete')
      setTimeout(() => bar.remove(), 300)
    }, 100)
  }
}

// 获取加载文字（根据当前语言设置）
const getLoadingText = (
  key: 'loadingData' | 'initializing' | 'pleaseWait' = 'loadingData'
): string => {
  const lang = localStorage.getItem('language') || 'zh-cn'
  const defaultTexts: Record<string, string> = {
    loadingData: '正在加载数据...',
    initializing: '正在初始化...',
    pleaseWait: '请稍候',
  }
  const texts: Record<string, Record<string, string>> = {
    'zh-cn': defaultTexts,
    'en-us': {
      loadingData: 'Loading data...',
      initializing: 'Initializing...',
      pleaseWait: 'Please wait',
    },
  }
  const selectedTexts = texts[lang]
  if (selectedTexts && selectedTexts[key]) {
    return selectedTexts[key]
  }
  return defaultTexts[key] || '正在加载数据...'
}

// 显示全屏加载遮罩（用于登录后的数据加载）
const showLoadingOverlay = (text?: string) => {
  let overlay = document.querySelector('.route-loading-overlay') as HTMLDivElement
  if (!overlay) {
    overlay = document.createElement('div')
    overlay.className = 'route-loading-overlay'
    overlay.innerHTML = `
      <div class="route-loading-logo">
        <div class="route-loading-logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <span class="route-loading-logo-text">Gateway Admin</span>
      </div>
      <div class="route-loading-dots">
        <div class="route-loading-dot"></div>
        <div class="route-loading-dot"></div>
        <div class="route-loading-dot"></div>
      </div>
      <p class="route-loading-text">${text || getLoadingText('loadingData')}</p>
    `
    document.body.appendChild(overlay)
  }
  return overlay
}

// 隐藏全屏加载遮罩
const hideLoadingOverlay = () => {
  const overlay = document.querySelector('.route-loading-overlay')
  if (overlay) {
    overlay.classList.add('fade-out')
    setTimeout(() => overlay.remove(), 300)
  }
}

// 初始化加载样式
injectLoadingStyle()

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: 'login.title' },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: 'dashboard.title' },
      },
      {
        path: 'channel',
        name: 'Channel',
        component: () => import('@/views/channel/index.vue'),
        meta: { title: 'channel.title' },
      },
      {
        path: 'route',
        name: 'RouteManagement',
        redirect: '/route/repository',
        meta: { title: 'route.title' },
        children: [
          {
            path: 'repository',
            name: 'RouteRepository',
            component: () => import('@/views/route/index.vue'),
            meta: { title: 'route.repositoryTitle' },
          },
          {
            path: 'push',
            name: 'PushRoute',
            component: () => import('@/views/pushRoute/index.vue'),
            meta: { title: 'pushRoute.title' },
          },
          {
            path: 'instance',
            name: 'InstanceRoute',
            component: () => import('@/views/instanceRoute/index.vue'),
            meta: { title: 'instanceRoute.title' },
          },
          {
            path: 'push-history',
            name: 'PushHistory',
            component: () => import('@/views/pushHistory/index.vue'),
            meta: { title: 'pushHistory.title' },
          },
        ],
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('@/views/config/index.vue'),
        meta: { title: 'config.title' },
      },
      {
        path: 'monitor',
        name: 'Monitor',
        redirect: '/monitor/dashboard',
        meta: { title: 'monitor.title' },
        children: [
          {
            path: 'dashboard',
            name: 'MonitorDashboard',
            component: () => import('@/views/monitor/index.vue'),
            meta: { title: 'monitor.dashboard' },
          },
          {
            path: 'alert-rule',
            name: 'MonitorAlertRule',
            component: () => import('@/views/alert/rules/index.vue'),
            meta: { title: 'monitor.alertRule' },
          },
          {
            path: 'alert-history',
            name: 'MonitorAlertHistory',
            component: () => import('@/views/alert/history/index.vue'),
            meta: { title: 'monitor.alertHistory' },
          },
          {
            path: 'circuit-breaker',
            name: 'MonitorCircuitBreaker',
            component: () => import('@/views/monitor/circuitBreaker/index.vue'),
            meta: { title: 'monitor.circuitBreaker' },
          },
        ],
      },
      {
        path: 'instance',
        name: 'Instance',
        component: () => import('@/views/instance/index.vue'),
        meta: { title: 'instance.title' },
      },
      {
        path: 'instance/detail',
        name: 'InstanceDetail',
        component: () => import('@/views/instance/detail/index.vue'),
        meta: { title: 'instance.detailTitle', hidden: true },
      },
      {
        path: 'dataSync',
        name: 'DataSync',
        component: () => import('@/views/dataSync/index.vue'),
        meta: { title: 'dataSync.title' },
      },
      {
        path: 'system',
        redirect: '/system/user',
        meta: { title: 'system.title' },
        children: [
          {
            path: 'user',
            name: 'SystemUser',
            component: () => import('@/views/system/user/index.vue'),
            meta: { title: 'system.user.title' },
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/views/system/role/index.vue'),
            meta: { title: 'system.role.title' },
          },
          {
            path: 'menu',
            name: 'SystemMenu',
            component: () => import('@/views/system/menu/index.vue'),
            meta: { title: 'system.menu.title' },
          },
          {
            path: 'permission',
            name: 'SystemPermissionDir',
            component: () => import('@/views/system/permission/layout.vue'),
            meta: { title: 'system.permission.title' },
            redirect: '/system/permission/api-permission',
            children: [
              {
                path: 'api-permission',
                name: 'SystemApiPermission',
                component: () => import('@/views/system/permission/index.vue'),
                meta: { title: 'system.permission.apiPermission', acType: 1 },
              },
              {
                path: 'data-permission',
                name: 'SystemDataPermissionDir',
                component: () => import('@/views/system/data-permission/layout.vue'),
                meta: { title: 'system.permission.dataPermission' },
                redirect: '/system/permission/data-permission/list',
                children: [
                  {
                    path: 'list',
                    name: 'SystemDataPermissionList',
                    component: () => import('@/views/system/permission/index.vue'),
                    meta: { title: 'system.permission.dataPermissionList', acType: 2 },
                  },
                  {
                    path: 'rule',
                    name: 'SystemDataFilterRule',
                    component: () => import('@/views/system/data-filter/index.vue'),
                    meta: { title: 'system.permission.dataFilterRule' },
                  },
                ],
              },
            ],
          },
          {
            path: 'operation-log',
            name: 'SystemOperationLog',
            component: () => import('@/views/system/operation-log/index.vue'),
            meta: { title: 'system.operationLog.title' },
          },
          {
            path: 'dict',
            redirect: '/system/dict/type',
            meta: { title: 'system.dict.title' },
            children: [
              {
                path: 'type',
                name: 'SystemDictType',
                component: () => import('@/views/system/dict/type/index.vue'),
                meta: { title: 'system.dict.typeTitle' },
              },
              {
                path: 'data',
                name: 'SystemDictData',
                component: () => import('@/views/system/dict/data/index.vue'),
                meta: { title: 'system.dict.dataTitle' },
              },
            ],
          },
          {
            path: 'config',
            name: 'SystemConfig',
            component: () => import('@/views/system/config/index.vue'),
            meta: { title: 'systemConfig.title' },
          },
        ],
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: { title: 'settings.title' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: 'header.profile' },
      },
      {
        path: 'notification',
        name: 'Notification',
        component: () => import('@/views/notification/index.vue'),
        meta: { title: 'notification.title' },
      },
    ],
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { public: true },
  },
  {
    path: '/500',
    name: 'ServerError',
    component: () => import('@/views/error/500.vue'),
    meta: { public: true },
  },
  {
    path: '/redirect/:path(.*)',
    name: 'Redirect',
    component: () => import('@/views/error/redirect.vue'),
    meta: { hidden: true },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫 - 认证检查与菜单加载
router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  const isPublic = to.meta.public

  // 显示进度条
  showRouteLoadingBar()

  if (isPublic) {
    next()
    return
  }

  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 刷新页面后重新获取用户信息和菜单数据
  const userStore = useUserStore()
  const dictStore = useDictStore()

  // 常用字典类型列表
  const commonDictTypes = [
    'sys_normal_status',
    'sys_sex',
    'sys_menu_type',
    'sys_show_status',
    'sys_yes_no',
  ]

  // 只有当 userInfo 为空时才需要重新获取用户信息
  // 登录成功后 userInfo 会被设置，无需再次获取
  if (!userStore.userInfo) {
    // 显示全屏加载遮罩
    showLoadingOverlay(getLoadingText('loadingData'))
    try {
      await userStore.fetchUserInfo()

      // 预加载常用字典数据
      await dictStore.loadDictData(commonDictTypes)

      // 隐藏全屏遮罩
      hideLoadingOverlay()
    } catch (error) {
      console.error('Failed to fetch user info:', error)
      hideLoadingOverlay()
      // 用户信息获取失败可能是 token 过期，跳转到登录页
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  next()
})

// 路由加载完成后隐藏进度条
router.afterEach(() => {
  hideRouteLoadingBar()
})

// 路由错误时也隐藏加载效果
router.onError(() => {
  hideRouteLoadingBar()
  hideLoadingOverlay()
})

// 导出加载控制函数供其他组件使用
export { showLoadingOverlay, hideLoadingOverlay, showRouteLoadingBar, hideRouteLoadingBar }

export default router
