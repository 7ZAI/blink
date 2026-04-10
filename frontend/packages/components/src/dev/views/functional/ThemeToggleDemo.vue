<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>主题切换 (ThemeToggle)</h2>
      <p>在亮色和暗色主题之间切换</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <el-row :gutter="24">
        <el-col :span="8">
          <div class="toggle-preview light">
            <div class="toggle-item">
              <el-switch v-model="lightMode" active-text="亮色" inactive-text="暗色" />
            </div>
            <p>Switch 样式</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="toggle-preview">
            <el-button :icon="Sunny" circle size="large" />
            <p>按钮样式 - 亮色</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="toggle-preview dark">
            <el-button :icon="Moon" circle size="large" />
            <p>按钮样式 - 暗色</p>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>使用示例</template>
      <pre class="code-block">{{ codeExample }}</pre>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>Props</template>
      <el-table :data="propsData" border>
        <el-table-column prop="name" label="参数" width="150" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="type" label="类型" width="150" />
        <el-table-column prop="default" label="默认值" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Sunny, Moon } from '@element-plus/icons-vue'

const lightMode = ref(true)

const codeExample = `<template>
  <ThemeToggle v-model="isDark" />
</template>

<script setup>
import { ref } from 'vue'
const isDark = ref(false)
<\/script>`

const propsData = [
  { name: 'modelValue', desc: '当前主题（暗色为true）', type: 'boolean', default: 'false' },
  { name: 'size', desc: '按钮尺寸', type: "'large' | 'default' | 'small'", default: 'default' },
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

.toggle-preview {
  padding: 24px;
  text-align: center;
  border: 1px solid #e4e7ed;
  border-radius: 8px;

  &.dark {
    background: #1f2937;
    color: #fff;
  }

  .toggle-item {
    padding: 16px 0;
  }

  p {
    margin-top: 12px;
    color: #909399;
    font-size: 13px;
  }
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