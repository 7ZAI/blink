<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>标签页 (TabsView)</h2>
      <p>多标签页管理组件，支持标签切换、关闭、刷新、滚动、最大数量限制等操作</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <div class="tabs-preview">
        <div class="config-panel">
          <el-form inline size="small">
            <el-form-item label="最大标签数">
              <el-input-number v-model="maxTabs" :min="5" :max="50" />
            </el-form-item>
            <el-form-item label="标签最小宽度">
              <el-input-number v-model="minTabWidth" :min="60" :max="120" />
            </el-form-item>
            <el-form-item label="标签最大宽度">
              <el-input-number v-model="maxTabWidth" :min="100" :max="200" />
            </el-form-item>
          </el-form>
        </div>

        <TabsView
          ref="tabsViewRef"
          :tabs="tabs"
          :active-path="activeTab"
          :max-tabs="maxTabs"
          :min-tab-width="minTabWidth"
          :max-tab-width="maxTabWidth"
          @tab-click="handleTabClick"
          @close-tab="handleCloseTab"
          @max-tabs-reached="handleMaxTabsReached"
        />

        <div class="tabs-content">
          <p>当前页面: {{ tabs.find(t => t.path === activeTab)?.title }}</p>
          <p>标签数量: {{ tabs.length }} / {{ maxTabs }}</p>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>功能演示</template>
      <el-space wrap>
        <el-button type="primary" @click="addTab">添加标签</el-button>
        <el-button @click="addManyTabs">批量添加 5 个标签</el-button>
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
        <el-table-column prop="default" label="默认值" width="100" />
      </el-table>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>Events</template>
      <el-table :data="eventsData" border>
        <el-table-column prop="name" label="事件名" width="180" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="params" label="参数" width="200" />
      </el-table>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>Methods (via ref)</template>
      <el-table :data="methodsData" border>
        <el-table-column prop="name" label="方法名" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="params" label="参数" width="150" />
        <el-table-column prop="return" label="返回值" width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import TabsView, { type TabItem } from '../../../components/Layout/TabsView/index.vue'

const activeTab = ref('/home')
const maxTabs = ref(20)
const minTabWidth = ref(80)
const maxTabWidth = ref(160)
const tabsViewRef = ref<InstanceType<typeof TabsView>>()

const tabs = ref<TabItem[]>([
  { path: '/home', name: 'Home', title: '首页', fullPath: '/home', closable: false },
  { path: '/user', name: 'User', title: '用户管理', fullPath: '/user', closable: true },
  { path: '/role', name: 'Role', title: '角色管理', fullPath: '/role', closable: true },
])

const tabCount = ref(0)

const handleTabClick = (tab: TabItem) => {
  activeTab.value = tab.path
}

const handleCloseTab = (tab: TabItem) => {
  const index = tabs.value.findIndex(t => t.path === tab.path)
  if (index > -1) {
    tabs.value.splice(index, 1)
    if (activeTab.value === tab.path && tabs.value.length > 0) {
      activeTab.value = tabs.value[Math.max(0, index - 1)].path
    }
  }
}

const handleMaxTabsReached = (currentCount: number, max: number) => {
  ElMessage.warning(`标签数量已达上限 ${max}，请先关闭部分标签后再打开新页面`)
}

const addTab = () => {
  // 使用组件暴露的方法检查是否可以添加
  if (!tabsViewRef.value?.checkMaxTabs()) {
    return
  }

  tabCount.value++
  const path = `/new-${tabCount.value}`
  tabs.value.push({
    path,
    name: `NewPage${tabCount.value}`,
    title: `新页面 ${tabCount.value}`,
    fullPath: path,
    closable: true,
    isNew: true,
  })
  activeTab.value = path
}

const addManyTabs = () => {
  for (let i = 0; i < 5; i++) {
    if (!tabsViewRef.value?.checkMaxTabs()) {
      break
    }
    tabCount.value++
    const path = `/batch-${tabCount.value}`
    tabs.value.push({
      path,
      name: `BatchPage${tabCount.value}`,
      title: `批量页面 ${tabCount.value}`,
      fullPath: path,
      closable: true,
      isNew: true,
    })
    activeTab.value = path
  }
}

const closeOther = () => {
  tabs.value = tabs.value.filter(t => !t.closable || t.path === activeTab.value)
}

const closeAll = () => {
  tabs.value = tabs.value.filter(t => !t.closable)
  activeTab.value = '/home'
}

const propsData = [
  { name: 'tabs', desc: '标签数据数组', type: 'TabItem[]', default: '[]' },
  { name: 'activePath', desc: '当前激活标签路径', type: 'string', default: '' },
  { name: 'maxTabs', desc: '最大标签数量，超过后触发 max-tabs-reached 事件', type: 'number', default: '20' },
  { name: 'overflowWarningThreshold', desc: '标签溢出警告阈值', type: 'number', default: '15' },
  { name: 'minTabWidth', desc: '标签最小宽度（像素）', type: 'number', default: '80' },
  { name: 'maxTabWidth', desc: '标签最大宽度（像素）', type: 'number', default: '160' },
  { name: 'showContextMenu', desc: '是否显示右键菜单', type: 'boolean', default: 'true' },
  { name: 'contextMenuItems', desc: '自定义右键菜单项', type: 'ContextMenuItem[]', default: '默认菜单' },
]

const eventsData = [
  { name: 'tab-click', desc: '标签被点击时触发', params: 'tab: TabItem' },
  { name: 'close-tab', desc: '关闭标签时触发', params: 'tab: TabItem' },
  { name: 'close-other-tabs', desc: '关闭其他标签时触发', params: 'tab: TabItem' },
  { name: 'close-right-tabs', desc: '关闭右侧标签时触发', params: 'tab: TabItem' },
  { name: 'close-left-tabs', desc: '关闭左侧标签时触发', params: 'tab: TabItem' },
  { name: 'close-all-tabs', desc: '关闭所有标签时触发', params: '-' },
  { name: 'refresh-tab', desc: '刷新标签时触发', params: 'tab: TabItem' },
  { name: 'max-tabs-reached', desc: '达到最大标签数量时触发', params: 'currentCount: number, maxTabs: number' },
]

const methodsData = [
  { name: 'checkMaxTabs', desc: '检查是否可以添加新标签，达到上限时触发 max-tabs-reached 事件', params: '-', return: 'boolean' },
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

.config-panel {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-color-page);
  border-radius: 8px;
}

.tabs-preview {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.tabs-content {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100px;
}
</style>