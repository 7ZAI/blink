<template>
  <div class="demo-page">
    <div class="demo-header">
      <h2>表格 (BlinkTable)</h2>
      <p>封装的表格组件，支持分页、选择、排序等功能</p>
    </div>

    <el-card>
      <template #header>组件预览</template>
      <div class="table-toolbar">
        <el-space>
          <el-button type="primary" :icon="Plus">新增</el-button>
          <el-button :icon="Delete" :disabled="!selectedRows.length">删除</el-button>
          <el-button :icon="Download">导出</el-button>
        </el-space>
        <el-input
          v-model="searchKey"
          placeholder="搜索..."
          style="width: 200px"
          :prefix-icon="Search"
          clearable
        />
      </div>
      <el-table
        :data="tableData"
        border
        stripe
        @selection-change="selectedRows = $event"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default>
            <el-button type="primary" link>编辑</el-button>
            <el-button type="danger" link>删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="100"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
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
import { ref } from 'vue'
import { Plus, Delete, Download, Search } from '@element-plus/icons-vue'

const searchKey = ref('')
const selectedRows = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)

const tableData = ref([
  { id: 1, name: '系统管理', status: 1, createTime: '2024-01-01 10:00:00' },
  { id: 2, name: '用户管理', status: 1, createTime: '2024-01-02 11:00:00' },
  { id: 3, name: '角色管理', status: 1, createTime: '2024-01-03 12:00:00' },
  { id: 4, name: '菜单管理', status: 0, createTime: '2024-01-04 13:00:00' },
  { id: 5, name: '日志管理', status: 1, createTime: '2024-01-05 14:00:00' },
])

const propsData = [
  { name: 'data', desc: '表格数据', type: 'any[]' },
  { name: 'columns', desc: '列配置', type: 'TableColumn[]' },
  { name: 'loading', desc: '加载状态', type: 'boolean' },
  { name: 'pagination', desc: '分页配置', type: 'PaginationConfig' },
  { name: 'selection', desc: '是否显示选择列', type: 'boolean' },
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

.table-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>