<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>主题设置 (ThemeSettings)</h2>
      <p>可视化主题配置面板，支持颜色、字体、预设主题等设置</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <el-button type="primary" @click="showSettings = true">
        打开主题设置
      </el-button>

      <el-dialog v-model="showSettings" title="主题设置" width="500px">
        <div class="settings-preview">
          <el-tabs>
            <el-tab-pane label="预设主题">
              <div class="preset-grid">
                <div
                  v-for="preset in presets"
                  :key="preset.name"
                  class="preset-item"
                  :class="{ active: currentPreset === preset.name }"
                  @click="currentPreset = preset.name"
                >
                  <div class="preset-color" :style="{ background: preset.primary }" />
                  <span>{{ preset.label }}</span>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="颜色设置">
              <el-form label-width="80px">
                <el-form-item label="主题色">
                  <el-color-picker v-model="themeColors.primary" />
                </el-form-item>
                <el-form-item label="成功色">
                  <el-color-picker v-model="themeColors.success" />
                </el-form-item>
                <el-form-item label="警告色">
                  <el-color-picker v-model="themeColors.warning" />
                </el-form-item>
                <el-form-item label="危险色">
                  <el-color-picker v-model="themeColors.danger" />
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="字体设置">
              <el-form label-width="80px">
                <el-form-item label="字体">
                  <el-select v-model="fontFamily" style="width: 100%">
                    <el-option label="默认字体" value="default" />
                    <el-option label="思源黑体" value="Noto Sans SC" />
                    <el-option label="霞鹜文楷" value="LXGW WenKai" />
                  </el-select>
                </el-form-item>
                <el-form-item label="字号">
                  <el-slider v-model="fontSize" :min="12" :max="20" show-input />
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-dialog>
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
import { ref, reactive } from 'vue'

const showSettings = ref(false)
const currentPreset = ref('default')

const presets = [
  { name: 'default', label: '默认蓝', primary: '#409eff' },
  { name: 'green', label: '清新绿', primary: '#67c23a' },
  { name: 'orange', label: '活力橙', primary: '#e6a23c' },
  { name: 'red', label: '热情红', primary: '#f56c6c' },
  { name: 'purple', label: '神秘紫', primary: '#a855f7' },
]

const themeColors = reactive({
  primary: '#409eff',
  success: '#67c23a',
  warning: '#e6a23c',
  danger: '#f56c6c',
})

const fontFamily = ref('default')
const fontSize = ref(14)

const propsData = [
  { name: 'modelValue', desc: '主题配置对象', type: 'FullThemeConfig' },
  { name: 'presets', desc: '预设主题列表', type: 'ThemePreset[]' },
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

.settings-preview {
  padding: 16px 0;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.preset-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #409eff;
  }

  &.active {
    border-color: #409eff;
    background: #ecf5ff;
  }

  .preset-color {
    width: 40px;
    height: 40px;
    border-radius: 50%;
  }

  span {
    font-size: 13px;
    color: #606266;
  }
}
</style>