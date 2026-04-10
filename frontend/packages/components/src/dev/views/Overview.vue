<template>
  <div class="overview-page">
    <div class="page-header">
      <h1>Blink Components 组件库</h1>
      <p>一个基于 Vue 3 + TypeScript + Element Plus 的企业级组件库</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="24" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" :size="40" color="#409eff"><Grid /></el-icon>
            <div class="stat-content">
              <div class="stat-value">{{ componentCount }}</div>
              <div class="stat-label">组件总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" :size="40" color="#67c23a"><Document /></el-icon>
            <div class="stat-content">
              <div class="stat-value">{{ testCount }}</div>
              <div class="stat-label">测试用例</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" :size="40" color="#e6a23c"><Tools /></el-icon>
            <div class="stat-content">
              <div class="stat-value">{{ composableCount }}</div>
              <div class="stat-label">Composables</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <el-icon class="stat-icon" :size="40" color="#f56c6c"><Monitor /></el-icon>
            <div class="stat-content">
              <div class="stat-value">{{ directiveCount }}</div>
              <div class="stat-label">指令</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 组件分类 -->
    <el-row :gutter="24" class="component-categories">
      <el-col :span="12">
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <el-icon :size="24"><Grid /></el-icon>
              <span>布局组件</span>
            </div>
          </template>
          <div class="component-list">
            <router-link
              v-for="comp in layoutComponents"
              :key="comp.path"
              :to="comp.path"
              class="component-item"
            >
              <span class="component-name">{{ comp.name }}</span>
              <span class="component-desc">{{ comp.desc }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <el-icon :size="24"><Tools /></el-icon>
              <span>功能组件</span>
            </div>
          </template>
          <div class="component-list">
            <router-link
              v-for="comp in functionalComponents"
              :key="comp.path"
              :to="comp.path"
              class="component-item"
            >
              <span class="component-name">{{ comp.name }}</span>
              <span class="component-desc">{{ comp.desc }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px">
      <el-col :span="12">
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <el-icon :size="24"><Briefcase /></el-icon>
              <span>业务组件</span>
            </div>
          </template>
          <div class="component-list">
            <router-link
              v-for="comp in businessComponents"
              :key="comp.path"
              :to="comp.path"
              class="component-item"
            >
              <span class="component-name">{{ comp.name }}</span>
              <span class="component-desc">{{ comp.desc }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <el-icon :size="24"><Monitor /></el-icon>
              <span>测试工具</span>
            </div>
          </template>
          <div class="component-list">
            <router-link
              v-for="comp in testingTools"
              :key="comp.path"
              :to="comp.path"
              class="component-item"
            >
              <span class="component-name">{{ comp.name }}</span>
              <span class="component-desc">{{ comp.desc }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Grid, Document, Tools, Monitor, Briefcase } from '@element-plus/icons-vue'

// 统计数据
const componentCount = ref(18)
const testCount = ref(8)
const composableCount = ref(7)
const directiveCount = ref(4)

// 布局组件
const layoutComponents = ref([
  { name: 'MainLayout', desc: '主布局容器', path: '/layout/main-layout' },
  { name: 'Sidebar', desc: '侧边栏导航', path: '/layout/sidebar' },
  { name: 'Header', desc: '头部导航栏', path: '/layout/header' },
  { name: 'TabsView', desc: '标签页视图', path: '/layout/tabs-view' },
  { name: 'Breadcrumb', desc: '面包屑导航', path: '/layout/breadcrumb' },
])

// 功能组件
const functionalComponents = ref([
  { name: 'ThemeToggle', desc: '主题切换按钮', path: '/functional/theme-toggle' },
  { name: 'LanguageSwitch', desc: '语言切换器', path: '/functional/language-switch' },
  { name: 'FullscreenToggle', desc: '全屏切换按钮', path: '/functional/fullscreen-toggle' },
  { name: 'ThemeSettings', desc: '主题设置面板', path: '/functional/theme-settings' },
  { name: 'CaptchaSlider', desc: '滑块验证码', path: '/functional/captcha-slider' },
  { name: 'IconSelector', desc: '图标选择器', path: '/functional/icon-selector' },
])

// 业务组件
const businessComponents = ref([
  { name: 'BlinkDialog', desc: '封装对话框', path: '/business/blink-dialog' },
  { name: 'BlinkTable', desc: '封装表格', path: '/business/blink-table' },
  { name: 'UserDropdown', desc: '用户下拉菜单', path: '/business/user-dropdown' },
])

// 测试工具
const testingTools = ref([
  { name: '测试覆盖率', desc: '查看代码覆盖率报告', path: '/testing/coverage' },
  { name: '组件测试', desc: '交互式组件测试', path: '/testing/component-test' },
])
</script>

<style scoped lang="scss">
.overview-page {
  .page-header {
    margin-bottom: 24px;
    h1 {
      font-size: 28px;
      color: #303133;
      margin-bottom: 8px;
    }
    p {
      color: #909399;
      font-size: 14px;
    }
  }
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.stat-icon {
  flex-shrink: 0;
}

.stat-content {
  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
  }
  .stat-label {
    font-size: 14px;
    color: #909399;
  }
}

.category-card {
  height: 100%;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
}

.component-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.component-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  text-decoration: none;
  color: #303133;
  transition: all 0.2s;

  &:hover {
    background: #ecf5ff;
    color: #409eff;
    transform: translateX(4px);
  }

  .component-name {
    font-weight: 500;
  }

  .component-desc {
    font-size: 13px;
    color: #909399;
  }
}
</style>