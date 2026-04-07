<template>
  <div class="settings-page p-4">
    <el-card shadow="never" class="rounded-lg">
      <template #header>
        <div class="card-header flex items-center justify-between">
          <span class="text-base font-semibold">{{ t('header.settings') }}</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="settings-tabs pt-4">
        <!-- 基础设置 -->
        <el-tab-pane :label="t('settings.basicSettings')" name="basic">
          <el-form label-width="120px" class="settings-form max-w-[600px] px-4">
            <el-form-item :label="t('settings.theme')">
              <div class="theme-options flex gap-6">
                <!-- 浅色主题选项 -->
                <div
                  class="theme-option flex flex-col items-center gap-2 p-4 border-2 rounded-lg cursor-pointer transition-all"
                  :class="{ 'border-primary bg-primary-light': themeStore.theme === 'light' }"
                  @click="setTheme('light')"
                >
                  <div class="theme-preview light-theme w-20 h-[60px] rounded shadow-md"></div>
                  <span>{{ t('settings.lightTheme') }}</span>
                </div>
                <!-- 暗黑主题选项 -->
                <div
                  class="theme-option flex flex-col items-center gap-2 p-4 border-2 rounded-lg cursor-pointer transition-all"
                  :class="{ 'border-primary bg-primary-light': themeStore.theme === 'dark' }"
                  @click="setTheme('dark')"
                >
                  <div class="theme-preview dark-theme w-20 h-[60px] rounded shadow-md"></div>
                  <span>{{ t('settings.darkTheme') }}</span>
                </div>
              </div>
            </el-form-item>

            <!-- 语言设置 -->
            <el-form-item :label="t('settings.language')">
              <el-select v-model="currentLocale" @change="handleLocaleChange">
                <el-option :label="t('settings.chinese')" value="zh_cn" />
                <el-option label="English" value="en_us" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 侧边栏设置 -->
        <el-tab-pane :label="t('settings.sidebarSettings')" name="sidebar">
          <el-form label-width="120px" class="settings-form max-w-[600px] px-4">
            <el-form-item :label="t('settings.sidebarWidth')">
              <el-slider
                v-model="sidebarWidth"
                :min="150"
                :max="400"
                :step="10"
                show-input
                @change="saveSidebarWidth"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useThemeStore } from '@/stores/theme'
import { setLocale, getCurrentLocale } from '@/locales'

defineOptions({
  name: 'Settings',
})

const { t } = useI18n()
const themeStore = useThemeStore()

const activeTab = ref('basic')
const currentLocale = ref(getCurrentLocale())
const sidebarWidth = ref(220)

const setTheme = (theme: 'light' | 'dark') => {
  themeStore.setMode(theme)
  ElMessage.success(t('message.operationSuccess'))
}

const handleLocaleChange = (locale: string) => {
  if (locale !== currentLocale.value) {
    setLocale(locale)
    currentLocale.value = locale
    ElMessage.success(t('message.operationSuccess'))
  }
}

const saveSidebarWidth = () => {
  localStorage.setItem('sidebarWidth', String(sidebarWidth.value))
  ElMessage.success(t('message.operationSuccess'))
}

onMounted(() => {
  const savedWidth = localStorage.getItem('sidebarWidth')
  if (savedWidth) {
    sidebarWidth.value = parseInt(savedWidth)
  }
})
</script>

<style scoped lang="scss">
/* 设置页面样式 */
.settings-page {
  /* 设置标签页 */
  .settings-tabs {
    @apply pt-4;

    /* 设置表单 */
    .settings-form {
      @apply max-w-[600px] px-4;
    }

    /* 主题选项 */
    .theme-options {
      @apply flex gap-6;

      .theme-option {
        @apply flex flex-col items-center gap-2 p-4 border-2 rounded-lg cursor-pointer transition-all;
        border-color: var(--border-color-light);

        &:hover {
          @apply border-primary;
        }

        /* 选中状态使用 Tailwind 的类绑定 */
        /* 主题预览 */
        .theme-preview {
          @apply w-20 h-[60px] rounded shadow-md;

          &.light-theme {
            background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
          }

          &.dark-theme {
            background: linear-gradient(135deg, #1f1f1f 0%, #141414 100%);
          }
        }
      }
    }
  }
}
</style>