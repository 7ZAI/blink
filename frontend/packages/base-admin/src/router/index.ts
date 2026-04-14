import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 路由加载进度条样式（注入到页面）
const injectLoadingStyle = () => {
  const style = document.createElement('style')
  style.textContent = `
    .route-loading-bar {
      position: fixed;
      top: 0;
      left: 0;
      height: 3px;
      background: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899, #3b82f6);
      background-size: 200% 100%;
      z-index: 9999;
      animation: route-loading 1.5s ease infinite, route-loading-width 0.3s ease-out forwards;
      box-shadow: 0 0 10px rgba(59, 130, 246, 0.5);
    }
    @keyframes route-loading {
      0% { background-position: 0% 0%; }
      100% { background-position: 200% 0%; }
    }
    @keyframes route-loading-width {
      0% { width: 0%; }
      100% { width: 80%; }
    }
    .route-loading-bar.complete {
      animation: route-loading-complete 0.3s ease-out forwards;
    }
    @keyframes route-loading-complete {
      0% { width: 80%; }
      100% { width: 100%; opacity: 0; }
    }
  `
  document.head.appendChild(style)
}

// 显示路由加载进度
const showRouteLoading = () => {
  let bar = document.querySelector('.route-loading-bar') as HTMLDivElement
  if (!bar) {
    bar = document.createElement('div')
    bar.className = 'route-loading-bar'
    document.body.appendChild(bar)
  } else {
    bar.className = 'route-loading-bar'
  }
  return bar
}

// 隐藏路由加载进度
const hideRouteLoading = () => {
  const bar = document.querySelector('.route-loading-bar')
  if (bar) {
    bar.classList.add('complete')
    setTimeout(() => bar.remove(), 300)
  }
}

// 初始化加载样式
injectLoadingStyle()

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { public: true },
    },
    {
      path: '/redirect/:path(.*)',
      name: 'Redirect',
      component: () => import('@/views/error/redirect.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/views/layout/index.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: 'menu.dashboard', icon: 'HomeFilled' },
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/index.vue'),
          meta: { title: 'menu.profile', icon: 'User' },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/settings/index.vue'),
          meta: { title: 'menu.settings', icon: 'Setting' },
        },
        {
          path: 'system',
          name: 'System',
          component: () => import('@/views/system/layout.vue'),
          meta: { title: 'menu.system', icon: 'Setting' },
          redirect: '/system/menu',
          children: [
            {
              path: 'user',
              name: 'SystemUserDir',
              component: () => import('@/views/system/user/layout.vue'),
              meta: { title: 'menu.user', icon: 'User' },
              redirect: '/system/user/list',
              children: [
                {
                  path: 'list',
                  name: 'SystemUserList',
                  component: () => import('@/views/system/user/index.vue'),
                  meta: { title: 'menu.userList', icon: 'User' },
                },
                {
                  path: 'online',
                  name: 'SystemOnlineUser',
                  component: () => import('@/views/system/online-user/index.vue'),
                  meta: { title: 'menu.onlineUser', icon: 'Monitor' },
                },
              ],
            },
            {
              path: 'menu',
              name: 'SystemMenu',
              component: () => import('@/views/system/menu/index.vue'),
              meta: { title: 'menu.menu', icon: 'Menu' },
            },
            {
              path: 'role',
              name: 'SystemRole',
              component: () => import('@/views/system/role/index.vue'),
              meta: { title: 'menu.role', icon: 'UserFilled' },
            },
            {
              path: 'group',
              name: 'SystemGroup',
              component: () => import('@/views/system/group/index.vue'),
              meta: { title: 'menu.group', icon: 'OfficeBuilding' },
            },
            {
              path: 'permission',
              name: 'SystemPermission',
              component: () => import('@/views/system/permission/layout.vue'),
              meta: { title: 'menu.permission', icon: 'Key' },
              redirect: '/system/permission/api-permission',
              children: [
                {
                  path: 'api-permission',
                  name: 'SystemApiPermission',
                  component: () => import('@/views/system/permission/index.vue'),
                  meta: { title: 'menu.apiPermission', icon: 'Connection', acType: 1 },
                },
                {
                  path: 'data-filter-permission',
                  name: 'SystemDataFilterPermission',
                  component: () => import('@/views/system/permission/layout.vue'),
                  meta: { title: 'menu.dataPermission', icon: 'DataAnalysis' },
                  redirect: '/system/permission/data-filter-permission/list',
                  children: [
                    {
                      path: 'list',
                      name: 'SystemDataPermissionList',
                      component: () => import('@/views/system/permission/index.vue'),
                      meta: { title: 'menu.dataPermissionList', icon: 'List', acType: 2 },
                    },
                    {
                      path: 'rule',
                      name: 'SystemDataFilterRule',
                      component: () => import('@/views/system/dataScope/index.vue'),
                      meta: { title: 'menu.dataFilterRule', icon: 'DataAnalysis' },
                    },
                  ],
                },
              ],
            },
            {
              path: 'config',
              name: 'SystemConfig',
              component: () => import('@/views/system/config/index.vue'),
              meta: { title: 'menu.config', icon: 'Tools' },
            },
            {
              path: 'dict',
              name: 'SystemDict',
              component: () => import('@/views/system/dict/layout.vue'),
              meta: { title: 'menu.dict', icon: 'Collection' },
              redirect: '/system/dict/type',
              children: [
                {
                  path: 'type',
                  name: 'SystemDictType',
                  component: () => import('@/views/system/dict/type/index.vue'),
                  meta: { title: 'menu.dictType', icon: 'CollectionTag' },
                },
                {
                  path: 'data',
                  name: 'SystemDictData',
                  component: () => import('@/views/system/dict/data/index.vue'),
                  meta: { title: 'menu.dictData', icon: 'List' },
                },
              ],
            },
            {
              path: 'operation-log',
              name: 'SystemOperationLog',
              component: () => import('@/views/system/operation-log/index.vue'),
              meta: { title: 'menu.operationLog', icon: 'Document' },
            },
          ],
        },
        {
          path: 'workflow',
          name: 'Workflow',
          component: () => import('@/views/workflow/layout.vue'),
          meta: { title: 'menu.workflow', icon: 'Share' },
          redirect: '/workflow/designer',
          children: [
            {
              path: 'designer',
              name: 'WorkflowDesigner',
              component: () => import('@/views/workflow/designer/index.vue'),
              meta: { title: 'menu.workflowDesigner', icon: 'Edit' },
            },
            {
              path: 'process',
              name: 'WorkflowProcess',
              component: () => import('@/views/workflow/process/index.vue'),
              meta: { title: 'menu.workflowProcess', icon: 'List' },
            },
            {
              path: 'task',
              name: 'WorkflowTask',
              component: () => import('@/views/workflow/task/index.vue'),
              meta: { title: 'menu.workflowTask', icon: 'Document' },
            },
          ],
        },
        {
          path: 'leave',
          name: 'Leave',
          component: () => import('@/views/leave/layout.vue'),
          meta: { title: 'menu.leave', icon: 'Calendar' },
          redirect: '/leave/my',
          children: [
            {
              path: 'my',
              name: 'LeaveMy',
              component: () => import('@/views/leave/my/index.vue'),
              meta: { title: 'menu.leaveMy', icon: 'User' },
            },
            {
              path: 'record',
              name: 'LeaveRecord',
              component: () => import('@/views/leave/record/index.vue'),
              meta: { title: 'menu.leaveRecord', icon: 'List' },
            },
          ],
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/404.vue'),
    },
  ],
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  // 显示路由加载进度
  showRouteLoading()

  const userStore = useUserStore()

  if (to.meta.public) {
    next()
    return
  }

  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }

  next()
})

// 路由加载完成后隐藏进度条
router.afterEach(() => {
  hideRouteLoading()
})

export default router
