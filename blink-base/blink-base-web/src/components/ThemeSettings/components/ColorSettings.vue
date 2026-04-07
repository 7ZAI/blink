<!-- src/components/ThemeSettings/components/ColorSettings.vue -->
<template>
  <div class="color-settings">
    <h4 class="section-title">{{ t('settings.colorSettings') }}</h4>
    <el-form label-position="left" label-width="100px" class="color-form">
      <el-form-item :label="t('settings.primaryColor')">
        <div class="color-input-wrapper">
          <el-color-picker
            v-model="localColors.primary"
            :predefine="predefineColors"
            @change="handleColorChange('primary')"
          />
          <el-input
            v-model="localColors.primary"
            placeholder="#409EFF"
            class="color-text-input"
            @change="handleColorChange('primary')"
          >
            <template #prefix>
              <span
                class="color-preview-dot"
                :style="{ backgroundColor: localColors.primary }"
              />
            </template>
          </el-input>
        </div>
      </el-form-item>

      <el-form-item :label="t('settings.successColor')">
        <div class="color-input-wrapper">
          <el-color-picker
            v-model="localColors.success"
            :predefine="predefineColors"
            @change="handleColorChange('success')"
          />
          <el-input
            v-model="localColors.success"
            placeholder="#67C23A"
            class="color-text-input"
            @change="handleColorChange('success')"
          >
            <template #prefix>
              <span
                class="color-preview-dot"
                :style="{ backgroundColor: localColors.success }"
              />
            </template>
          </el-input>
        </div>
      </el-form-item>

      <el-form-item :label="t('settings.warningColor')">
        <div class="color-input-wrapper">
          <el-color-picker
            v-model="localColors.warning"
            :predefine="predefineColors"
            @change="handleColorChange('warning')"
          />
          <el-input
            v-model="localColors.warning"
            placeholder="#E6A23C"
            class="color-text-input"
            @change="handleColorChange('warning')"
          >
            <template #prefix>
              <span
                class="color-preview-dot"
                :style="{ backgroundColor: localColors.warning }"
              />
            </template>
          </el-input>
        </div>
      </el-form-item>

      <el-form-item :label="t('settings.dangerColor')">
        <div class="color-input-wrapper">
          <el-color-picker
            v-model="localColors.danger"
            :predefine="predefineColors"
            @change="handleColorChange('danger')"
          />
          <el-input
            v-model="localColors.danger"
            placeholder="#F56C6C"
            class="color-text-input"
            @change="handleColorChange('danger')"
          >
            <template #prefix>
              <span
                class="color-preview-dot"
                :style="{ backgroundColor: localColors.danger }"
              />
            </template>
          </el-input>
        </div>
      </el-form-item>

      <el-form-item :label="t('settings.infoColor')">
        <div class="color-input-wrapper">
          <el-color-picker
            v-model="localColors.info"
            :predefine="predefineColors"
            @change="handleColorChange('info')"
          />
          <el-input
            v-model="localColors.info"
            placeholder="#909399"
            class="color-text-input"
            @change="handleColorChange('info')"
          >
            <template #prefix>
              <span
                class="color-preview-dot"
                :style="{ backgroundColor: localColors.info }"
              />
            </template>
          </el-input>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ThemeColors } from '../types'

defineOptions({
  name: 'ColorSettings',
})

interface Props {
  colors: ThemeColors
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'change', colors: ThemeColors): void
}>()

const { t } = useI18n()

// Predefined colors for quick selection
const predefineColors = [
  '#3b82f6',
  '#8b5cf6',
  '#f97316',
  '#10b981',
  '#ef4444',
  '#06b6d4',
  '#64748b',
  '#1e293b',
  '#6366f1',
  '#f43f5e',
  '#22c55e',
  '#fbbf24',
]

// Local reactive copy of colors
const localColors = reactive<ThemeColors>({
  primary: props.colors.primary,
  success: props.colors.success,
  warning: props.colors.warning,
  danger: props.colors.danger,
  info: props.colors.info,
})

// Deep watch props.colors for external changes
watch(
  () => props.colors,
  (newColors) => {
    localColors.primary = newColors.primary
    localColors.success = newColors.success
    localColors.warning = newColors.warning
    localColors.danger = newColors.danger
    localColors.info = newColors.info
  },
  { deep: true }
)

// Handle color change
const handleColorChange = (key: keyof ThemeColors) => {
  // Validate hex color format
  const colorValue = localColors[key]
  if (colorValue && !/^#[0-9A-Fa-f]{6}$/.test(colorValue)) {
    // Auto-fix: add # prefix if missing
    if (/^[0-9A-Fa-f]{6}$/.test(colorValue)) {
      localColors[key] = `#${colorValue}`
    }
  }

  emit('change', { ...localColors })
}
</script>

<style scoped lang="scss">
.color-settings {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 16px;
  }

  .color-form {
    .el-form-item {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  .color-input-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;

    :deep(.el-color-picker) {
      --el-color-picker-size: 32px;

      .el-color-picker__trigger {
        border: 1px solid var(--border-color-light);
        transition: border-color 0.3s ease;

        &:hover {
          border-color: var(--primary-color);
        }
      }
    }

    .color-text-input {
      flex: 1;
      max-width: 180px;

      :deep(.el-input__wrapper) {
        padding-left: 8px;
      }

      :deep(.el-input__prefix) {
        display: flex;
        align-items: center;
      }
    }

    .color-preview-dot {
      width: 16px;
      height: 16px;
      border-radius: 4px;
      border: 1px solid var(--border-color-lighter);
      display: inline-block;
    }
  }

  // Dark mode adjustments using CSS variables
  @media (prefers-color-scheme: dark) {
    .section-title {
      color: var(--text-color-primary);
    }

    .color-input-wrapper {
      :deep(.el-color-picker__trigger) {
        background-color: var(--bg-color-overlay);
      }
    }
  }
}
</style>