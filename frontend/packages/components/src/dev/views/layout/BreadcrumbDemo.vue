<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>面包屑 (Breadcrumb)</h2>
      <p>显示当前页面在导航层级中的位置</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <el-space direction="vertical" :size="24" style="width: 100%">
        <div>
          <h4 style="margin-bottom: 12px">基础用法</h4>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>系统管理</el-breadcrumb-item>
            <el-breadcrumb-item>用户管理</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div>
          <h4 style="margin-bottom: 12px">带图标</h4>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>
              <el-icon><HomeFilled /></el-icon>
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item>
              <el-icon><Setting /></el-icon>
              系统管理
            </el-breadcrumb-item>
            <el-breadcrumb-item>
              <el-icon><User /></el-icon>
              用户管理
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div>
          <h4 style="margin-bottom: 12px">自定义分隔符</h4>
          <el-breadcrumb separator=">">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>系统管理</el-breadcrumb-item>
            <el-breadcrumb-item>用户管理</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </el-space>
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
import { HomeFilled, Setting, User } from '@element-plus/icons-vue'

const propsData = [
  { name: 'items', desc: '面包屑数据', type: 'BreadcrumbItem[]' },
  { name: 'separator', desc: '分隔符', type: 'string' },
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