<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>用户下拉菜单 (UserDropdown)</h2>
      <p>用户信息展示和操作下拉菜单</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <el-row :gutter="24">
        <el-col :span="8">
          <h4 style="margin-bottom: 12px">基础用法</h4>
          <el-dropdown trigger="click">
            <div class="user-trigger">
              <el-avatar :size="36" icon="UserFilled" />
              <span class="username">Admin</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item :icon="Setting">设置</el-dropdown-item>
                <el-dropdown-item divided :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-col>
        <el-col :span="8">
          <h4 style="margin-bottom: 12px">带头像</h4>
          <el-dropdown trigger="click">
            <div class="user-trigger">
              <el-avatar :size="36" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">张三</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="User">个人中心</el-dropdown-item>
                <el-dropdown-item divided :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-col>
        <el-col :span="8">
          <h4 style="margin-bottom: 12px">折叠模式</h4>
          <el-dropdown trigger="click">
            <el-avatar :size="36" icon="UserFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>Admin</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-col>
      </el-row>
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
import { ArrowDown, User, Setting, SwitchButton } from '@element-plus/icons-vue'

const propsData = [
  { name: 'userInfo', desc: '用户信息对象', type: 'UserInfo' },
  { name: 'menuItems', desc: '菜单项配置', type: 'MenuItem[]' },
  { name: 'collapsed', desc: '是否折叠模式', type: 'boolean' },
  { name: 'trigger', desc: '触发方式', type: "'hover' | 'click'" },
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

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f5f7fa;
  }

  .username {
    font-size: 14px;
    color: #303133;
  }
}
</style>