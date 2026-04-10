<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>全屏切换 (FullscreenToggle)</h2>
      <p>切换浏览器全屏模式</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <el-row :gutter="24">
        <el-col :span="12">
          <div class="fullscreen-demo">
            <el-button :icon="FullScreen" circle size="large" @click="toggleFullscreen" />
            <p>点击切换全屏</p>
            <p class="status">当前状态: {{ isFullscreen ? '全屏' : '窗口' }}</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="fullscreen-demo">
            <el-button type="primary" :icon="isFullscreen ? Aim : FullScreen">
              {{ isFullscreen ? '退出全屏' : '进入全屏' }}
            </el-button>
            <p>按钮样式</p>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card style="margin-top: 24px">
      <template #header>使用示例</template>
      <pre class="code-block">{{ codeExample }}</pre>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { FullScreen, Aim } from '@element-plus/icons-vue'

const isFullscreen = ref(false)

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

onMounted(() => {
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

const codeExample = `<template>
  <FullscreenToggle v-model="isFullscreen" />
</template>`
</script>

<style scoped lang="scss">
.demo-page {
  .demo-header {
    margin-bottom: 24px;
    h2 { margin-bottom: 8px; }
    p { color: #909399; }
  }
}

.fullscreen-demo {
  padding: 24px;
  text-align: center;
  border: 1px solid #e4e7ed;
  border-radius: 8px;

  p {
    margin-top: 12px;
    color: #909399;
    font-size: 13px;
  }

  .status {
    color: #409eff;
    font-weight: 500;
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