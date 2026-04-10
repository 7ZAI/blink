<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>主布局 (MainLayout)</h2>
      <p>完整的应用布局容器，包含侧边栏、头部、标签页和内容区域</p>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom: 24px">
      主布局组件需要配合 Sidebar、Header、TabsView 等子组件使用，建议在全屏模式下预览效果
    </el-alert>

    <el-card>
      <template #header>
        <span>组件预览</span>
        <el-button type="primary" size="small" @click="fullscreen = true">
          全屏预览
        </el-button>
      </template>
      <div class="preview-container" :style="{ height: fullscreen ? '100vh' : '500px' }">
        <div class="mock-layout">
          <div class="mock-sidebar">
            <div class="mock-logo">Logo</div>
            <div class="mock-menu">
              <div class="mock-menu-item active">首页</div>
              <div class="mock-menu-item">系统管理</div>
              <div class="mock-menu-item">用户管理</div>
            </div>
          </div>
          <div class="mock-main">
            <div class="mock-header">
              <span>头部导航</span>
              <div class="mock-actions">用户信息</div>
            </div>
            <div class="mock-tabs">
              <div class="mock-tab active">首页</div>
              <div class="mock-tab">用户列表</div>
            </div>
            <div class="mock-content">
              <h3>内容区域</h3>
              <p>这里是页面的主要内容</p>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>使用示例</template>
      <el-tabs>
        <el-tab-pane label="基础用法">
          <pre class="code-block">{{ basicUsage }}</pre>
        </el-tab-pane>
        <el-tab-pane label="Props">
          <el-table :data="propsData" border>
            <el-table-column prop="name" label="参数" width="150" />
            <el-table-column prop="desc" label="说明" />
            <el-table-column prop="type" label="类型" width="150" />
            <el-table-column prop="default" label="默认值" width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const fullscreen = ref(false)

const basicUsage = `<template>
  <MainLayout
    :collapsed="collapsed"
    :show-tabs="true"
    @toggle-sidebar="collapsed = !collapsed"
  >
    <template #sidebar>
      <Sidebar :menu-data="menuData" />
    </template>
    <template #header>
      <Header :user-info="userInfo" />
    </template>
    <template #tabs>
      <TabsView :tabs="tabs" />
    </template>
    <router-view />
  </MainLayout>
</template>`

const propsData = [
  { name: 'collapsed', desc: '侧边栏是否折叠', type: 'boolean', default: 'false' },
  { name: 'showTabs', desc: '是否显示标签页', type: 'boolean', default: 'true' },
  { name: 'fixedHeader', desc: '是否固定头部', type: 'boolean', default: 'true' },
]
</script>

<style scoped lang="scss">
.demo-page {
  .demo-header {
    margin-bottom: 24px;
    h2 {
      margin-bottom: 8px;
    }
    p {
      color: #909399;
    }
  }
}

.preview-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.mock-layout {
  display: flex;
  height: 100%;
  background: #f5f7fa;
}

.mock-sidebar {
  width: 200px;
  background: #304156;
  color: #fff;
  .mock-logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
  }
  .mock-menu {
    padding: 12px;
  }
  .mock-menu-item {
    padding: 12px 16px;
    border-radius: 4px;
    margin-bottom: 4px;
    cursor: pointer;
    &.active {
      background: #409eff;
    }
    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }
  }
}

.mock-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.mock-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e4e7ed;
}

.mock-tabs {
  height: 40px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 8px;
  .mock-tab {
    padding: 8px 16px;
    border-radius: 4px 4px 0 0;
    cursor: pointer;
    &.active {
      background: #e6f7ff;
      color: #409eff;
    }
  }
}

.mock-content {
  flex: 1;
  padding: 24px;
  background: #fff;
  margin: 16px;
  border-radius: 8px;
}

.code-block {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 6px;
  font-family: monospace;
  font-size: 14px;
  overflow-x: auto;
}
</style>