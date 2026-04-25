<template>
  <div class="overview-page">
    <div class="page-header">
      <div class="header-content">
        <h1>Blink Components 组件库</h1>
        <p>一个基于 Vue 3 + TypeScript + Element Plus 的企业级组件库</p>
      </div>
      <div class="header-actions">
        <el-tag type="success" effect="dark" size="large">
          <el-icon><Check /></el-icon>
          71 测试通过
        </el-tag>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="24" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon-wrapper layout-icon">
              <el-icon :size="32"><Grid /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ layoutCount }}</div>
              <div class="stat-label">布局组件</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon-wrapper functional-icon">
              <el-icon :size="32"><Tools /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ functionalCount }}</div>
              <div class="stat-label">功能组件</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon-wrapper business-icon">
              <el-icon :size="32"><Briefcase /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ businessCount }}</div>
              <div class="stat-label">业务组件</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-icon-wrapper test-icon">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ testCount }}</div>
              <div class="stat-label">测试用例</div>
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
              <div class="category-icon layout-icon">
                <el-icon :size="24"><Grid /></el-icon>
              </div>
              <span>布局组件</span>
              <el-tag size="small" type="info">{{ layoutComponents.length }} 个</el-tag>
            </div>
          </template>
          <div class="component-list">
            <router-link
              v-for="comp in layoutComponents"
              :key="comp.path"
              :to="comp.path"
              class="component-item"
            >
              <div class="item-left">
                <el-icon class="item-icon"><component :is="comp.icon" /></el-icon>
                <span class="component-name">{{ comp.name }}</span>
              </div>
              <span class="component-desc">{{ comp.desc }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <div class="category-icon functional-icon">
                <el-icon :size="24"><Tools /></el-icon>
              </div>
              <span>功能组件</span>
              <el-tag size="small" type="info">{{ functionalComponents.length }} 个</el-tag>
            </div>
          </template>
          <div class="component-list">
            <router-link
              v-for="comp in functionalComponents"
              :key="comp.path"
              :to="comp.path"
              class="component-item"
            >
              <div class="item-left">
                <el-icon class="item-icon"><component :is="comp.icon" /></el-icon>
                <span class="component-name">{{ comp.name }}</span>
              </div>
              <span class="component-desc">{{ comp.desc }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px">
      <el-col :span="12">
        <el-card class="category-card business-card">
          <template #header>
            <div class="category-header">
              <div class="category-icon business-icon">
                <el-icon :size="24"><Briefcase /></el-icon>
              </div>
              <span>业务组件</span>
              <el-tag size="small" type="info">{{ businessComponents.length }} 个</el-tag>
            </div>
          </template>
          <div class="component-list">
            <router-link
              v-for="comp in businessComponents"
              :key="comp.path"
              :to="comp.path"
              class="component-item"
              :class="{ 'new-item': comp.isNew }"
            >
              <div class="item-left">
                <el-icon class="item-icon"><component :is="comp.icon" /></el-icon>
                <span class="component-name">{{ comp.name }}</span>
                <el-tag v-if="comp.isNew" type="success" size="small" effect="dark">NEW</el-tag>
              </div>
              <span class="component-desc">{{ comp.desc }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <div class="category-icon test-icon">
                <el-icon :size="24"><Monitor /></el-icon>
              </div>
              <span>测试工具</span>
              <el-tag size="small" type="info">{{ testingTools.length }} 个</el-tag>
            </div>
          </template>
          <div class="component-list">
            <router-link
              v-for="comp in testingTools"
              :key="comp.path"
              :to="comp.path"
              class="component-item"
            >
              <div class="item-left">
                <el-icon class="item-icon"><component :is="comp.icon" /></el-icon>
                <span class="component-name">{{ comp.name }}</span>
              </div>
              <span class="component-desc">{{ comp.desc }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Composables & Directives -->
    <el-row :gutter="24" style="margin-top: 24px">
      <el-col :span="12">
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <div class="category-icon composable-icon">
                <el-icon :size="24"><Operation /></el-icon>
              </div>
              <span>Composables</span>
              <el-tag size="small" type="info">{{ composableCount }} 个</el-tag>
            </div>
          </template>
          <div class="composable-list">
            <div v-for="item in composables" :key="item.name" class="composable-item">
              <span class="composable-name">{{ item.name }}</span>
              <span class="composable-desc">{{ item.desc }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="category-card">
          <template #header>
            <div class="category-header">
              <div class="category-icon directive-icon">
                <el-icon :size="24"><Star /></el-icon>
              </div>
              <span>指令</span>
              <el-tag size="small" type="info">{{ directiveCount }} 个</el-tag>
            </div>
          </template>
          <div class="composable-list">
            <div v-for="item in directives" :key="item.name" class="composable-item">
              <span class="composable-name">{{ item.name }}</span>
              <span class="composable-desc">{{ item.desc }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  Grid,
  Document,
  Tools,
  Monitor,
  Briefcase,
  Check,
  Operation,
  Star,
  Menu,
  HomeFilled,
  UserFilled,
  FolderOpened,
  Sunny,
  Rank,
  FullScreen,
  Setting,
  Lock,
  PictureFilled,
  ChatDotSquare,
  Tickets,
} from '@element-plus/icons-vue'

// 统计数据
const layoutCount = ref(5)
const functionalCount = ref(6)
const businessCount = ref(4)
const testCount = ref(71)

// 布局组件
const layoutComponents = ref([
  { name: 'MainLayout', desc: '主布局容器', path: '/layout/main-layout', icon: Grid },
  { name: 'Sidebar', desc: '侧边栏导航', path: '/layout/sidebar', icon: Menu },
  { name: 'Header', desc: '头部导航栏', path: '/layout/header', icon: HomeFilled },
  { name: 'TabsView', desc: '标签页视图', path: '/layout/tabs-view', icon: FolderOpened },
  { name: 'Breadcrumb', desc: '面包屑导航', path: '/layout/breadcrumb', icon: Rank },
])

// 功能组件
const functionalComponents = ref([
  { name: 'ThemeToggle', desc: '主题切换按钮', path: '/functional/theme-toggle', icon: Sunny },
  { name: 'LanguageSwitch', desc: '语言切换器', path: '/functional/language-switch', icon: ChatDotSquare },
  { name: 'FullscreenToggle', desc: '全屏切换按钮', path: '/functional/fullscreen-toggle', icon: FullScreen },
  { name: 'ThemeSettings', desc: '主题设置面板', path: '/functional/theme-settings', icon: Setting },
  { name: 'CaptchaSlider', desc: '滑块验证码', path: '/functional/captcha-slider', icon: Lock },
  { name: 'IconSelector', desc: '图标选择器', path: '/functional/icon-selector', icon: PictureFilled },
])

// 业务组件
const businessComponents = ref([
  { name: 'BlinkDialog', desc: '封装对话框', path: '/business/blink-dialog', icon: ChatDotSquare },
  { name: 'BlinkTable', desc: '封装表格', path: '/business/blink-table', icon: Tickets },
  { name: 'UserDropdown', desc: '用户下拉菜单', path: '/business/user-dropdown', icon: UserFilled },
  { name: 'BlinkTaskDialog', desc: '任务进度弹窗', path: '/business/blink-task-dialog', icon: Operation, isNew: true },
])

// 测试工具
const testingTools = ref([
  { name: '测试覆盖率', desc: '查看代码覆盖率报告', path: '/testing/coverage', icon: Document },
  { name: '组件测试', desc: '交互式组件测试', path: '/testing/component-test', icon: Monitor },
])

// Composables
const composableCount = ref(8)
const composables = ref([
  { name: 'useSidebarState', desc: '侧边栏状态管理' },
  { name: 'useTabsState', desc: '标签页状态管理' },
  { name: 'useHeaderState', desc: '头部状态管理' },
  { name: 'useLayoutState', desc: '布局状态管理' },
  { name: 'useThemeSettings', desc: '主题设置管理' },
  { name: 'useSubmitGuard', desc: '提交防抖保护' },
  { name: 'useTransition', desc: '数据过渡动画' },
  { name: 'useTaskRunner', desc: '任务状态管理' },
])

// 指令
const directiveCount = ref(4)
const directives = ref([
  { name: 'dataFadeDirective', desc: '数据加载淡入效果' },
  { name: 'listFadeDirective', desc: '列表数据过渡' },
  { name: 'tableFadeDirective', desc: '表格数据过渡' },
  { name: 'rippleDirective', desc: '按钮涟漪效果' },
])
</script>

<style scoped lang="scss">
.overview-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding: 20px 24px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    color: #fff;

    .header-content {
      h1 {
        font-size: 28px;
        margin-bottom: 8px;
        font-weight: 600;
      }
      p {
        font-size: 14px;
        opacity: 0.9;
      }
    }

    .header-actions {
      .el-tag {
        font-weight: 500;
      }
    }
  }
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-card-wrapper {
  :deep(.el-card__body) {
    padding: 20px;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;

  &.layout-icon {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }
  &.functional-icon {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }
  &.business-icon {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  }
  &.test-icon {
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  }
  &.composable-icon {
    background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  }
  &.directive-icon {
    background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
    color: #666;
  }
}

.stat-content {
  .stat-value {
    font-size: 32px;
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

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.el-card__body) {
    padding: 16px 20px;
  }
}

.business-card {
  :deep(.el-card__header) {
    background: linear-gradient(135deg, rgba(79, 172, 254, 0.1) 0%, rgba(0, 242, 254, 0.1) 100%);
  }
}

.category-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 500;

  span {
    flex: 1;
  }
}

.category-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
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
  padding: 14px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  text-decoration: none;
  color: #303133;
  transition: all 0.3s ease;
  border: 1px solid transparent;

  &:hover {
    background: #fff;
    color: #409eff;
    border-color: #409eff;
    transform: translateX(4px);
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  }

  &.new-item {
    background: linear-gradient(135deg, rgba(67, 233, 123, 0.1) 0%, rgba(56, 249, 215, 0.1) 100%);
    border-color: rgba(67, 233, 123, 0.3);

    &:hover {
      background: #fff;
      border-color: #67c23a;
    }
  }
}

.item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-icon {
  color: #909399;
}

.component-item:hover .item-icon {
  color: #409eff;
}

.component-name {
  font-weight: 500;
}

.component-desc {
  font-size: 13px;
  color: #909399;
}

.composable-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.composable-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.composable-name {
  font-weight: 500;
  color: #303133;
  font-family: 'SF Mono', Monaco, monospace;
}

.composable-desc {
  font-size: 13px;
  color: #909399;
}
</style>