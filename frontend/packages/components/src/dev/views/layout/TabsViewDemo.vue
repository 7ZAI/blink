<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>标签页 (TabsView)</h2>
      <p>多标签页管理组件，支持标签切换、关闭、刷新等操作</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <div class="tabs-preview">
        <div class="tabs-demo">
          <div class="tabs-wrapper">
            <div
              v-for="tab in tabs"
              :key="tab.path"
              class="tab-item"
              :class="{ active: activeTab === tab.path }"
              @click="activeTab = tab.path"
            >
              <span>{{ tab.title }}</span>
              <el-icon v-if="tab.closable" class="close-icon" @click.stop><Close /></el-icon>
            </div>
          </div>
        </div>
        <div class="tabs-content">
          <p>当前页面: {{ tabs.find(t => t.path === activeTab)?.title }}</p>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>功能演示</template>
      <el-space wrap>
        <el-button type="primary" @click="addTab">添加标签</el-button>
        <el-button @click="closeOther">关闭其他</el-button>
        <el-button @click="closeAll">关闭所有</el-button>
      </el-space>
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
import { ref } from 'vue'
import { Close } from '@element-plus/icons-vue'

const activeTab = ref('/home')

const tabs = ref([
  { path: '/home', title: '首页', closable: false },
  { path: '/user', title: '用户管理', closable: true },
  { path: '/role', title: '角色管理', closable: true },
])

const tabCount = ref(0)

const addTab = () => {
  tabCount.value++
  const path = `/new-${tabCount.value}`
  tabs.value.push({ path, title: `新页面 ${tabCount.value}`, closable: true })
  activeTab.value = path
}

const closeOther = () => {
  tabs.value = tabs.value.filter(t => !t.closable || t.path === activeTab.value)
}

const closeAll = () => {
  tabs.value = tabs.value.filter(t => !t.closable)
  activeTab.value = '/home'
}

const propsData = [
  { name: 'tabs', desc: '标签数据', type: 'TabItem[]' },
  { name: 'activeTab', desc: '当前激活标签', type: 'string' },
  { name: 'maxTabs', desc: '最大标签数量', type: 'number' },
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

.tabs-preview {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.tabs-demo {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.tabs-wrapper {
  display: flex;
  padding: 8px 12px;
  gap: 4px;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:hover {
    background: #f5f7fa;
  }

  &.active {
    background: #ecf5ff;
    color: #409eff;
    border-color: #409eff;
  }

  .close-icon {
    font-size: 12px;
    border-radius: 50%;

    &:hover {
      background: #c0c4cc;
      color: #fff;
    }
  }
}

.tabs-content {
  padding: 24px;
  background: #f5f7fa;
  min-height: 200px;
}
</style>