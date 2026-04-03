---
name: 系统管理页面布局优化
description: 现代简约渐进式表格页面布局设计
type: project
---

# 系统管理页面布局优化设计

## 概述

对 gateway-admin-web 系统管理模块的表格页面进行现代简约风格优化，主要解决搜索区过高、按钮区占用空间大、分页位置不统一等问题。

## 设计决策

| 区域 | 决策 | 说明 |
|------|------|------|
| 搜索区 | 完全行内布局 | 搜索条件与按钮在同一行，自动换行 |
| 按钮区 | 扁平排列 | 所有按钮同行，间距紧凑 |
| 分页组件 | 右下角对齐 | 统一放在右下角 |
| 表格样式 | 内部无框 | 仅外边框，行间浅色底线分隔 |

## 核心尺寸规范

### 搜索卡片
- padding: `12px 16px`（原 16px）
- 按钮高度: `28px`（原 32px）
- 元素间距 gap: `12px`

### 按钮区
- padding: `8px 16px`（原 12px 16px）
- 按钮高度: `28px`（原 32px）
- 按钮间距 gap: `8px`

### 表格样式
- 边框: 仅外边框，圆角 `6px`
- 行分隔: 浅色底线 `#f1f5f9`
- 斑马纹背景: `#fafbfc`
- 表头背景: `#f8fafc`
- 单元格 padding: `10px 12px`

### 分页区
- padding: `8px 16px`（原 12px 16px）
- 位置: 右对齐 `justify-content: flex-end`
- 字体: `12px`

## 需要修改的文件

### 全局样式
- `src/styles/index.scss` - 修改 `.table-page-container` 样式

### 页面文件
- `src/views/system/user/index.vue`
- `src/views/system/role/index.vue`
- `src/views/system/menu/index.vue`
- `src/views/system/permission/index.vue`
- `src/views/system/operation-log/index.vue`
- `src/views/system/dict/data/index.vue`
- `src/views/system/dict/type/index.vue`
- `src/views/system/data-filter/index.vue`
- `src/views/system/config/index.vue`

## 实现要点

### 1. 搜索区改造
```vue
<!-- 改前：按钮独占一行 -->
<div class="search-buttons mb-3">
  <el-button>搜索</el-button>
  <el-button>重置</el-button>
</div>
<div class="search-conditions">...</div>

<!-- 改后：条件与按钮同行 -->
<div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
  <el-form-item>...</el-form-item>
  <el-button type="primary" style="height:28px">搜索</el-button>
  <el-button style="height:28px">重置</el-button>
</div>
```

### 2. 按钮区改造
```scss
.table-header {
  padding: 8px 16px;  // 原 12px 16px
  gap: 8px;
}

// 按钮高度
.el-button {
  height: 28px;  // 原 32px
  font-size: 13px;
  padding: 0 12px;
}
```

### 3. 表格样式
```scss
.el-table {
  border: 1px solid #e2e8f0;
  border-radius: 6px;

  // 移除内部边框
  .el-table__cell {
    border-bottom: 1px solid #f1f5f9;
  }

  // 斑马纹
  &.el-table--striped .el-table__body tr.el-table__row--striped td {
    background-color: #fafbfc;
  }
}
```

### 4. 分页位置
```scss
.pagination-wrapper {
  padding: 8px 16px;
  display: flex;
  justify-content: flex-end;
}
```

## 预期效果

- 垂直空间节省约 40px
- 表格内容区域占比增加 ~8%
- 视觉风格更现代简约
- 所有系统管理页面风格统一