# Blink Base Web

[English](./README.md) | 中文

基于 Vue 3、TypeScript 和 Element Plus 构建的企业级 RBAC（基于角色的访问控制）管理后台。

## 概述

Blink Base Web 是 Blink 微服务框架的现代管理仪表盘，提供完整的用户管理、基于角色的访问控制、菜单权限、工作流编排等功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5+ | 渐进式 JavaScript 框架 |
| TypeScript | 5.9+ | 类型安全的 JavaScript 超集 |
| Vite | 7.0+ | 下一代前端构建工具 |
| Element Plus | 2.13+ | Vue 3 组件库 |
| Pinia | 3.0+ | 直观的状态管理 |
| Vue Router | 4.6+ | 路由管理 |
| Vue I18n | 11.0+ | 国际化 |
| Axios | 1.13+ | HTTP 客户端 |
| SCSS | - | CSS 预处理器 |
| LogicFlow | 1.2+ | 流程图引擎 |
| tsParticles | 3.9+ | 粒子动画效果 |

## 功能特性

### 核心功能
- **用户管理** - 完整 CRUD、角色/组织分配、强制下线
- **角色管理** - 角色 CRUD、菜单/权限分配、角色详情
- **菜单管理** - 树形菜单管理、路由配置
- **权限管理** - 权限 CRUD、权限标识管理
- **组织管理** - 层级部门/组织结构
- **数据权限** - 细粒度数据权限配置，支持字段级过滤

### 系统配置
- **字典管理** - 字典类型和数据管理
- **系统参数** - 配置参数管理
- **操作日志** - 完整操作审计追踪

### 工作流引擎
- **流程设计器** - 可视化流程设计（LogicFlow）
- **流程实例** - 流程部署和运行监控
- **任务管理** - 待办任务和已办任务

### 用户体验
- **明暗主题切换** - 基于 CSS 变量的实时主题切换
- **国际化支持** - 完整 i18n（中文/英文）
- **响应式布局** - 适配各种屏幕尺寸
- **标签页导航** - 多标签页浏览模式
- **数据过渡动画** - 流畅的加载动画
- **Token 认证** - 安全令牌认证
- **动态路由** - 菜单驱动的动态路由生成
- **防止重复提交** - 内置提交守卫 composable
- **粒子特效** - 登录时的庆祝动画

## 快速开始

### 环境准备

- Node.js 18+
- npm 9+

### 安装依赖

```bash
cd blink-base/blink-base-web
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:5173

### 生产构建

```bash
npm run build
```

### 预览生产版本

```bash
npm run preview
```

## 项目结构

```
src/
├── api/                     # API 接口
│   ├── auth.ts              # 认证相关
│   ├── user.ts              # 用户管理
│   ├── role.ts              # 角色管理
│   ├── menu.ts              # 菜单管理
│   ├── permission.ts        # 权限管理
│   ├── group.ts             # 组织管理
│   ├── dict.ts              # 字典管理
│   ├── config.ts            # 系统配置
│   └── workflow/            # 工作流 API
├── assets/                  # 静态资源
│   ├── svg/                 # SVG 图标
│   └── avatar/              # 默认头像
├── components/              # 公共组件
│   ├── IconSelector/        # 图标选择器
│   ├── GroupSelector/       # 组织选择器
│   ├── RoleSelector/        # 角色选择器
│   ├── AvatarSelector/      # 头像选择器
│   ├── UserSelector/        # 用户选择器
│   └── PermissionSelector/  # 权限选择器
├── composables/             # 组合式函数
│   ├── useSubmitGuard.ts    # 防止重复提交
│   └── useDataTransition.ts # 数据过渡动画
├── config/                  # 配置
│   └── routes.ts           # 路由定义
├── directives/              # 自定义指令
├── locales/                 # 国际化
│   ├── zh-cn.ts            # 中文
│   └── en-us.ts            # 英文
├── router/                  # Vue Router 配置
├── stores/                  # Pinia 状态管理
│   ├── user.ts             # 用户状态
│   ├── permission.ts       # 权限状态
│   └── app.ts              # 应用状态（主题、侧边栏等）
├── styles/                  # 全局样式
│   ├── variables.scss      # SCSS 变量和 CSS 变量
│   └── index.css           # 基础样式
├── types/                   # TypeScript 类型定义
├── utils/                   # 工具函数
│   ├── request.ts          # Axios 封装（拦截器）
│   ├── avatar.ts          # 头像生成（DiceBear）
│   └── auth.ts            # 认证工具
└── views/                   # 页面组件
    ├── login/              # 登录页
    │   └── components/
    │       └── ResetPasswordDialog.vue
    ├── layout/              # 主布局
    ├── dashboard/           # 仪表盘
    ├── system/              # 系统管理
    │   ├── user/           # 用户管理
    │   ├── role/           # 角色管理
    │   ├── menu/           # 菜单管理
    │   ├── permission/     # 权限管理
    │   ├── group/          # 组织管理
    │   ├── dict/           # 字典管理
    │   ├── config/         # 系统配置
    │   ├── dataScope/      # 数据权限
    │   ├── operation-log/ # 操作日志
    │   └── online-user/    # 在线用户监控
    ├── workflow/            # 工作流模块
    │   ├── designer/      # 流程设计器
    │   ├── process/        # 流程实例
    │   └── task/           # 任务管理
    ├── profile/             # 个人中心
    ├── settings/           # 系统设置
    │   └── components/
    │       └── ThemeEditor.vue
    └── error/               # 错误页面
```

## 开发规范

### API 规范

所有 API 使用 POST 方法，参数放在 body 中：

```typescript
export const getUserList = (params: QueryUserParams): Promise<PageResult<UserInfo>> => {
  return request.post('/sysUser/getSysUserList', { body: params })
}
```

### 组件结构规范

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'ComponentName' })

const { t } = useI18n()
const loading = ref(false)
const dataList = ref<DataItem[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getData()
    dataList.value = res.rows
  } finally {
    loading.value = false
  }
}

onMounted(() => { fetchData() })
</script>
```

### 防止重复提交

使用内置的 `useSubmitGuard` composable：

```typescript
import { useSubmitGuard } from '@/composables/useSubmitGuard'

const { isSubmitting, submitGuard } = useSubmitGuard()

const handleSubmit = async () => {
  await submitGuard(async () => {
    await saveData(formData)
    ElMessage.success(t('message.success'))
  })
}
```

```vue
<el-button :loading="isSubmitting" @click="handleSubmit">
  {{ t('common.submit') }}
</el-button>
```

### 国际化规范

所有用户可见文本必须使用 i18n：

```vue
<template>
  <el-button>{{ t('common.search') }}</el-button>
  <el-table-column :label="t('user.loginName')" />
</template>

<script setup>
const { t } = useI18n()
ElMessage.success(t('message.saveSuccess'))
</script>
```

### 暗黑模式支持

使用 CSS 变量实现主题切换：

```scss
.component-name {
  background: var(--card-bg);
  border: 1px solid var(--border-color-light);
  color: var(--text-color-primary);
}
```

## 配置

### 环境变量

创建 `.env.development` 或 `.env.production`：

```
VITE_APP_TITLE=Blink Base
VITE_APP_API_BASE_URL=/api
VITE_APP_UPLOAD_URL=/api/sys-file/upload
```

## 相关模块

- [blink-base-app](https://github.com/blink/rbac-service) - 后端 RBAC 服务
- [blink-base-api-dubbo](https://github.com/blink/dubbo-api) - Dubbo 接口定义
- [blink-gateway-reactive](https://github.com/blink/gateway) - API 网关
- [blink-framework-common](https://github.com/blink/common) - 通用工具

## 许可证

MIT License

## 贡献指南

欢迎贡献代码！提交 PR 前请先阅读贡献指南。