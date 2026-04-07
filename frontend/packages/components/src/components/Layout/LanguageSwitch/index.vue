<template>
  <el-dropdown
    class="language-switch"
    trigger="click"
    :placement="placement"
    @command="handleCommand"
  >
    <slot :current-language="currentLanguage" :languages="languages">
      <!-- 默认触发器 -->
      <div class="language-trigger">
        <span class="language-text">{{ currentLanguageLabel }}</span>
        <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
      </div>
    </slot>

    <template #dropdown>
      <el-dropdown-menu>
        <slot name="menu" :languages="languages" :current-language="currentLanguage" :select="handleCommand">
          <el-dropdown-item
            v-for="lang in languages"
            :key="lang.code"
            :command="lang.code"
            :class="{ 'is-active': lang.code === currentLanguage }"
          >
            <span class="lang-label">{{ lang.label }}</span>
            <span v-if="lang.nativeLabel" class="lang-native">{{ lang.nativeLabel }}</span>
          </el-dropdown-item>
        </slot>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
/**
 * LanguageSwitch 语言切换组件
 *
 * 特点：
 * - 支持自定义语言列表
 * - 支持自定义触发器
 * - 解耦 i18n，通过事件通知外部
 * - 完全可定制样式
 */

import { computed } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'

// ============================================
// 类型定义
// ============================================

/**
 * 语言选项接口
 */
export interface LanguageOption {
  /** 语言代码 */
  code: string
  /** 显示标签 */
  label: string
  /** 原生标签（如中文显示"中文"，英文显示"English"） */
  nativeLabel?: string
}

export interface Props {
  /** 当前语言 */
  currentLanguage?: string
  /** 语言列表 */
  languages?: LanguageOption[]
  /** 下拉菜单位置 */
  placement?: 'top' | 'top-start' | 'top-end' | 'bottom' | 'bottom-start' | 'bottom-end'
}

// ============================================
// Props 定义
// ============================================

const props = withDefaults(defineProps<Props>(), {
  currentLanguage: 'zh_cn',
  languages: () => [
    { code: 'zh_cn', label: '中文', nativeLabel: '简体中文' },
    { code: 'en_us', label: 'EN', nativeLabel: 'English' },
  ],
  placement: 'bottom-end',
})

const emit = defineEmits<{
  (e: 'change', lang: string): void
}>()

// ============================================
// 计算属性
// ============================================

const currentLanguageLabel = computed(() => {
  const lang = props.languages.find((l) => l.code === props.currentLanguage)
  return lang?.label || props.currentLanguage
})

// ============================================
// 方法
// ============================================

const handleCommand = (lang: string) => {
  emit('change', lang)
}

// ============================================
// 暴露
// ============================================

defineExpose({
  currentLanguageLabel,
  handleCommand,
})
</script>

<style scoped lang="scss">
.language-switch {
  .language-trigger {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s ease;
    color: var(--text-color-regular, #606266);

    &:hover {
      color: var(--primary-color, #3b82f6);
      background: rgba(59, 130, 246, 0.1);
    }

    .language-text {
      font-size: 13px;
      white-space: nowrap;
    }

    .dropdown-arrow {
      font-size: 12px;
      transition: transform 0.2s ease;
    }
  }

  &:hover .dropdown-arrow {
    transform: rotate(180deg);
  }
}

.lang-label {
  font-weight: 500;
}

.lang-native {
  margin-left: 8px;
  color: var(--text-color-secondary, #909399);
  font-size: 12px;
}

:deep(.el-dropdown-menu__item) {
  &.is-active {
    color: var(--primary-color, #3b82f6);
    background: rgba(59, 130, 246, 0.1);
  }
}
</style>