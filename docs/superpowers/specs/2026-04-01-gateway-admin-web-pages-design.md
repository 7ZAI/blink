# gateway-admin-web 页面实现设计

## 概述

为 gateway-admin-web 新增权限管理、操作日志、设置和字典管理页面，直接复制 blink-base-web 的实现，后端 API 完全一致。

## 1. 页面清单

### 1.1 权限管理
- 路由：`/system/permission`
- 主文件：`views/system/permission/index.vue`
- 组件：
  - `PermissionFormDialog.vue` - 新增/编辑权限表单弹窗
  - `PermissionSelectDialog.vue` - 菜单选择弹窗（关联菜单）

### 1.2 操作日志
- 路由：`/system/operation-log`
- 主文件：`views/system/operation-log/index.vue`
- 组件：
  - `OperationLogDetailDialog.vue` - 日志详情弹窗

### 1.3 设置页面
- 路由：`/settings`
- 主文件：`views/settings/index.vue`
- 组件：
  - `ThemeEditor.vue` - 主题编辑器组件

### 1.4 字典类型管理
- 路由：`/system/dict/type`
- 主文件：`views/system/dict/type/index.vue`
- 组件：
  - `DictTypeFormDialog.vue` - 新增/编辑字典类型表单弹窗

### 1.5 字典数据管理
- 路由：`/system/dict/data`
- 主文件：`views/system/dict/data/index.vue`
- 组件：
  - `DictDataFormDialog.vue` - 新增/编辑字典数据表单弹窗

## 2. API 文件

新增以下 API 文件：

| 文件 | 内容 |
|------|------|
| `api/permission.ts` | 权限管理 CRUD：getPermissionList, addPermission, updatePermission, deletePermission, getMenuTreeForPermission |
| `api/operation-log.ts` | 操作日志查询：getOperationLogList, getOperationLogDetail, LogType枚举, logTypeOptions, executeStatusOptions |
| `api/dict.ts` | 字典类型/数据 CRUD：getDictTypeList, addDictType, updateDictType, deleteDictType, getDictDataList, getDictDataByType, addDictData, updateDictData, deleteDictData, getDictDataByTypes |

## 3. 国际化 (i18n)

在 `locales/zh-cn.ts` 和 `locales/en-us.ts` 中新增以下翻译键：

### 3.1 权限管理 (permission)
```typescript
permission: {
  title: '权限管理',
  acName: '权限名称',
  acEnName: '权限英文名',
  acIdentity: '权限标识',
  url: '接口地址',
  createTime: '创建时间',
  createBy: '创建人',
  deleteConfirm: '确定删除该权限吗？',
  addTitle: '新增权限',
  editTitle: '编辑权限',
  selectMenu: '选择菜单'
}
```

### 3.2 操作日志 (operationLog)
```typescript
operationLog: {
  title: '操作日志',
  list: '日志列表',
  operator: '操作人',
  logType: '日志类型',
  executeStatus: '执行状态',
  operationTime: '操作时间',
  requestUrl: '请求地址',
  executeTimeMs: '执行时长',
  ipAddress: 'IP地址',
  description: '操作描述',
  keywordPlaceholder: '请输入关键词',
  detailTitle: '日志详情',
  requestMethod: '请求方式',
  requestParams: '请求参数',
  responseData: '响应数据',
  errorMsg: '错误信息',
  userAgent: '浏览器'
}
```

### 3.3 设置页面 (settings)
```typescript
settings: {
  title: '系统设置',
  basicSettings: '基础设置',
  sidebarSettings: '侧边栏设置',
  theme: '主题设置',
  lightTheme: '浅色主题',
  darkTheme: '暗黑主题',
  language: '语言设置',
  chinese: '简体中文',
  sidebarWidth: '侧边栏宽度',
  to: '至',
  startDate: '开始日期',
  endDate: '结束日期'
}
```

### 3.4 字典管理 (dict)
```typescript
dict: {
  title: '字典管理',
  typeTitle: '字典类型',
  dataTitle: '字典数据',
  dictName: '字典名称',
  dictType: '字典类型编码',
  dictLabel: '字典标签',
  dictValue: '字典键值',
  dictSort: '字典排序',
  locale: '语言',
  statusEnable: '正常',
  statusDisable: '停用',
  isDefault: '是否默认',
  cssClass: '样式属性',
  listClass: '表格回显样式',
  deleteTypeConfirm: '确定删除该字典类型吗？',
  deleteDataConfirm: '确定删除该字典数据吗？',
  addTypeTitle: '新增字典类型',
  editTypeTitle: '编辑字典类型',
  addDataTitle: '新增字典数据',
  editDataTitle: '编辑字典数据'
}
```

## 4. 路由配置

### 4.1 系统管理子路由
在 `router/index.ts` 的 `system` children 中新增：

```typescript
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
}
```

### 4.2 设置页面路由
在根路由 children 中新增：

```typescript
{
  path: 'settings',
  name: 'Settings',
  component: () => import('@/views/settings/index.vue'),
  meta: { title: 'settings.title' }
}
```

## 5. 需要复制的支撑文件

### 5.1 stores
- `stores/theme.ts` - 主题状态管理（设置页面需要）

### 5.2 composables
- `composables/usePermission.ts` - 权限按钮控制（AuthButton 需要 ButtonPerms）
- `composables/useTransition.ts` - 数据加载过渡动画

### 5.3 components（如果不存在）
- `components/AuthButton.vue` - 权限按钮组件

## 6. 目录结构

最终目录结构：

```
blink-gateway/gateway-admin-web/src/
├── api/
│   ├── permission.ts        # 新增
│   ├── operation-log.ts     # 新增
│   └── dict.ts              # 新增
├── composables/
│   ├── usePermission.ts     # 新增（如不存在）
│   └── useTransition.ts     # 新增（如不存在）
├── components/
│   └── AuthButton.vue       # 新增（如不存在）
├── stores/
│   └── theme.ts             # 新增
├── views/
│   ├── settings/
│   │   ├── index.vue
│   │   └── components/
│   │       └── ThemeEditor.vue
│   └── system/
│       ├── permission/
│       │   ├── index.vue
│       │   └── components/
│       │       ├── PermissionFormDialog.vue
│       │       └── PermissionSelectDialog.vue
│       ├── operation-log/
│       │   ├── index.vue
│       │   └── components/
│       │       └── OperationLogDetailDialog.vue
│       └── dict/
│           ├── type/
│           │   ├── index.vue
│           │   └── components/
│           │       └── DictTypeFormDialog.vue
│           └── data/
│           │   ├── index.vue
│           │   └── components/
│           │       └── DictDataFormDialog.vue
├── locales/
│   ├── zh-cn.ts             # 更新：新增 permission, operationLog, settings, dict 翻译
│   └── en-us.ts             # 更新：新增对应英文翻译
└── router/
│   └── index.ts             # 更新：新增路由配置
```

## 7. 实现顺序

1. 复制支撑文件（stores, composables, components）
2. 复制 API 文件
3. 更新 i18n 翻译文件
4. 复制页面组件
5. 更新路由配置
6. 验证测试