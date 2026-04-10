# Blink Base Web

[English](./README.md) | [中文](./README.zh-CN.md)

Enterprise-grade RBAC (Role-Based Access Control) management frontend built with Vue 3, TypeScript, and Element Plus.

## Overview

Blink Base Web is a modern admin dashboard for the Blink microservice framework. It provides comprehensive user management, role-based access control, menu permissions, workflow orchestration, and more.

## Tech Stack

| Technology   | Version | Description                           |
| ------------ | ------- | ------------------------------------- |
| Vue          | 3.5+    | Progressive JavaScript framework      |
| TypeScript   | 5.9+    | Type-safe JavaScript superset         |
| Vite         | 7.0+    | Next generation frontend build tool   |
| Element Plus | 2.13+   | Vue 3 UI component library            |
| Pinia        | 3.0+    | Intuitive, type safe state management |
| Vue Router   | 4.6+    | Routing management                    |
| Vue I18n     | 11.0+   | Internationalization                  |
| Axios        | 1.13+   | HTTP client                           |
| SCSS         | -       | CSS preprocessor                      |
| LogicFlow    | 1.2+    | Process flowchart engine              |
| tsParticles  | 3.9+    | Particle animations                   |

## Features

### Core Capabilities

- **User Management** - Full CRUD with role/group assignment, force logout
- **Role Management** - Role CRUD with menu & permission assignment, role details viewer
- **Menu Management** - Tree-structured menu management with routing configuration
- **Permission Management** - Permission CRUD with identifier management
- **Organization Management** - Hierarchical department/group structure
- **Data Scope Control** - Fine-grained data permission configuration with field-level filtering

### System Configuration

- **Dictionary Management** - Dictionary type and data management
- **System Parameters** - Configuration parameter management
- **Operation Logs** - Full operation audit trail

### Workflow Engine

- **Process Designer** - Visual flow design with LogicFlow
- **Process Instances** - Deployment and runtime monitoring
- **Task Management** - Pending and completed tasks

### User Experience

- **Light/Dark Theme** - Real-time theme switching with CSS variables
- **Internationalization** - Full i18n support (Chinese/English)
- **Responsive Layout** - Adaptive to all screen sizes
- **Tab Navigation** - Multi-tab browser pattern
- **Data Transitions** - Smooth loading animations
- **Token Authentication** - Secure token-based auth
- **Dynamic Routing** - Menu-driven route generation
- **Duplicate Submission Prevention** - Built-in submit guard composable
- **Particle Effects** - Celebration animations on login

## Quick Start

### Prerequisites

- Node.js 18+
- npm 9+

### Installation

```bash
cd blink-base/blink-base-web
npm install
```

### Development

```bash
npm run dev
```

Visit http://localhost:5173

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## Project Structure

```
src/
├── api/                     # API interfaces
│   ├── auth.ts              # Authentication
│   ├── user.ts              # User management
│   ├── role.ts              # Role management
│   ├── menu.ts              # Menu management
│   ├── permission.ts        # Permission management
│   ├── group.ts             # Organization/group
│   ├── dict.ts              # Dictionary
│   ├── config.ts            # System config
│   └── workflow/            # Workflow APIs
├── assets/                  # Static resources
│   ├── svg/                 # SVG icons
│   └── avatar/              # Default avatars
├── components/              # Shared components
│   ├── IconSelector/         # Icon picker
│   ├── GroupSelector/       # Organization picker
│   ├── RoleSelector/        # Role picker
│   ├── AvatarSelector/      # Avatar picker
│   ├── UserSelector/        # User picker
│   └── PermissionSelector/  # Permission picker
├── composables/             # Composable functions
│   ├── useSubmitGuard.ts    # Duplicate submission prevention
│   └── useDataTransition.ts # Data transition animations
├── config/                  # Configuration
│   └── routes.ts            # Route definitions
├── directives/              # Custom directives
├── locales/                 # i18n translations
│   ├── zh-cn.ts             # Chinese
│   └── en-us.ts             # English
├── router/                  # Vue Router setup
├── stores/                  # Pinia stores
│   ├── user.ts              # User state
│   ├── permission.ts        # Permission state
│   └── app.ts               # App state (theme, sidebar, etc.)
├── styles/                  # Global styles
│   ├── variables.scss       # SCSS variables & CSS variables
│   └── index.css            # Base styles
├── types/                   # TypeScript type definitions
├── utils/                   # Utility functions
│   ├── request.ts           # Axios wrapper with interceptors
│   ├── avatar.ts            # Avatar generation (DiceBear)
│   └── auth.ts              # Auth utilities
└── views/                   # Page components
    ├── login/               # Login page
    │   └── components/
    │       └── ResetPasswordDialog.vue
    ├── layout/              # Main layout
    ├── dashboard/            # Dashboard
    ├── system/               # System management
    │   ├── user/            # User management
    │   ├── role/            # Role management
    │   ├── menu/            # Menu management
    │   ├── permission/      # Permission management
    │   ├── group/           # Organization management
    │   ├── dict/            # Dictionary management
    │   ├── config/          # System config
    │   ├── dataScope/       # Data scope/permission
    │   ├── operation-log/   # Operation logs
    │   └── online-user/     # Online user monitoring
    ├── workflow/            # Workflow module
    │   ├── designer/        # Process designer
    │   ├── process/         # Process instances
    │   └── task/            # Task management
    ├── profile/             # User profile
    ├── settings/            # Settings page
    │   └── components/
    │       └── ThemeEditor.vue
    └── error/               # Error pages
```

## Development Standards

### API Specification

All APIs use POST method with body-wrapped parameters:

```typescript
export const getUserList = (params: QueryUserParams): Promise<PageResult<UserInfo>> => {
  return request.post('/sysUser/getSysUserList', { body: params })
}
```

### Component Structure

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

onMounted(() => {
  fetchData()
})
</script>
```

### Duplicate Submission Prevention

Use the built-in `useSubmitGuard` composable:

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

### Internationalization

All user-visible text must use i18n:

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

### Dark Mode Support

Use CSS variables for theming:

```scss
.component-name {
  background: var(--card-bg);
  border: 1px solid var(--border-color-light);
  color: var(--text-color-primary);
}
```

## Configuration

### Environment Variables

Create `.env.development` or `.env.production`:

```
VITE_APP_TITLE=Blink Base
VITE_APP_API_BASE_URL=/api
VITE_APP_UPLOAD_URL=/api/sys-file/upload
```

## Related Modules

- [blink-base-app](https://github.com/blink/rbac-service) - Backend RBAC service
- [blink-base-api-dubbo](https://github.com/blink/dubbo-api) - Dubbo interface definitions
- [blink-gateway-reactive](https://github.com/blink/gateway) - API Gateway
- [blink-framework-common](https://github.com/blink/common) - Common utilities

## License

MIT License

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.
