<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>侧边栏 (Sidebar)</h2>
      <p>可折叠的侧边栏导航组件，支持多级菜单</p>
    </div>

    <el-row :gutter="24">
      <el-col :span="12">
        <el-card>
          <template #header>展开状态</template>
          <div class="sidebar-preview" style="width: 240px">
            <div class="sidebar-demo">
              <div class="sidebar-header">
                <span>Blink Admin</span>
              </div>
              <div class="sidebar-menu">
                <div class="menu-item active">
                  <el-icon><HomeFilled /></el-icon>
                  <span>首页</span>
                </div>
                <div class="menu-item">
                  <el-icon><Setting /></el-icon>
                  <span>系统管理</span>
                </div>
                <div class="menu-group">
                  <div class="menu-item">
                    <el-icon><User /></el-icon>
                    <span>用户管理</span>
                  </div>
                  <div class="menu-item">
                    <el-icon><Lock /></el-icon>
                    <span>角色管理</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>折叠状态</template>
          <div class="sidebar-preview" style="width: 64px">
            <div class="sidebar-demo collapsed">
              <div class="sidebar-header">B</div>
              <div class="sidebar-menu">
                <div class="menu-item icon-only active">
                  <el-icon><HomeFilled /></el-icon>
                </div>
                <div class="menu-item icon-only">
                  <el-icon><Setting /></el-icon>
                </div>
                <div class="menu-item icon-only">
                  <el-icon><User /></el-icon>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 24px">
      <template #header>Props</template>
      <el-table :data="propsData" border>
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="200" />
        <el-table-column prop="default" label="默认值" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { HomeFilled, Setting, User, Lock } from '@element-plus/icons-vue'

const propsData = [
  { name: 'menuData', desc: '菜单数据', type: 'MenuItem[]', default: '[]' },
  { name: 'collapsed', desc: '是否折叠', type: 'boolean', default: 'false' },
  { name: 'defaultActive', desc: '默认激活菜单', type: 'string', default: "''" },
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

.sidebar-preview {
  height: 400px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.sidebar-demo {
  height: 100%;
  background: #304156;
  color: #fff;
  display: flex;
  flex-direction: column;

  .sidebar-header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 18px;
    border-bottom: 1px solid rgba(255,255,255,0.1);
  }

  &.collapsed .sidebar-header {
    font-size: 24px;
  }

  .sidebar-menu {
    flex: 1;
    padding: 12px;
    overflow-y: auto;
  }

  .menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    border-radius: 4px;
    cursor: pointer;
    margin-bottom: 4px;
    transition: all 0.2s;

    &:hover { background: rgba(255,255,255,0.1); }
    &.active { background: #409eff; }
    &.icon-only { justify-content: center; padding: 12px; }
  }

  .menu-group {
    padding-left: 24px;
  }
}
</style>