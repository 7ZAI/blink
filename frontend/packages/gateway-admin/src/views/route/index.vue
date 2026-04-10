<template>
  <!-- 路由管理页面 -->
  <div class="route-management table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('route.routeGroup')">
          <el-input
            v-model.trim="searchForm.routesGroup"
            :placeholder="t('route.routeGroupPlaceholder')"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            style="height: 28px; padding: 0 12px; font-size: 13px"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            {{ t('common.search') }}
          </el-button>
          <el-button style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleReset">
            <el-icon><Refresh /></el-icon>
            {{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card flex-1 flex flex-col overflow-hidden" shadow="never">
      <template #header>
        <div class="table-header">
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.Route.Add)"
            type="primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>
            {{ t('route.addRoute') }}
          </AuthButton>
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.Route.Refresh)"
            type="success"
            @click="handleRefreshRoutes"
          >
            <el-icon><Refresh /></el-icon>
            {{ t('dashboard.refreshRoutes') }}
          </AuthButton>
        </div>
      </template>

      <!-- 表格区域 -->
      <div class="table-wrapper">
        <el-table v-loading="loading" :data="tableData" height="100%" stripe>
          <el-table-column
            prop="id"
            :label="t('route.routeId')"
            min-width="160"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span class="route-id">{{ row.id || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="uri" :label="t('route.uri')" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag type="success" effect="plain" size="small">{{ row.uri || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('route.predicates')" min-width="200">
            <template #default="{ row }">
              <div class="predicate-tags">
                <el-tooltip
                  v-for="(p, index) in row.predicates"
                  :key="index"
                  :content="formatPredicateArgs(p)"
                  placement="top"
                >
                  <el-tag class="predicate-tag" type="primary" effect="light" size="small">
                    {{ p.name }}
                  </el-tag>
                </el-tooltip>
                <span v-if="!row.predicates?.length" class="text-gray-400">-</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('route.filters')" min-width="160">
            <template #default="{ row }">
              <div class="filter-tags">
                <el-tag
                  v-for="(f, index) in row.filters"
                  :key="index"
                  class="filter-tag"
                  type="warning"
                  effect="light"
                  size="small"
                >
                  {{ f.name }}
                </el-tag>
                <span v-if="!row.filters?.length" class="text-gray-400">-</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('route.order')" width="80" align="center">
            <template #default="{ row }">
              <el-tag type="info" effect="plain" size="small">{{ row.order || 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="180" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.Route.Edit)"
                  type="primary"
                  link
                  size="small"
                  @click="handleEdit(row)"
                >
                  <el-icon><Edit /></el-icon>
                  {{ t('common.edit') }}
                </AuthButton>
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.Route.Delete)"
                  type="danger"
                  link
                  size="small"
                  @click="handleDelete(row)"
                >
                  <el-icon><Delete /></el-icon>
                  {{ t('common.delete') }}
                </AuthButton>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
          <template #total="{ total }">
            {{ t('pagination.total', { total }) }}
          </template>
        </el-pagination>
      </div>
    </el-card>

    <!-- Form Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="900px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="route-dialog"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('route.routeGroup')" prop="routesGroup">
              <el-input
                v-model.trim="formData.routesGroup"
                :placeholder="t('route.routeGroupPlaceholder')"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('route.routeId')" prop="routeId">
              <el-input v-model="currentRoute.id" :placeholder="t('route.routeIdPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="18">
            <el-form-item :label="t('route.uri')" prop="uri">
              <el-input
                v-model="currentRoute.uri"
                placeholder="lb://service-name 或 https://example.com"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item :label="t('route.order')">
              <el-input-number v-model="currentRoute.order" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 编辑模式切换 -->
        <el-divider>
          <el-radio-group v-model="editMode" size="small">
            <el-radio-button value="form">{{ t('common.show') }}</el-radio-button>
            <el-radio-button value="json">JSON</el-radio-button>
          </el-radio-group>
        </el-divider>

        <!-- 表单模式 -->
        <template v-if="editMode === 'form'">
          <!-- Predicates -->
          <el-form-item :label="t('route.predicates')">
            <div class="dynamic-section">
              <div
                v-for="(predicate, index) in currentRoute.predicates"
                :key="index"
                class="dynamic-item"
              >
                <el-select
                  v-model="predicate.name"
                  placeholder="Type"
                  style="width: 160px"
                  @change="onPredicateChange(predicate)"
                >
                  <el-option
                    v-for="p in predicateTypes"
                    :key="p.value"
                    :label="p.label"
                    :value="p.value"
                  />
                </el-select>
                <div class="args-input">
                  <template v-if="predicate.name === 'Path'">
                    <el-input v-model="predicate.args.pattern" placeholder="/api/**" />
                  </template>
                  <template v-else-if="predicate.name === 'Method'">
                    <el-select
                      v-model="predicate.args.methods"
                      multiple
                      placeholder="GET, POST"
                      style="width: 100%"
                    >
                      <el-option label="GET" value="GET" />
                      <el-option label="POST" value="POST" />
                      <el-option label="PUT" value="PUT" />
                      <el-option label="DELETE" value="DELETE" />
                      <el-option label="PATCH" value="PATCH" />
                    </el-select>
                  </template>
                  <template v-else-if="predicate.name === 'Header'">
                    <el-input
                      v-model="predicate.args.header"
                      placeholder="X-Request-Id"
                      style="width: 150px"
                    />
                    <el-input
                      v-model="predicate.args.regexp"
                      placeholder="正则表达式"
                      class="flex-1"
                    />
                  </template>
                  <template v-else-if="predicate.name === 'Query'">
                    <el-input
                      v-model="predicate.args.param"
                      placeholder="参数名"
                      style="width: 150px"
                    />
                    <el-input
                      v-model="predicate.args.regexp"
                      placeholder="正则表达式(可选)"
                      class="flex-1"
                    />
                  </template>
                  <template v-else-if="predicate.name === 'Host'">
                    <el-input v-model="predicate.args.pattern" placeholder="**.example.com" />
                  </template>
                  <template v-else>
                    <el-input v-model="predicate.args.pattern" placeholder="参数值" />
                  </template>
                </div>
                <el-button type="danger" :icon="Delete" circle @click="removePredicate(index)" />
              </div>
              <el-button type="primary" :icon="Plus" @click="addPredicate">
                {{ t('route.addPredicate') }}
              </el-button>
            </div>
          </el-form-item>

          <!-- Filters -->
          <el-form-item :label="t('route.filters')">
            <div class="dynamic-section">
              <div
                v-for="(filter, index) in currentRoute.filters"
                :key="index"
                class="dynamic-item"
              >
                <el-select
                  v-model="filter.name"
                  placeholder="Type"
                  style="width: 180px"
                  @change="onFilterChange(filter)"
                >
                  <el-option
                    v-for="f in filterTypes"
                    :key="f.value"
                    :label="f.label"
                    :value="f.value"
                  />
                </el-select>
                <div class="args-input">
                  <template v-if="filter.name === 'StripPrefix'">
                    <el-input-number
                      v-model="filter.args.parts"
                      :min="1"
                      placeholder="路径前缀数量"
                      style="width: 150px"
                    />
                  </template>
                  <template v-else-if="filter.name === 'AddRequestHeader'">
                    <el-input
                      v-model="filter.args.name"
                      placeholder="Header Name"
                      style="width: 150px"
                    />
                    <el-input
                      v-model="filter.args.value"
                      placeholder="Header Value"
                      class="flex-1"
                    />
                  </template>
                  <template v-else-if="filter.name === 'AddRequestParameter'">
                    <el-input
                      v-model="filter.args.name"
                      placeholder="Param Name"
                      style="width: 150px"
                    />
                    <el-input
                      v-model="filter.args.value"
                      placeholder="Param Value"
                      class="flex-1"
                    />
                  </template>
                  <template v-else-if="filter.name === 'RewritePath'">
                    <el-input
                      v-model="filter.args.regexp"
                      placeholder="正则表达式"
                      style="width: 200px"
                    />
                    <el-input
                      v-model="filter.args.replacement"
                      placeholder="替换路径"
                      class="flex-1"
                    />
                  </template>
                  <template v-else-if="filter.name === 'RequestRateLimiter'">
                    <el-input-number
                      v-model="filter.args.replenishRate"
                      :min="1"
                      placeholder="补充速率"
                      style="width: 120px"
                    />
                    <el-input-number
                      v-model="filter.args.burstCapacity"
                      :min="1"
                      placeholder="容量"
                      style="width: 120px"
                    />
                  </template>
                  <template v-else>
                    <el-input v-model="filter.args.args" placeholder="参数值" class="flex-1" />
                  </template>
                </div>
                <el-button type="danger" :icon="Delete" circle @click="removeFilter(index)" />
              </div>
              <el-button type="primary" :icon="Plus" @click="addFilter">
                {{ t('route.addFilter') }}
              </el-button>
            </div>
          </el-form-item>
        </template>

        <!-- JSON 模式 -->
        <template v-else>
          <el-form-item label="JSON">
            <el-input
              v-model="routeJson"
              type="textarea"
              :rows="15"
              placeholder="JSON format route definition"
              class="json-editor"
            />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 路由管理页面
 * 管理网关路由，包括创建、编辑、删除和刷新
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getRouteList,
  saveRoute,
  deleteRoute,
  refreshRoutes,
  type RouteDefinition,
  type PredicateDefinition,
  type FilterDefinition,
  type RouteForm,
} from '@/api/route'
import { ButtonPerms, usePermission } from '@/composables/usePermission'

defineOptions({
  name: 'RouteManagement',
})

const { hasPermission: checkPermission } = usePermission()

const { t } = useI18n()

// 搜索表单
const searchForm = reactive({
  routesGroup: '',
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

// 表格数据
const loading = ref(false)
const tableData = ref<RouteDefinition[]>([])

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const editMode = ref<'form' | 'json'>('form')
const routeJson = ref('')

const formData = reactive<RouteForm>({
  routesGroup: 'default',
  routes: [],
})

const currentRoute = reactive<RouteDefinition>({
  id: '',
  uri: '',
  predicates: [],
  filters: [],
  order: 0,
})

const formRules = {
  routesGroup: [{ required: true, message: () => t('route.routeGroupRequired'), trigger: 'blur' }],
  routeId: [{ required: true, message: () => t('route.routeIdPlaceholder'), trigger: 'blur' }],
  uri: [{ required: true, message: 'URI is required', trigger: 'blur' }],
}

const dialogTitle = computed(() => (isEdit.value ? t('route.editRoute') : t('route.addRoute')))

// 断言类型选项
const predicateTypes = [
  { label: 'Path', value: 'Path' },
  { label: 'Method', value: 'Method' },
  { label: 'Header', value: 'Header' },
  { label: 'Query', value: 'Query' },
  { label: 'Host', value: 'Host' },
  { label: 'Cookie', value: 'Cookie' },
  { label: 'After', value: 'After' },
  { label: 'Before', value: 'Before' },
  { label: 'Between', value: 'Between' },
  { label: 'RemoteAddr', value: 'RemoteAddr' },
]

// 过滤器类型选项
const filterTypes = [
  { label: 'StripPrefix', value: 'StripPrefix' },
  { label: 'AddRequestHeader', value: 'AddRequestHeader' },
  { label: 'AddResponseHeader', value: 'AddResponseHeader' },
  { label: 'AddRequestParameter', value: 'AddRequestParameter' },
  { label: 'RewritePath', value: 'RewritePath' },
  { label: 'RequestRateLimiter', value: 'RequestRateLimiter' },
  { label: 'CircuitBreaker', value: 'CircuitBreaker' },
  { label: 'Retry', value: 'Retry' },
  { label: 'PrefixPath', value: 'PrefixPath' },
  { label: 'SetPath', value: 'SetPath' },
]

// 格式化断言参数显示
const formatPredicateArgs = (predicate: PredicateDefinition): string => {
  if (!predicate.args) return ''
  const args = Object.entries(predicate.args)
    .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(', ') : v}`)
    .join(', ')
  return args
}

// 监听编辑模式切换
watch(editMode, (mode) => {
  if (mode === 'json') {
    routeJson.value = JSON.stringify(currentRoute, null, 2)
  } else {
    try {
      const parsed = JSON.parse(routeJson.value)
      Object.assign(currentRoute, parsed)
    } catch {
      // JSON 解析错误，忽略
    }
  }
})

/**
 * 加载路由列表数据
 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await getRouteList({
      routesGroup: searchForm.routesGroup,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    })
    tableData.value = res.routes || res.rows || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('[RouteManagement] Failed to load route list:', error)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/**
 * 处理搜索操作
 */
const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

/**
 * 处理重置搜索表单
 */
const handleReset = () => {
  searchForm.routesGroup = ''
  pagination.pageNum = 1
  loadData()
}

/**
 * 处理分页大小改变
 */
const handleSizeChange = () => {
  loadData()
}

/**
 * 处理页码改变
 */
const handleCurrentChange = () => {
  loadData()
}

/**
 * 处理新增路由
 */
const handleAdd = () => {
  isEdit.value = false
  editMode.value = 'form'
  Object.assign(currentRoute, {
    id: '',
    uri: '',
    predicates: [{ name: 'Path', args: { pattern: '' } }],
    filters: [],
    order: 0,
  })
  formData.routesGroup = 'default'
  dialogVisible.value = true
}

/**
 * 处理编辑路由
 * @param row - 路由信息
 */
const handleEdit = (row: RouteDefinition) => {
  isEdit.value = true
  editMode.value = 'form'
  const copyData = JSON.parse(JSON.stringify(row))
  // 确保 predicates 和 filters 存在
  copyData.predicates = copyData.predicates || []
  copyData.filters = copyData.filters || []
  // 确保每个断言和过滤器都有 args
  copyData.predicates.forEach((p: PredicateDefinition) => {
    p.args = p.args || {}
  })
  copyData.filters.forEach((f: FilterDefinition) => {
    f.args = f.args || {}
  })
  Object.assign(currentRoute, copyData)
  dialogVisible.value = true
}

/**
 * 处理表单提交
 */
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    submitting.value = true
    try {
      // 如果是 JSON 模式，解析 JSON
      let routeData = currentRoute
      if (editMode.value === 'json') {
        try {
          routeData = JSON.parse(routeJson.value)
        } catch {
          ElMessage.error('Invalid JSON format')
          return
        }
      }

      formData.routes = [routeData]
      await saveRoute(formData)
      ElMessage.success(t('message.success'))
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error('[RouteManagement] Failed to submit form:', error)
    } finally {
      submitting.value = false
    }
  })
}

/**
 * 弹窗关闭后重置表单
 */
const resetForm = () => {
  formRef.value?.resetFields()
}

/**
 * 处理删除路由
 * @param row - 路由信息
 */
const handleDelete = async (row: RouteDefinition) => {
  try {
    await ElMessageBox.confirm(t('route.deleteConfirm'), t('message.tips'), { type: 'warning' })
    await deleteRoute({
      routesGroup: searchForm.routesGroup || 'default',
      routeIds: [row.id],
    })
    ElMessage.success(t('message.deleteSuccess'))
    loadData()
  } catch {
    // 用户取消删除
  }
}

/**
 * 处理刷新路由
 */
const handleRefreshRoutes = async () => {
  try {
    await refreshRoutes()
    ElMessage.success(t('message.success'))
  } catch (error) {
    console.error('[RouteManagement] Failed to refresh routes:', error)
  }
}

const addPredicate = () => {
  currentRoute.predicates.push({ name: 'Path', args: { pattern: '' } })
}

const removePredicate = (index: number) => {
  currentRoute.predicates.splice(index, 1)
}

const onPredicateChange = (predicate: PredicateDefinition) => {
  // 重置参数
  predicate.args = {}
  if (predicate.name === 'Path') {
    predicate.args.pattern = ''
  } else if (predicate.name === 'Method') {
    predicate.args.methods = []
  }
}

const addFilter = () => {
  currentRoute.filters.push({ name: 'StripPrefix', args: { parts: 1 } })
}

const removeFilter = (index: number) => {
  currentRoute.filters.splice(index, 1)
}

const onFilterChange = (filter: FilterDefinition) => {
  // 重置参数
  filter.args = {}
  if (filter.name === 'StripPrefix') {
    filter.args.parts = 1
  }
}

// 组件挂载时加载初始数据
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
/* 路由管理页面 - 继承全局 table-page-container 样式 */

.route-id {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
}

.predicate-tags,
.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.predicate-tag,
.filter-tag {
  cursor: pointer;
}
</style>

<style lang="scss">
/* 路由弹窗样式（非 scoped） */
.route-dialog {
  .dynamic-section {
    .dynamic-item {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      padding: 12px;
      background: var(--bg-color-page);
      border-radius: 8px;

      .args-input {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 8px;

        .flex-1 {
          flex: 1;
        }
      }
    }
  }

  .json-editor {
    .el-textarea__inner {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      font-size: 13px;
      line-height: 1.6;
    }
  }
}
</style>
