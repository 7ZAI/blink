<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>语言切换 (LanguageSwitch)</h2>
      <p>多语言切换组件</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <el-row :gutter="24">
        <el-col :span="12">
          <h4 style="margin-bottom: 12px">下拉菜单样式</h4>
          <el-dropdown>
            <el-button>
              <el-icon><Globe /></el-icon>
              简体中文
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>简体中文</el-dropdown-item>
                <el-dropdown-item>English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-col>
        <el-col :span="12">
          <h4 style="margin-bottom: 12px">按钮组样式</h4>
          <el-button-group>
            <el-button type="primary">中文</el-button>
            <el-button>EN</el-button>
          </el-button-group>
        </el-col>
      </el-row>
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
import { Globe, ArrowDown } from '@element-plus/icons-vue'

const propsData = [
  { name: 'locale', desc: '当前语言', type: 'string' },
  { name: 'locales', desc: '语言列表', type: 'LanguageOption[]' },
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
</style>