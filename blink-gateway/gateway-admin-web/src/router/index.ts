import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: 'login.title' }
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
        meta: { title: 'dashboard.title' }
      },
      {
        path: 'channel',
        name: 'Channel',
        component: () => import('@/views/channel/index.vue'),
        meta: { title: 'channel.title' }
      },
      {
        path: 'route',
        name: 'Route',
        component: () => import('@/views/route/index.vue'),
        meta: { title: 'route.title' }
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('@/views/config/index.vue'),
        meta: { title: 'config.title' }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/index.vue'),
        meta: { title: 'monitor.title' }
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
            meta: { title: 'system.user.title' }
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/views/system/role/index.vue'),
            meta: { title: 'system.role.title' }
          },
          {
            path: 'menu',
            name: 'SystemMenu',
            component: () => import('@/views/system/menu/index.vue'),
            meta: { title: 'system.menu.title' }
          },
          {
            path: 'permission',
            name: 'SystemPermission',
            component: () => import('@/views/system/permission/index.vue'),
            meta: { title: 'system.permission.title' }
          },
          {
            path: 'operation-log',
            name: 'SystemOperationLog',
            component: () => import('@/views/system/operation-log/index.vue'),
            meta: { title: 'system.operationLog.title' }
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
                meta: { title: 'system.dict.typeTitle' }
              },
              {
                path: 'data',
                name: 'SystemDictData',
                component: () => import('@/views/system/dict/data/index.vue'),
                meta: { title: 'system.dict.dataTitle' }
              }
            ]
          },
          {
            path: 'data-filter',
            name: 'SystemDataFilter',
            component: () => import('@/views/system/data-filter/index.vue'),
            meta: { title: 'dataScope.title' }
          },
          {
            path: 'config',
            name: 'SystemConfig',
            component: () => import('@/views/system/config/index.vue'),
            meta: { title: 'systemConfig.title' }
          }
        ]
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: { title: 'settings.title' }
      }
    ]
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { public: true }
  },
  {
    path: '/500',
    name: 'ServerError',
    component: () => import('@/views/error/500.vue'),
    meta: { public: true }
  },
  {
    path: '/redirect/:path(.*)',
    name: 'Redirect',
    component: () => import('@/views/error/redirect.vue'),
    meta: { hidden: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 认证检查与菜单加载
router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  const isPublic = to.meta.public

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

  // 只有当 userInfo 为空时才需要重新获取用户信息
  // 登录成功后 userInfo 会被设置，无需再次获取
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      console.error('Failed to fetch user info:', error)
      // 用户信息获取失败可能是 token 过期，跳转到登录页
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  next()
})

export default router