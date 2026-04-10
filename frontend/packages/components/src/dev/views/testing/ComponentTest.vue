<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>组件测试</h2>
      <p>交互式组件测试工具，快速验证组件功能</p>
    </div>

    <el-row :gutter="24">
      <el-col :span="8">
        <el-card>
          <template #header>选择组件</template>
          <el-tree
            :data="componentTree"
            :props="{ label: 'name' }"
            :highlight-current="true"
            @node-click="selectComponent"
          />
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>测试面板</span>
            <el-button type="primary" size="small" @click="runTest">
              <el-icon><VideoPlay /></el-icon>
              运行测试
            </el-button>
          </template>

          <div v-if="selectedComponent" class="test-panel">
            <div class="component-info">
              <h3>{{ selectedComponent.name }}</h3>
              <p>{{ selectedComponent.description }}</p>
            </div>

            <el-divider>Props 测试</el-divider>

            <el-form label-width="100px">
              <el-form-item
                v-for="prop in selectedComponent.props"
                :key="prop.name"
                :label="prop.name"
              >
                <template v-if="prop.type === 'boolean'">
                  <el-switch v-model="propValues[prop.name]" />
                </template>
                <template v-else-if="prop.type === 'string'">
                  <el-input v-model="propValues[prop.name]" :placeholder="prop.default" />
                </template>
                <template v-else-if="prop.type === 'number'">
                  <el-input-number v-model="propValues[prop.name]" />
                </template>
                <template v-else>
                  <el-input v-model="propValues[prop.name]" />
                </template>
                <span class="prop-type">{{ prop.type }}</span>
              </el-form-item>
            </el-form>

            <el-divider>渲染预览</el-divider>

            <div class="preview-area">
              <div class="preview-content" :class="{ 'dark-mode': darkMode }">
                <component
                  :is="selectedComponent.component"
                  v-bind="propValues"
                  @click="handleEvent('click')"
                  @change="handleEvent('change')"
                />
              </div>
              <div class="preview-actions">
                <el-switch v-model="darkMode" active-text="暗色" inactive-text="亮色" />
              </div>
            </div>

            <el-divider>测试结果</el-divider>

            <div class="test-results">
              <el-timeline>
                <el-timeline-item
                  v-for="(result, index) in testResults"
                  :key="index"
                  :type="result.success ? 'success' : 'danger'"
                  :timestamp="result.time"
                >
                  {{ result.message }}
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="!testResults.length" description="点击运行测试查看结果" />
            </div>
          </div>

          <el-empty v-else description="请从左侧选择要测试的组件" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import {
  ThemeToggle,
  FullscreenToggle,
  LanguageSwitch,
} from '../../lib-index'

const selectedComponent = ref<any>(null)
const darkMode = ref(false)
const propValues = reactive<Record<string, any>>({})
const testResults = ref<any[]>([])

const componentTree = ref([
  {
    name: '布局组件',
    children: [
      { name: 'ThemeToggle', description: '主题切换按钮' },
      { name: 'LanguageSwitch', description: '语言切换器' },
      { name: 'FullscreenToggle', description: '全屏切换按钮' },
    ],
  },
  {
    name: '功能组件',
    children: [
      { name: 'ThemeSettings', description: '主题设置面板' },
      { name: 'CaptchaSlider', description: '滑块验证码' },
      { name: 'IconSelector', description: '图标选择器' },
    ],
  },
  {
    name: '业务组件',
    children: [
      { name: 'BlinkDialog', description: '封装对话框' },
      { name: 'BlinkTable', description: '封装表格' },
      { name: 'UserDropdown', description: '用户下拉菜单' },
    ],
  },
])

const componentMap: Record<string, any> = {
  ThemeToggle: {
    name: 'ThemeToggle',
    description: '主题切换按钮，支持亮色/暗色模式切换',
    component: ThemeToggle,
    props: [
      { name: 'modelValue', type: 'boolean', default: 'false' },
      { name: 'size', type: 'string', default: 'default' },
    ],
  },
  LanguageSwitch: {
    name: 'LanguageSwitch',
    description: '语言切换器，支持多语言切换',
    component: LanguageSwitch,
    props: [
      { name: 'locale', type: 'string', default: 'zh_cn' },
    ],
  },
  FullscreenToggle: {
    name: 'FullscreenToggle',
    description: '全屏切换按钮',
    component: FullscreenToggle,
    props: [
      { name: 'size', type: 'string', default: 'default' },
    ],
  },
}

const selectComponent = (node: any) => {
  if (!node.children) {
    const comp = componentMap[node.name]
    if (comp) {
      selectedComponent.value = comp
      // 初始化 prop 值
      propValues.value = {}
      comp.props.forEach((p: any) => {
        propValues[p.name] = p.type === 'boolean' ? p.default === 'true' : p.default
      })
      testResults.value = []
    }
  }
}

const handleEvent = (event: string) => {
  console.log(`Event: ${event}`, propValues)
}

const runTest = () => {
  testResults.value = []
  const now = new Date().toLocaleTimeString()

  // 模拟测试
  testResults.value.push({
    success: true,
    message: '组件渲染成功',
    time: now,
  })

  if (selectedComponent.value.props.length > 0) {
    testResults.value.push({
      success: true,
      message: 'Props 绑定正常',
      time: now,
    })
  }

  testResults.value.push({
    success: Math.random() > 0.3,
    message: '事件触发测试',
    time: now,
  })
}
</script>

<style scoped lang="scss">
.demo-page {
  .demo-header {
    margin-bottom: 24px;
    h2 { margin-bottom: 8px; }
    p { color: #909399; }
  }
}

.test-panel {
  .component-info {
    margin-bottom: 16px;
    h3 { margin-bottom: 4px; }
    p { color: #909399; font-size: 13px; }
  }
}

.prop-type {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
}

.preview-area {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.preview-content {
  padding: 24px;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;

  &.dark-mode {
    background: #1f2937;
    color: #fff;
  }
}

.preview-actions {
  padding: 12px;
  background: #f5f7fa;
  display: flex;
  justify-content: flex-end;
}

.test-results {
  max-height: 200px;
  overflow-y: auto;
}
</style>