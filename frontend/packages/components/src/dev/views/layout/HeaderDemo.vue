<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>头部导航 (Header)</h2>
      <p>顶部导航栏组件，包含面包屑、功能按钮和用户信息</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <div class="header-preview">
        <div class="header-demo">
          <div class="header-left">
            <el-icon class="toggle-btn"><Fold /></el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>首页</el-breadcrumb-item>
              <el-breadcrumb-item>系统管理</el-breadcrumb-item>
              <el-breadcrumb-item>用户管理</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-tooltip content="搜索">
              <el-button :icon="Search" circle />
            </el-tooltip>
            <el-tooltip content="全屏">
              <el-button :icon="FullScreen" circle />
            </el-tooltip>
            <el-tooltip content="消息">
              <el-badge :value="3" :max="99">
                <el-button :icon="Bell" circle />
              </el-badge>
            </el-tooltip>
            <el-dropdown>
              <div class="user-info">
                <el-avatar :size="32" icon="UserFilled" />
                <span class="username">Admin</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人中心</el-dropdown-item>
                  <el-dropdown-item>设置</el-dropdown-item>
                  <el-dropdown-item divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>Props</template>
      <el-table :data="propsData" border>
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { Fold, Search, FullScreen, Bell } from '@element-plus/icons-vue'

const propsData = [
  { name: 'collapsed', desc: '侧边栏是否折叠', type: 'boolean' },
  { name: 'userInfo', desc: '用户信息', type: 'UserInfo' },
  { name: 'breadcrumb', desc: '面包屑数据', type: 'BreadcrumbItem[]' },
]
</script>

<style scoped lang="scss">
.demo-page {
  .demo-header {
    margin-bottom: 24px;
    h2 { margin-bottom: 8px; }
    p { color: #909399; }
  }
}

.header-preview {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.header-demo {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;

  .toggle-btn {
    font-size: 20px;
    cursor: pointer;
    &:hover { color: #409eff; }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  .username {
    font-size: 14px;
    color: #303133;
  }
}
</style>