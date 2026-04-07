<!-- src/components/ThemeSettings/components/SystemSettings.vue -->
<template>
  <div class="system-settings">
    <h4 class="section-title">系统设置</h4>
    <el-form label-position="left" label-width="100px" class="system-form">
      <!-- Border Radius Slider -->
      <el-form-item label="圆角大小">
        <div class="slider-wrapper">
          <el-slider
            v-model="localConfig.borderRadius"
            :min="0"
            :max="24"
            :step="1"
            show-input
            :show-input-controls="false"
            input-size="small"
            @change="handleConfigChange"
          />
          <span class="slider-unit">px</span>
        </div>
        <div class="preview-box" :style="{ borderRadius: localConfig.borderRadius + 'px' }">
          圆角预览
        </div>
      </el-form-item>

      <!-- Shadow Intensity Radio Group -->
      <el-form-item label="阴影强度">
        <el-radio-group v-model="localConfig.shadowIntensity" @change="handleConfigChange">
          <el-radio-button value="none">无</el-radio-button>
          <el-radio-button value="light">轻</el-radio-button>
          <el-radio-button value="medium">中</el-radio-button>
          <el-radio-button value="strong">强</el-radio-button>
        </el-radio-group>
        <div class="shadow-preview-box" :style="shadowPreviewStyle">
          阴影预览
        </div>
      </el-form-item>

      <!-- Compact Mode Switch -->
      <el-form-item label="紧凑模式">
        <el-switch
          v-model="localConfig.compactMode"
          active-text="开启"
          inactive-text="关闭"
          @change="handleConfigChange"
        />
        <span class="compact-tip">启用后减少界面间距，适合小屏幕</span>
      </el-form-item>

      <!-- Content Width Radio Group -->
      <el-form-item label="内容宽度">
        <el-radio-group v-model="localConfig.contentWidth" @change="handleConfigChange">
          <el-radio-button value="fluid">流式</el-radio-button>
          <el-radio-button value="fixed">固定</el-radio-button>
        </el-radio-group>
        <div class="width-preview-wrapper">
          <div
            class="width-preview-box"
            :style="{ width: localConfig.contentWidth === 'fixed' ? '60%' : '100%' }"
          >
            {{ localConfig.contentWidth === 'fluid' ? '流式布局' : '固定布局' }}
          </div>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed } from 'vue'
import type { SystemConfig } from '../types'

defineOptions({
  name: 'SystemSettings',
})

interface Props {
  config: SystemConfig
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'change', config: SystemConfig): void
}>()

// Local reactive copy of config
const localConfig = reactive<SystemConfig>({
  borderRadius: props.config.borderRadius,
  shadowIntensity: props.config.shadowIntensity,
  compactMode: props.config.compactMode,
  contentWidth: props.config.contentWidth,
})

// Deep watch props.config for external changes
watch(
  () => props.config,
  (newConfig) => {
    localConfig.borderRadius = newConfig.borderRadius
    localConfig.shadowIntensity = newConfig.shadowIntensity
    localConfig.compactMode = newConfig.compactMode
    localConfig.contentWidth = newConfig.contentWidth
  },
  { deep: true }
)

// Shadow preview style based on intensity
const shadowPreviewStyle = computed(() => {
  const intensityMap: Record<SystemConfig['shadowIntensity'], string> = {
    none: 'none',
    light: '0 2px 4px rgba(0, 0, 0, 0.1)',
    medium: '0 4px 8px rgba(0, 0, 0, 0.15)',
    strong: '0 8px 16px rgba(0, 0, 0, 0.2)',
  }
  return {
    boxShadow: intensityMap[localConfig.shadowIntensity],
  }
})

// Handle config change
const handleConfigChange = () => {
  emit('change', { ...localConfig })
}
</script>

<style scoped lang="scss">
.system-settings {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 16px;
  }

  .system-form {
    .el-form-item {
      margin-bottom: 20px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  .slider-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;

    :deep(.el-slider) {
      flex: 1;
      max-width: 200px;
    }

    :deep(.el-input-number) {
      width: 60px;
    }

    .slider-unit {
      color: var(--text-color-secondary);
      font-size: 14px;
    }
  }

  .preview-box {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 8px 16px;
    background-color: var(--primary-color);
    color: #fff;
    font-size: 12px;
    margin-left: 16px;
    transition: border-radius 0.3s ease;
  }

  .shadow-preview-box {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 8px 16px;
    background-color: var(--bg-color-overlay);
    border: 1px solid var(--border-color-light);
    font-size: 12px;
    margin-left: 16px;
    transition: box-shadow 0.3s ease;
  }

  .compact-tip {
    color: var(--text-color-secondary);
    font-size: 12px;
    margin-left: 12px;
  }

  .width-preview-wrapper {
    display: flex;
    margin-left: 16px;
    flex: 1;
    max-width: 200px;
    background-color: var(--bg-color-page);
    padding: 4px;
    border-radius: 4px;
  }

  .width-preview-box {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 8px 12px;
    background-color: var(--primary-color-light-3);
    color: var(--text-color-primary);
    font-size: 12px;
    border-radius: 4px;
    transition: width 0.3s ease;
  }

  :deep(.el-radio-group) {
    flex-wrap: wrap;
  }

  :deep(.el-radio-button__inner) {
    padding: 8px 16px;
  }

  // Dark mode adjustments using CSS variables
  @media (prefers-color-scheme: dark) {
    .section-title {
      color: var(--text-color-primary);
    }

    .preview-box {
      background-color: var(--primary-color);
    }

    .shadow-preview-box {
      background-color: var(--bg-color-overlay);
      border-color: var(--border-color-light);

      // Adjust shadow for dark mode visibility
      --shadow-light: 0 2px 4px rgba(255, 255, 255, 0.1);
      --shadow-medium: 0 4px 8px rgba(255, 255, 255, 0.15);
      --shadow-strong: 0 8px 16px rgba(255, 255, 255, 0.2);
    }

    .width-preview-wrapper {
      background-color: var(--bg-color-page);
    }

    .width-preview-box {
      background-color: var(--primary-color-light-3);
      color: var(--text-color-primary);
    }

    .compact-tip,
    .slider-unit {
      color: var(--text-color-secondary);
    }
  }
}
</style>