<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>图标选择器 (IconSelector)</h2>
      <p>可视化图标选择组件，支持分类、搜索和最近使用</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <el-row :gutter="24">
        <el-col :span="12">
          <h4 style="margin-bottom: 12px">选择图标</h4>
          <el-input
            v-model="selectedIcon"
            placeholder="点击选择图标"
            readonly
            @click="showSelector = true"
          >
            <template #prefix>
              <el-icon v-if="selectedIcon"><component :is="selectedIcon" /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="12">
          <h4 style="margin-bottom: 12px">已选图标</h4>
          <el-tag v-if="selectedIcon" type="success" size="large">
            <el-icon style="margin-right: 4px"><component :is="selectedIcon" /></el-icon>
            {{ selectedIcon }}
          </el-tag>
        </el-col>
      </el-row>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>图标分类预览</template>
      <el-tabs>
        <el-tab-pane label="Element 图标">
          <div class="icon-grid">
            <div
              v-for="icon in elementIcons"
              :key="icon"
              class="icon-item"
              @click="selectedIcon = icon"
            >
              <el-icon :size="24"><component :is="icon" /></el-icon>
              <span>{{ icon }}</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="系统图标">
          <div class="icon-grid">
            <div
              v-for="icon in systemIcons"
              :key="icon"
              class="icon-item"
              @click="selectedIcon = icon"
            >
              <el-icon :size="24"><component :is="icon" /></el-icon>
              <span>{{ icon }}</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="媒体图标">
          <div class="icon-grid">
            <div
              v-for="icon in mediaIcons"
              :key="icon"
              class="icon-item"
              @click="selectedIcon = icon"
            >
              <el-icon :size="24"><component :is="icon" /></el-icon>
              <span>{{ icon }}</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
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
import {
  HomeFilled,
  Setting,
  User,
  Lock,
  Document,
  Folder,
  Calendar,
  Bell,
  Search,
  Edit,
  Delete,
  Download,
  Upload,
  Picture,
  VideoPlay,
  Headset,
  Microphone,
  Camera,
} from '@element-plus/icons-vue'

const showSelector = ref(false)
const selectedIcon = ref('')

const elementIcons = ['HomeFilled', 'Setting', 'User', 'Lock', 'Document', 'Folder', 'Calendar', 'Bell']
const systemIcons = ['Search', 'Edit', 'Delete', 'Download', 'Upload', 'Picture']
const mediaIcons = ['VideoPlay', 'Headset', 'Microphone', 'Camera']

const propsData = [
  { name: 'modelValue', desc: '选中的图标', type: 'string' },
  { name: 'groups', desc: '图标分组配置', type: 'IconGroup[]' },
  { name: 'placeholder', desc: '占位文本', type: 'string' },
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

.icon-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #ecf5ff;
    border-color: #409eff;
    color: #409eff;
  }

  span {
    font-size: 11px;
    color: #909399;
    text-align: center;
    word-break: break-all;
  }
}
</style>