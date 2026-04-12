<template>
  <!-- 路由管理页面 -->
  <div class="route-management table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <!-- 存储方式选择 -->
        <el-form-item :label="t('route.storageMode')">
          <el-select
            v-model="searchForm.storageMode"
            :placeholder="t('route.storageModePlaceholder')"
            style="width: 160px"
            @change="handleStorageModeChange"
          >
            <el-option value="redis">
              <div class="storage-mode-option">
                <span>{{ t('route.redisStorage') }}</span>
              </div>
            </el-option>
            <el-option value="nacos">
              <div class="storage-mode-option">
                <span>{{ t('route.nacosStorage') }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- Redis 模式：路由分组 -->
        <el-form-item v-if="searchForm.storageMode === 'redis'" :label="t('route.routeGroup')">
          <el-input
            v-model.trim="searchForm.routesGroup"
            :placeholder="t('route.routeGroupPlaceholder')"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <!-- Redis 模式：路由名称 -->
        <el-form-item v-if="searchForm.storageMode === 'redis'" :label="t('route.routeName')">
          <el-input
            v-model.trim="searchForm.routeName"
            :placeholder="t('route.routeNamePlaceholder')"
            clearable
            style="width: 150px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <!-- Nacos 模式：Data ID 和 Group -->
        <el-form-item v-if="searchForm.storageMode === 'nacos'" :label="t('route.nacosDataId')">
          <el-input
            v-model.trim="searchForm.nacosDataId"
            :placeholder="t('route.nacosDataIdPlaceholder')"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item v-if="searchForm.storageMode === 'nacos'" :label="t('route.nacosGroup')">
          <el-input
            v-model.trim="searchForm.nacosGroup"
            :placeholder="t('route.nacosGroupPlaceholder')"
            clearable
            style="width: 150px"
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
            :disabled="selectedRoutes.length === 0"
            @click="handlePushSelected"
          >
            <el-icon><Promotion /></el-icon>
            {{ t('route.pushSelected') }} ({{ selectedRoutes.length }})
          </AuthButton>
          <!-- 新增按钮 -->
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.Route.BatchStatus)"
            type="default"
            :disabled="selectedRoutes.length === 0"
            @click="handleBatchStatus"
          >
            <el-icon><Switch /></el-icon>
            {{ t('route.batchStatus') }} ({{ selectedRoutes.length }})
          </AuthButton>
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.Route.FullPush)"
            type="warning"
            @click="handleFullPush"
          >
            <el-icon><Upload /></el-icon>
            {{ t('route.fullPush') }}
          </AuthButton>
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.Route.Import)"
            type="default"
            @click="handleImport"
          >
            <el-icon><Download /></el-icon>
            {{ t('route.importRoutes') }}
          </AuthButton>
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.Route.Export)"
            type="default"
            :disabled="selectedRoutes.length === 0"
            @click="handleExport"
          >
            <el-icon><UploadFilled /></el-icon>
            {{ t('route.exportRoutes') }} ({{ selectedRoutes.length }})
          </AuthButton>
          <AuthButton
            :has-permission="() => checkPermission(ButtonPerms.Route.Refresh)"
            type="warning"
            @click="handleSyncToInstances"
          >
            <el-icon><Connection /></el-icon>
            {{ t('route.syncToInstances') }}
          </AuthButton>
        </div>
      </template>

      <!-- 表格区域 -->
      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="tableData"
          height="100%"
          stripe
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column
            prop="routeId"
            :label="t('route.routeId')"
            min-width="160"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span class="route-id">{{ row.routeId || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="routeName" :label="t('route.routeName')" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.routeName || '-' }}</span>
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
                  :content="formatConfigArgs(p)"
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
              <el-tag type="info" effect="plain" size="small">{{ row.orderNum || 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('route.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain" size="small">
                {{ row.status === 1 ? t('route.statusEnable') : t('route.statusDisable') }}
              </el-tag>
            </template>
          </el-table-column>
          <!-- 新增：推送状态列 -->
          <el-table-column :label="t('route.pushStatus')" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                :type="getPushStatusType(row.pushStatus)"
                effect="plain"
                size="small"
              >
                {{ getPushStatusText(row.pushStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <!-- 新增：最后推送时间列 -->
          <el-table-column :label="t('route.lastPushTime')" width="160">
            <template #default="{ row }">
              <span class="time-text">{{ row.lastPushTime || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="280" fixed="right">
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
                  :has-permission="() => checkPermission(ButtonPerms.Route.Clone)"
                  type="info"
                  link
                  size="small"
                  @click="handleClone(row)"
                >
                  <el-icon><DocumentCopy /></el-icon>
                  {{ t('route.cloneRoute') }}
                </AuthButton>
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.Route.Edit)"
                  type="info"
                  link
                  size="small"
                  @click="handleHistory(row)"
                >
                  <el-icon><Clock /></el-icon>
                  {{ t('route.history') }}
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
            <el-form-item :label="t('route.routeId')" prop="routeId">
              <el-input
                v-model="formData.routeId"
                :placeholder="t('route.routeIdPlaceholder')"
                :disabled="isEdit"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('route.routeName')">
              <el-input v-model="formData.routeName" :placeholder="t('route.routeNamePlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>

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
            <el-form-item :label="t('route.uri')" prop="uri">
              <el-input
                v-model="formData.uri"
                :placeholder="t('route.uriPlaceholder')"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('route.order')">
              <el-input-number v-model="formData.orderNum" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('route.status')">
              <el-switch
                v-model="formData.status"
                :active-value="1"
                :inactive-value="0"
                :active-text="t('route.statusEnable')"
                :inactive-text="t('route.statusDisable')"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item :label="t('route.remark')">
              <el-input v-model="formData.remark" :placeholder="t('route.remarkPlaceholder')" />
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
                v-for="(predicate, index) in formData.predicates"
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
                  <template v-else-if="predicate.name === 'Custom'">
                    <el-input
                      v-model="predicate.customName"
                      :placeholder="t('route.customNamePlaceholder')"
                      style="width: 150px"
                    />
                    <el-input
                      v-model="predicate.customArgsJson"
                      :placeholder="t('route.customArgsPlaceholder')"
                      class="flex-1"
                    />
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
                v-for="(filter, index) in formData.filters"
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
                  <template v-else-if="filter.name === 'Custom'">
                    <el-input
                      v-model="filter.customName"
                      :placeholder="t('route.customNamePlaceholder')"
                      style="width: 150px"
                    />
                    <el-input
                      v-model="filter.customArgsJson"
                      :placeholder="t('route.customArgsPlaceholder')"
                      class="flex-1"
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

    <!-- 历史记录弹窗 -->
    <el-dialog
      v-model="historyDialogVisible"
      :title="t('route.historyTitle')"
      width="800px"
      :close-on-click-modal="false"
      class="history-dialog"
    >
      <el-table v-loading="historyLoading" :data="historyData" height="400" stripe>
        <el-table-column prop="historyId" :label="t('route.historyId')" width="100" />
        <el-table-column prop="routeName" :label="t('route.routeName')" width="120" />
        <el-table-column prop="operationType" :label="t('route.operationType')" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperationTypeTag(row.operationType)" effect="plain" size="small">
              {{ getOperationTypeLabel(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" :label="t('route.operatorName')" width="120" />
        <el-table-column prop="operateTime" :label="t('route.operateTime')" width="160" />
        <el-table-column :label="t('common.operation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.operationType !== 'A'"
              type="primary"
              link
              size="small"
              @click="handleRollback(row)"
            >
              {{ t('route.rollback') }}
            </el-button>
            <el-button type="info" link size="small" @click="handleViewHistoryDetail(row)">
              {{ t('route.viewDetail') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper mt-4">
        <el-pagination
          v-model:current-page="historyPagination.pageNum"
          v-model:page-size="historyPagination.pageSize"
          :total="historyPagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadHistoryData"
          @current-change="loadHistoryData"
        />
      </div>
    </el-dialog>

    <!-- 同步到实例弹窗 -->
    <SyncInstanceDialog
      v-model="syncDialogVisible"
      :storage-mode="searchForm.storageMode"
      :routes-group="searchForm.routesGroup"
      :data-id="searchForm.nacosDataId"
      :group="searchForm.nacosGroup"
      :route-ids="selectedRouteIds"
      @success="handleSyncSuccess"
    />

    <!-- 新增：批量状态更新弹窗 -->
    <el-dialog
      v-model="batchStatusDialogVisible"
      :title="t('route.batchStatus')"
      width="400px"
      :close-on-click-modal="false"
    >
      <div class="batch-status-tip">
        {{ t('route.batchStatusTip', { count: selectedRoutes.length }) }}
      </div>
      <el-form label-width="80px">
        <el-form-item :label="t('route.targetStatus')">
          <el-radio-group v-model="batchStatusValue">
            <el-radio :value="1">
              <el-tag type="success" effect="plain" size="small">{{ t('route.statusEnable') }}</el-tag>
            </el-radio>
            <el-radio :value="0">
              <el-tag type="danger" effect="plain" size="small">{{ t('route.statusDisable') }}</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchStatusDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="batchStatusLoading" @click="handleBatchStatusSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增：导入路由弹窗 -->
    <el-dialog
      v-model="importDialogVisible"
      :title="t('route.importRoutes')"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="import-tip">{{ t('route.importTip') }}</div>
      <el-form label-width="100px">
        <el-form-item :label="t('route.routesGroup')">
          <el-input v-model="importRoutesGroup" :placeholder="t('route.routeGroupPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('route.importData')">
          <el-input
            v-model="importJsonData"
            type="textarea"
            :rows="10"
            placeholder="JSON format route array"
          />
        </el-form-item>
        <el-form-item :label="t('route.overwrite')">
          <el-switch v-model="importOverwrite" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImportSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增：克隆路由弹窗 -->
    <el-dialog
      v-model="cloneDialogVisible"
      :title="t('route.cloneRoute')"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="clone-tip">
        {{ t('route.cloneTip', { routeId: cloneSourceRouteId }) }}
      </div>
      <el-form label-width="100px">
        <el-form-item :label="t('route.newRouteId')">
          <el-input v-model="cloneNewRouteId" :placeholder="t('route.newRouteIdPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('route.newRouteName')">
          <el-input v-model="cloneNewRouteName" :placeholder="t('route.newRouteNamePlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cloneDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="cloneLoading" @click="handleCloneSubmit">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 路由管理页面
 * 管理网关路由，包括创建、编辑、删除、历史查询和回滚
 * 数据库为主存储 + Redis/Nacos 为运行时缓存
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Edit,
  Delete,
  Connection,
  Clock,
  Promotion,
  // 新增图标
  Switch,
  Upload,
  UploadFilled,
  Download,
  DocumentCopy,
} from '@element-plus/icons-vue'
import {
  getRouteList,
  saveRoute,
  updateRoute,
  deleteRoute,
  refreshRoutes,
  getRouteHistory,
  rollbackRoute,
  getNacosRouteList,
  saveNacosRoute,
  deleteNacosRoute,
  // 新增接口方法
  fullPushRoutes,
  batchUpdateStatus,
  exportRoutes,
  importRoutes,
  cloneRoute,
  type RouteDefinition,
  type PredicateConfig,
  type FilterConfig,
  type SaveRouteReq,
  type UpdateRouteReq,
  type DeleteRouteReq,
  type QueryRouteHistoryReq,
  type RollbackRouteReq,
  type RouteHistory,
  type SaveNacosRouteReq,
  type BatchUpdateStatusReq,
  type FullPushRoutesReq,
  type ImportRoutesReq,
  type ImportRoutesRsp,
  type CloneRouteReq,
} from '@/api/route'
import { ButtonPerms, usePermission } from '@/composables/usePermission'
import SyncInstanceDialog from './components/SyncInstanceDialog.vue'

defineOptions({
  name: 'RouteManagement',
})

const { hasPermission: checkPermission } = usePermission()

const { t } = useI18n()

// 存储方式常量
const STORAGE_MODE_KEY = 'route_storage_mode'

// 搜索表单
const searchForm = reactive({
  storageMode: localStorage.getItem(STORAGE_MODE_KEY) || 'redis',
  routesGroup: '',
  routeName: '',
  nacosDataId: t('route.nacosDataIdDefault'),
  nacosGroup: t('route.nacosGroupDefault'),
})

// 同步弹窗
const syncDialogVisible = ref(false)
const selectedRouteIds = ref<string[]>([])

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})

// 表格数据
const loading = ref(false)
const tableData = ref<RouteDefinition[]>([])
const selectedRoutes = ref<RouteDefinition[]>([])

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const editMode = ref<'form' | 'json'>('form')
const routeJson = ref('')

// 表单数据（扩展类型，确保 predicates 和 filters 均存在）
interface RouteFormData extends SaveRouteReq {
  predicates: PredicateConfig[]
  filters: FilterConfig[]
}

const formData = reactive<RouteFormData>({
  routeId: '',
  routeName: '',
  uri: '',
  predicates: [{ name: 'Path', args: { pattern: '' } }],
  filters: [],
  orderNum: 0,
  routesGroup: 'default',
  storageMode: 'redis',
  remark: '',
  status: 1,
})

const formRules = {
  routeId: [{ required: true, message: () => t('route.routeIdPlaceholder'), trigger: 'blur' }],
  routesGroup: [{ required: true, message: () => t('route.routeGroupRequired'), trigger: 'blur' }],
  uri: [{ required: true, message: () => t('route.uriRequired'), trigger: 'blur' }],
}

const dialogTitle = computed(() => (isEdit.value ? t('route.editRoute') : t('route.addRoute')))

// 历史弹窗
const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const historyData = ref<RouteHistory[]>([])
const historyPagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0,
})
const currentHistoryRouteId = ref('')

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
  { label: t('route.customPredicate'), value: 'Custom' },
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
  { label: t('route.customFilter'), value: 'Custom' },
]

// 格式化配置参数显示
const formatConfigArgs = (config: PredicateConfig | FilterConfig): string => {
  if (!config.args) return ''
  const args = Object.entries(config.args)
    .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(', ') : v}`)
    .join(', ')
  return args
}

// 获取操作类型标签
const getOperationTypeTag = (type: string): 'success' | 'warning' | 'danger' | 'info' => {
  switch (type) {
    case 'A':
      return 'success'
    case 'M':
      return 'warning'
    case 'D':
      return 'danger'
    default:
      return 'info'
  }
}

// 获取操作类型标签文本
const getOperationTypeLabel = (type: string): string => {
  switch (type) {
    case 'A':
      return t('route.operationAdd')
    case 'M':
      return t('route.operationModify')
    case 'D':
      return t('route.operationDelete')
    default:
      return type
  }
}

// 监听编辑模式切换
watch(editMode, (mode) => {
  if (mode === 'json') {
    routeJson.value = JSON.stringify(formData, null, 2)
  } else {
    try {
      const parsed = JSON.parse(routeJson.value)
      Object.assign(formData, parsed)
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
    if (searchForm.storageMode === 'redis') {
      // Redis 模式 - 从数据库查询
      const res = await getRouteList({
        routesGroup: searchForm.routesGroup,
        routeName: searchForm.routeName,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
      })
      tableData.value = res.rows || []
      pagination.total = res.total || 0
    } else {
      // Nacos 模式
      const res = await getNacosRouteList({
        dataId: searchForm.nacosDataId,
        group: searchForm.nacosGroup,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
      })
      tableData.value = res.rows || []
      pagination.total = res.total || 0
    }
  } catch (error) {
    console.error('[RouteManagement] Failed to load route list:', error)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/**
 * 加载路由历史数据
 */
const loadHistoryData = async () => {
  if (!currentHistoryRouteId.value) return

  historyLoading.value = true
  try {
    const req: QueryRouteHistoryReq = {
      routeId: currentHistoryRouteId.value,
      pageNum: historyPagination.pageNum,
      pageSize: historyPagination.pageSize,
    }
    const res = await getRouteHistory(req)
    historyData.value = res.rows || []
    historyPagination.total = res.total || 0
  } catch (error) {
    console.error('[RouteManagement] Failed to load route history:', error)
    historyData.value = []
    historyPagination.total = 0
  } finally {
    historyLoading.value = false
  }
}

/**
 * 处理存储方式切换
 */
const handleStorageModeChange = (mode: string) => {
  localStorage.setItem(STORAGE_MODE_KEY, mode)
  searchForm.routesGroup = ''
  searchForm.routeName = ''
  searchForm.nacosDataId = t('route.nacosDataIdDefault')
  searchForm.nacosGroup = t('route.nacosGroupDefault')
  pagination.pageNum = 1
  loadData()
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
  if (searchForm.storageMode === 'redis') {
    searchForm.routesGroup = ''
    searchForm.routeName = ''
  } else {
    searchForm.nacosDataId = t('route.nacosDataIdDefault')
    searchForm.nacosGroup = t('route.nacosGroupDefault')
  }
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
  resetFormData()
  formData.routesGroup = searchForm.routesGroup || 'default'
  dialogVisible.value = true
}

/**
 * 处理编辑路由
 */
const handleEdit = (row: RouteDefinition) => {
  isEdit.value = true
  editMode.value = 'form'
  const copyData = JSON.parse(JSON.stringify(row)) as RouteFormData
  // 转换字段名以适配表单
  formData.routeId = copyData.routeId
  formData.routeName = copyData.routeName || ''
  formData.uri = copyData.uri
  formData.predicates = copyData.predicates?.length
    ? copyData.predicates.map((p) => ({ ...p, args: p.args || {} }))
    : [{ name: 'Path', args: { pattern: '' } }]
  formData.filters = copyData.filters?.length
    ? copyData.filters.map((f) => ({ ...f, args: f.args || {} }))
    : []
  formData.orderNum = copyData.orderNum || 0
  formData.routesGroup = copyData.routesGroup || 'default'
  formData.storageMode = copyData.storageMode || 'redis'
  formData.status = copyData.status ?? 1
  formData.remark = copyData.remark || ''
  dialogVisible.value = true
}

/**
 * 处理查看历史
 */
const handleHistory = (row: RouteDefinition) => {
  currentHistoryRouteId.value = row.routeId
  historyPagination.pageNum = 1
  historyDialogVisible.value = true
  loadHistoryData()
}

/**
 * 处理回滚
 */
const handleRollback = async (row: RouteHistory) => {
  try {
    await ElMessageBox.confirm(
      t('route.rollbackConfirm', { routeName: row.routeName || currentHistoryRouteId.value }),
      t('message.tips'),
      { type: 'warning' }
    )

    const req: RollbackRouteReq = {
      routeId: currentHistoryRouteId.value,
      historyId: row.historyId,
      syncToStorage: true,
    }
    await rollbackRoute(req)

    ElMessage.success(t('message.success'))
    historyDialogVisible.value = false
    loadData()
  } catch {
    // 用户取消
  }
}

/**
 * 处理查看历史详情
 */
const handleViewHistoryDetail = (row: RouteHistory) => {
  // 显示详情，可以使用 JSON 格式展示 beforeData 或 afterData
  const detail = row.operationType === 'D' ? row.beforeData : row.afterData
  if (detail) {
    ElMessageBox.alert(JSON.stringify(detail, null, 2), t('route.historyDetailTitle'), {
      confirmButtonText: t('common.confirm'),
    })
  }
}

/**
 * 处理表单提交
 */
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    submitting.value = true

    // 如果是 JSON 模式，解析 JSON
    let submitData: SaveRouteReq | UpdateRouteReq = JSON.parse(JSON.stringify(formData))
    if (editMode.value === 'json') {
      try {
        submitData = JSON.parse(routeJson.value)
      } catch {
        ElMessage.error(t('route.invalidJsonFormat'))
        submitting.value = false
        return
      }
    }

    // 处理自定义类型的数据转换
    submitData = convertCustomTypes(submitData)

    if (searchForm.storageMode === 'redis') {
      if (isEdit.value) {
        // 更新路由
        await updateRoute(submitData as UpdateRouteReq)
      } else {
        // 新增路由
        await saveRoute(submitData as SaveRouteReq)
      }
    } else {
      // Nacos 模式
      const nacosReq: SaveNacosRouteReq = {
        dataId: searchForm.nacosDataId,
        group: searchForm.nacosGroup,
        routes: [submitData as RouteDefinition],
      }
      await saveNacosRoute(nacosReq)
    }

    ElMessage.success(t('route.pushSuccessTip'))
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[RouteManagement] Failed to submit form:', error)
  } finally {
    submitting.value = false
  }
}

/**
 * 转换自定义类型的断言和过滤器
 */
const convertCustomTypes = (data: SaveRouteReq | UpdateRouteReq): SaveRouteReq | UpdateRouteReq => {
  const converted = JSON.parse(JSON.stringify(data))

  if (converted.predicates && Array.isArray(converted.predicates)) {
    converted.predicates = converted.predicates.map((p: PredicateConfig) => {
      if (p.name === 'Custom' && p.customName) {
        const newPredicate: PredicateConfig = { name: p.customName, args: {} }
        if (p.customArgsJson) {
          try {
            newPredicate.args = JSON.parse(p.customArgsJson)
          } catch {
            newPredicate.args = {}
          }
        }
        return newPredicate
      }
      return p
    })
  }

  if (converted.filters && Array.isArray(converted.filters)) {
    converted.filters = converted.filters.map((f: FilterConfig) => {
      if (f.name === 'Custom' && f.customName) {
        const newFilter: FilterConfig = { name: f.customName, args: {} }
        if (f.customArgsJson) {
          try {
            newFilter.args = JSON.parse(f.customArgsJson)
          } catch {
            newFilter.args = {}
          }
        }
        return newFilter
      }
      return f
    })
  }

  return converted
}

/**
 * 重置表单数据
 */
const resetFormData = () => {
  formData.routeId = ''
  formData.routeName = ''
  formData.uri = ''
  formData.predicates = [{ name: 'Path', args: { pattern: '' } }]
  formData.filters = []
  formData.orderNum = 0
  formData.routesGroup = 'default'
  formData.storageMode = 'redis'
  formData.remark = ''
  formData.status = 1
}

/**
 * 弹窗关闭后重置表单
 */
const resetForm = () => {
  formRef.value?.resetFields()
  resetFormData()
}

/**
 * 处理删除路由
 */
const handleDelete = async (row: RouteDefinition) => {
  try {
    await ElMessageBox.confirm(t('route.deleteConfirm'), t('message.tips'), { type: 'warning' })

    if (searchForm.storageMode === 'redis') {
      const req: DeleteRouteReq = {
        routesGroup: searchForm.routesGroup || 'default',
        routeIds: [row.routeId],
      }
      await deleteRoute(req)
    } else {
      await deleteNacosRoute({
        dataId: searchForm.nacosDataId,
        group: searchForm.nacosGroup,
        routeIds: [row.routeId],
      })
    }

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

/**
 * 处理同步到实例
 */
const handleSyncToInstances = () => {
  selectedRouteIds.value = tableData.value.map((r) => r.routeId)
  syncDialogVisible.value = true
}

/**
 * 同步成功回调
 */
const handleSyncSuccess = () => {
  ElMessage.success(t('message.success'))
}

const addPredicate = () => {
  formData.predicates.push({ name: 'Path', args: { pattern: '' } })
}

const removePredicate = (index: number) => {
  formData.predicates.splice(index, 1)
}

const onPredicateChange = (predicate: PredicateConfig) => {
  predicate.args = {}
  if (predicate.name === 'Path') {
    predicate.args.pattern = ''
  } else if (predicate.name === 'Method') {
    predicate.args.methods = [] as string[]
  } else if (predicate.name === 'Custom') {
    predicate.customName = ''
    predicate.customArgsJson = ''
  }
}

const addFilter = () => {
  formData.filters.push({ name: 'StripPrefix', args: { parts: '1' } })
}

const removeFilter = (index: number) => {
  formData.filters.splice(index, 1)
}

const onFilterChange = (filter: FilterConfig) => {
  filter.args = {}
  if (filter.name === 'StripPrefix') {
    filter.args.parts = '1'
  } else if (filter.name === 'Custom') {
    filter.customName = ''
    filter.customArgsJson = ''
  }
}

/**
 * 处理表格选择变化
 */
const handleSelectionChange = (selection: RouteDefinition[]) => {
  selectedRoutes.value = selection
}

/**
 * 处理推送选中路由
 */
const handlePushSelected = () => {
  if (selectedRoutes.value.length === 0) {
    ElMessage.warning(t('route.selectRouteToPush'))
    return
  }
  selectedRouteIds.value = selectedRoutes.value.map((r) => r.routeId)
  syncDialogVisible.value = true
}

// ========== 新增功能：批量状态、全量推送、导入导出、克隆路由 ==========

// 批量状态更新弹窗状态
const batchStatusDialogVisible = ref(false)
const batchStatusValue = ref(1) // 1-启用 0-禁用
const batchStatusLoading = ref(false)

// 导入路由弹窗状态
const importDialogVisible = ref(false)
const importRoutesGroup = ref('default')
const importJsonData = ref('')
const importOverwrite = ref(false)
const importLoading = ref(false)

// 克隆路由弹窗状态
const cloneDialogVisible = ref(false)
const cloneSourceRouteId = ref('')
const cloneNewRouteId = ref('')
const cloneNewRouteName = ref('')
const cloneLoading = ref(false)

/**
 * 获取推送状态类型（用于Tag颜色）
 */
const getPushStatusType = (pushStatus: number | undefined): 'info' | 'success' | 'danger' | 'warning' => {
  if (pushStatus === undefined || pushStatus === 0) return 'info'
  if (pushStatus === 1) return 'success'
  if (pushStatus === 2) return 'danger'
  return 'warning'
}

/**
 * 获取推送状态文本
 */
const getPushStatusText = (pushStatus: number | undefined): string => {
  if (pushStatus === undefined || pushStatus === 0) return t('route.pushStatusNotPushed')
  if (pushStatus === 1) return t('route.pushStatusPushed')
  if (pushStatus === 2) return t('route.pushStatusFailed')
  return t('route.pushStatusUnknown')
}

/**
 * 处理批量状态更新按钮点击
 */
const handleBatchStatus = () => {
  if (selectedRoutes.value.length === 0) {
    ElMessage.warning(t('route.selectRouteToBatch'))
    return
  }
  batchStatusValue.value = 1
  batchStatusDialogVisible.value = true
}

/**
 * 处理批量状态更新提交
 */
const handleBatchStatusSubmit = async () => {
  batchStatusLoading.value = true
  try {
    const req: BatchUpdateStatusReq = {
      routeIds: selectedRoutes.value.map((r) => r.routeId),
      status: batchStatusValue.value,
      routesGroup: searchForm.routesGroup || 'default',
    }
    await batchUpdateStatus(req)
    ElMessage.success(t('message.success'))
    batchStatusDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[RouteManagement] Failed to batch update status:', error)
    ElMessage.error(t('message.operationFailed'))
  } finally {
    batchStatusLoading.value = false
  }
}

/**
 * 处理全量推送按钮点击
 */
const handleFullPush = async () => {
  try {
    await ElMessageBox.confirm(
      t('route.fullPushConfirm'),
      t('message.tips'),
      { type: 'warning' }
    )

    const req: FullPushRoutesReq = {
      storageMode: searchForm.storageMode,
      routesGroup: searchForm.routesGroup || 'default',
      nacosDataId: searchForm.nacosDataId,
      nacosGroup: searchForm.nacosGroup,
    }
    await fullPushRoutes(req)
    ElMessage.success(t('route.fullPushSuccess'))
    loadData()
  } catch {
    // 用户取消
  }
}

/**
 * 处理导入路由按钮点击
 */
const handleImport = () => {
  importRoutesGroup.value = searchForm.routesGroup || 'default'
  importJsonData.value = ''
  importOverwrite.value = false
  importDialogVisible.value = true
}

/**
 * 处理导入路由提交
 */
const handleImportSubmit = async () => {
  if (!importJsonData.value.trim()) {
    ElMessage.warning(t('route.importDataRequired'))
    return
  }

  // 验证JSON格式
  try {
    JSON.parse(importJsonData.value)
  } catch {
    ElMessage.error(t('route.invalidJsonFormat'))
    return
  }

  importLoading.value = true
  try {
    const req: ImportRoutesReq = {
      routesData: importJsonData.value,
      routesGroup: importRoutesGroup.value,
      overwrite: importOverwrite.value,
    }
    const result: ImportRoutesRsp = await importRoutes(req)

    if (result.failedCount > 0) {
      ElMessage.warning(
        t('route.importResultPartial', {
          success: result.successCount,
          failed: result.failedCount,
        })
      )
    } else {
      ElMessage.success(
        t('route.importResultSuccess', { count: result.successCount })
      )
    }
    importDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[RouteManagement] Failed to import routes:', error)
    ElMessage.error(t('message.operationFailed'))
  } finally {
    importLoading.value = false
  }
}

/**
 * 处理导出路由按钮点击
 */
const handleExport = async () => {
  if (selectedRoutes.value.length === 0) {
    ElMessage.warning(t('route.selectRouteToExport'))
    return
  }

  try {
    const req: ExportRoutesReq = {
      routeIds: selectedRoutes.value.map((r) => r.routeId),
      routesGroup: searchForm.routesGroup,
    }
    const jsonData = await exportRoutes(req)

    // 复制到剪贴板
    await navigator.clipboard.writeText(jsonData)
    ElMessage.success(t('route.exportSuccess'))

    // 同时下载文件
    const blob = new Blob([jsonData], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `routes-export-${new Date().toISOString().slice(0, 10)}.json`
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('[RouteManagement] Failed to export routes:', error)
    ElMessage.error(t('message.operationFailed'))
  }
}

/**
 * 处理克隆路由按钮点击
 */
const handleClone = (row: RouteDefinition) => {
  cloneSourceRouteId.value = row.routeId
  cloneNewRouteId.value = `${row.routeId}-clone`
  cloneNewRouteName.value = row.routeName ? `${row.routeName}${t('route.clonedSuffix')}` : ''
  cloneDialogVisible.value = true
}

/**
 * 处理克隆路由提交
 */
const handleCloneSubmit = async () => {
  if (!cloneNewRouteId.value.trim()) {
    ElMessage.warning(t('route.newRouteIdRequired'))
    return
  }

  cloneLoading.value = true
  try {
    const req: CloneRouteReq = {
      sourceRouteId: cloneSourceRouteId.value,
      newRouteId: cloneNewRouteId.value,
      newRouteName: cloneNewRouteName.value,
    }
    await cloneRoute(req)
    ElMessage.success(t('route.cloneSuccess'))
    cloneDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[RouteManagement] Failed to clone route:', error)
    ElMessage.error(t('message.operationFailed'))
  } finally {
    cloneLoading.value = false
  }
}

// 组件挂载时加载初始数据
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
/* 路由管理页面 - 继承全局 table-page-container 样式 */

.storage-mode-option {
  display: flex;
  flex-direction: column;
  small {
    color: #909399;
    font-size: 12px;
  }
}

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

.operation-buttons {
  display: flex;
  gap: 8px;
}

// 新增弹窗样式
.batch-status-tip,
.import-tip,
.clone-tip {
  padding: 12px 16px;
  background: var(--el-color-primary-light-9);
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  border-left: 3px solid var(--el-color-primary);
}

.time-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>

<style lang="scss">
/* 路由弹窗样式（非 scoped） */
.route-dialog,
.history-dialog {
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