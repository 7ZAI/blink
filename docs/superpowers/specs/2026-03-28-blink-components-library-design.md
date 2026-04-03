# @blink/components 组件库设计

## 1. 概述

### 1.1 背景

blink-base-web 中包含大量可复用的组件、composables 和 directives，需要提取为独立的组件库，供 gateway-admin-web 等其他前端项目使用。

### 1.2 目标

- 将 blink-base-web 的**通用组件**（无业务依赖）打包为 npm 包 `@blink/components`
- 支持本地 npm pack 发布
- 样式由消费者自行配置 TailwindCSS，组件库不打包样式
- 完整的 TypeScript 类型支持

### 1.3 范围

| 包含内容 | 不包含内容 |
|----------|------------|
| SkeletonLoader | DictSelect/DictTag（依赖业务 store） |
| BlinkIcon | AuthButton（依赖业务 store） |
| Breadcrumb | TabsView（依赖业务 store） |
| IconSelector | RoleSelector（依赖业务 API） |
| useTransition | GroupSelector（依赖业务 API） |
| useSubmitGuard | AvatarSelector（依赖业务 utils） |
| dataFadeDirective | BackgroundEffects（依赖业务 store） |
| rippleDirective | LogicFlowDesigner（依赖业务 API） |
| | usePermission（依赖业务 store） |
| | useDict（依赖业务 store） |
| | authDirective（依赖业务 store） |

---

## 2. 技术方案

### 2.1 构建工具

使用 Vite 库模式构建，配合 `vite-plugin-dts` 生成类型定义。

### 2.2 输出格式

- ES Module: `dist/index.js`
- CommonJS: `dist/index.cjs`
- 类型定义: `dist/lib-index.d.ts` 及相关组件类型

### 2.3 依赖处理

以下依赖作为 `peerDependencies`，不打包进库：

| 依赖 | 版本要求 |
|------|----------|
| vue | ^3.5.0 |
| vue-router | ^4.6.0 |
| pinia | ^3.0.0 |
| element-plus | ^2.13.0 |
| @element-plus/icons-vue | ^2.3.0 |
| @iconify/vue | ^5.0.0 |

---

## 3. 库入口设计

### 3.1 入口文件

创建 `src/lib-index.ts`：

```ts
// src/lib-index.ts

// ============================================
// 组件导出
// ============================================

// 布局组件
export { default as Breadcrumb } from './components/Breadcrumb/index.vue'

// 选择器组件
export { default as IconSelector } from './components/IconSelector/index.vue'

// 功能组件
export { default as SkeletonLoader } from './components/SkeletonLoader/index.vue'
export { default as BlinkIcon } from './components/BlinkIcon/index.vue'

// ============================================
// Composables 导出
// ============================================

export { useTransition, useFadeIn } from './composables/useDataTransition'
export { useSubmitGuard } from './composables/useSubmitGuard'

// ============================================
// Directives 导出
// ============================================

export { dataFadeDirective, listFadeDirective, tableFadeDirective } from './directives/dataFade'
export { default as rippleDirective } from './directives/ripple'
```

### 3.2 样式处理

组件库**不打包样式**。消费者需要：

1. 配置 TailwindCSS（gateway-admin-web 已配置）
2. 确保 `tailwind.config.js` 扫描到组件库路径：
   ```js
   content: [
     "./node_modules/@blink/components/**/*.{vue,js,ts}"
   ]
   ```

---

## 4. 构建配置

### 4.1 vite.lib.config.ts

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import dts from 'vite-plugin-dts'

export default defineConfig({
  plugins: [
    vue(),
    dts({
      tsconfigPath: './tsconfig.app.json',
      outDir: 'dist',
      include: ['src/lib-index.ts', 'src/components/**/*.vue', 'src/composables/**/*.ts', 'src/directives/**/*.ts'],
      exclude: ['node_modules/**', 'src/views/**'],
    })
  ],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/lib-index.ts'),
      name: 'BlinkComponents',
      formats: ['es', 'cjs'],
      fileName: (format) => `index.${format === 'es' ? 'js' : 'cjs'}`
    },
    rollupOptions: {
      external: [
        'vue',
        'vue-router',
        'pinia',
        'element-plus',
        '@element-plus/icons-vue',
        '@iconify/vue',
        'vue-i18n'
      ],
      output: {
        globals: {
          vue: 'Vue',
          'vue-router': 'VueRouter',
          pinia: 'Pinia',
          'element-plus': 'ElementPlus',
          'vue-i18n': 'VueI18n',
        }
      }
    },
    cssCodeSplit: false,
  }
})
```

### 4.2 package.json 变更

```json
{
  "name": "@blink/components",
  "version": "1.0.0",
  "type": "module",
  "main": "./dist/index.cjs",
  "module": "./dist/index.js",
  "types": "./dist/lib-index.d.ts",
  "exports": {
    ".": {
      "types": "./dist/lib-index.d.ts",
      "import": "./dist/index.js",
      "require": "./dist/index.cjs"
    }
  },
  "files": [
    "dist"
  ],
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "build:lib": "vite build --config vite.lib.config.ts",
    "pack:lib": "npm run build:lib && npm pack"
  },
  "peerDependencies": {
    "vue": "^3.5.0",
    "vue-router": "^4.6.0",
    "pinia": "^3.0.0",
    "element-plus": "^2.13.0",
    "@element-plus/icons-vue": "^2.3.0",
    "@iconify/vue": "^5.0.0"
  },
  "devDependencies": {
    "vite-plugin-dts": "^4.0.0"
  }
}
```

---

## 5. 使用方式

### 5.1 安装

```bash
# 方式一：安装 tgz 文件
cd blink-gateway/gateway-admin-web
npm install ../../blink-base/blink-base-web/blink-components-1.0.0.tgz

# 方式二：使用本地路径
# package.json 中添加:
# "@blink/components": "file:../../blink-base/blink-base-web"
```

### 5.2 使用组件

```vue
<script setup lang="ts">
import { BlinkIcon, SkeletonLoader, useTransition } from '@blink/components'

const { transitionClass, finishTransition } = useTransition()
</script>

<template>
  <BlinkIcon icon="mdi:home" size="20" />
  <SkeletonLoader type="table" :rows="5" />
</template>
```

### 5.3 使用指令

```ts
// main.ts
import { dataFadeDirective, rippleDirective } from '@blink/components'

app.directive('data-fade', dataFadeDirective)
app.directive('ripple', rippleDirective)
```

---

## 6. 文件变更清单

| 操作 | 文件路径 |
|------|----------|
| 新建 | `src/lib-index.ts` |
| 新建 | `vite.lib.config.ts` |
| 修改 | `package.json` |
| 修改 | `tsconfig.app.json` |
| 安装 | `vite-plugin-dts` |

---

## 7. 验证步骤

1. 构建库：`npm run build:lib`
2. 检查产物：`ls dist/` 应看到 `index.js`, `index.cjs`, `lib-index.d.ts`
3. 打包：`npm run pack:lib` 生成 `blink-components-1.0.0.tgz`
4. 在 gateway-admin-web 中安装并测试

---

## 8. 注意事项

### 8.1 业务组件限制

由于大部分组件依赖 blink-base-web 的业务 stores（如 `useUserStore`, `useDictStore`）和 API（如 `@/api/role`），这些组件**无法**打包到组件库中。

如果 gateway-admin-web 需要这些组件，有以下方案：

1. **复制代码**：直接复制组件代码到 gateway-admin-web，适配其 stores 和 API
2. **重构组件**：将依赖通过 props 传入，使组件不依赖具体 store
3. **约定接口**：两个项目使用相同的 store 接口

### 8.2 已知问题

- `components.css` 文件会被生成，但实际不包含有用样式
- 部分未导出的模块类型定义也会被打包（如 `useDict.d.ts`），但不影响使用