<template>
  <!-- 路由管理页面 -->
  <div class="route-management table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <!-- 路由分组 -->
        <el-form-item :label="t('route.routeGroup')">
          <el-input
            v-model.trim="searchForm.routesGroup"
            :placeholder="t('route.routeGroupPlaceholder')"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <!-- 路由名称 -->
        <el-form-item :label="t('route.routeName')">
          <el-input
            v-model.trim="searchForm.routeName"
            :placeholder="t('route.routeNamePlaceholder')"
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
            :has-permission="() => checkPermission(ButtonPerms.Route.Add)"
            type="warning"
            @click="handleFetchInstanceRoutes"
          >
            <el-icon><Download /></el-icon>
            {{ t('route.fetchInstanceRoutes') }}
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
          <el-table-column prop="routesGroup" :label="t('route.routeGroup')" min-width="100" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag type="info" effect="plain" size="small">{{ row.routesGroup || 'default' }}</el-tag>
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
          <el-table-column :label="t('common.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain" size="small">
                {{ row.status === 1 ? t('common.statusEnable') : t('common.statusDisable') }}
              </el-tag>
            </template>
          </el-table-column>
          <!-- 新增：推送状态列 -->
          <el-table-column :label="t('route.pushStatus')" min-width="120" align="center">
            <template #default="{ row }">
              <el-popover
                placement="top"
                :width="320"
                trigger="hover"
                :disabled="!hasInstancePushStatus(row.routeId)"
              >
                <template #reference>
                  <el-tag
                    :type="getPushStatusType(row.pushStatus, row.routeId)"
                    effect="plain"
                    size="small"
                    class="cursor-pointer"
                  >
                    {{ getPushStatusText(row.pushStatus, row.routeId) }}
                  </el-tag>
                </template>
                <div class="instance-push-status-popover">
                  <div class="popover-title">{{ t('route.instancePushDetail') }}</div>
                  <div class="instance-list">
                    <div
                      v-for="detail in getInstancePushDetails(row.routeId)"
                      :key="detail.instanceId"
                      class="instance-item"
                    >
                      <span class="instance-id">{{ detail.instanceId }}</span>
                      <el-tag
                        :type="getInstancePushStatusType(detail.pushStatus)"
                        size="small"
                        effect="plain"
                      >
                        {{ detail.pushStatusDesc }}
                      </el-tag>
                    </div>
                  </div>
                </div>
              </el-popover>
            </template>
          </el-table-column>
          <!-- 新增：最后推送时间列 -->
          <el-table-column :label="t('route.lastPushTime')" width="160">
            <template #default="{ row }">
              <span class="time-text">{{ row.lastPushTime || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="320" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton
                  :has-permission="() => checkPermission(ButtonPerms.Route.Edit)"
                  type="primary"
                  link
                  size="small"
                  @click="handleDetail(row)"
                >
                  <el-icon><View /></el-icon>
                  {{ t('common.detail') }}
                </AuthButton>
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
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
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
            <el-form-item :label="t('common.status')">
              <el-switch
                v-model="formData.status"
                :active-value="1"
                :inactive-value="0"
                :active-text="t('common.statusEnable')"
                :inactive-text="t('common.statusDisable')"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item :label="t('common.remark')">
              <el-input v-model="formData.remark" :placeholder="t('route.remarkPlaceholder')" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 编辑模式切换 -->
        <el-divider>
          <el-radio-group v-model="editMode" size="small">
            <el-radio-button value="form">{{ t('common.show') }}</el-radio-button>
            <el-radio-button value="json">JSON</el-radio-button>
            <el-radio-button value="yaml">YAML</el-radio-button>
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
        <template v-else-if="editMode === 'json'">
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

        <!-- YAML 模式 -->
        <template v-else-if="editMode === 'yaml'">
          <el-form-item label="YAML">
            <el-input
              v-model="routeYaml"
              type="textarea"
              :rows="15"
              placeholder="YAML format route definition"
              class="yaml-editor"
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
      :lock-scroll="false"
      class="history-dialog"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
    >
      <el-table v-loading="historyLoading" :data="historyData" height="400" stripe>
        <el-table-column prop="historyId" :label="t('route.historyId')" width="100" align="center" />
        <el-table-column prop="routeName" :label="t('route.routeName')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="operationType" :label="t('route.operationType')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getOperationTypeTag(row.operationType)" effect="plain" size="small">
              {{ getOperationTypeLabel(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" :label="t('route.operatorName')" min-width="100" show-overflow-tooltip />
        <el-table-column prop="operateTime" :label="t('route.operateTime')" width="160" />
        <el-table-column :label="t('common.operation')" width="140" align="center">
          <template #default="{ row }">
            <div class="history-operation-buttons">
              <el-button
                v-if="row.operationType !== 'A'"
                type="primary"
                link
                size="small"
                @click="handleRollback(row)"
              >
                {{ t('route.rollback') }}
              </el-button>
              <span v-else class="operation-placeholder">-</span>
              <el-button type="info" link size="small" @click="handleViewHistoryDetail(row)">
                {{ t('route.viewDetail') }}
              </el-button>
            </div>
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
      :routes-group="searchForm.routesGroup"
      :route-ids="selectedRouteIds"
      @success="handleSyncSuccess"
    />

    <!-- 新增：批量状态更新弹窗 -->
    <el-dialog
      v-model="batchStatusDialogVisible"
      :title="t('route.batchStatus')"
      width="450px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
    >
      <div class="batch-status-tip">
        {{ t('route.batchStatusTip', { count: selectedRoutes.length }) }}
      </div>
      <el-form label-width="80px">
        <el-form-item :label="t('route.targetStatus')">
          <el-radio-group v-model="batchStatusValue">
            <el-radio :value="1">
              <el-tag type="success" effect="plain" size="small">{{ t('common.statusEnable') }}</el-tag>
            </el-radio>
            <el-radio :value="0">
              <el-tag type="danger" effect="plain" size="small">{{ t('common.statusDisable') }}</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <!-- 操作结果 -->
      <div v-if="batchStatusResult" class="batch-status-result">
        <el-alert
          :type="batchStatusResult.success ? 'success' : 'error'"
          :closable="false"
          show-icon
        >
          <template #title>
            {{ batchStatusResult.success ? t('message.success') : t('message.operationFailed') }}
          </template>
          <template #default>
            <div class="result-detail">
              {{ t('route.batchStatusResult', { count: selectedRoutes.length }) }}
            </div>
          </template>
        </el-alert>
      </div>
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
      width="800px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
    >
      <!-- 步骤指示器 -->
      <el-steps :active="importStep === 'input' ? 0 : 1" simple style="margin-bottom: 20px">
        <el-step :title="t('route.importStepInput')" />
        <el-step :title="t('route.importStepPreview')" />
      </el-steps>

      <!-- 步骤1：输入数据 -->
      <div v-show="importStep === 'input'">
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
      </div>

      <!-- 步骤2：预览数据 -->
      <div v-show="importStep === 'preview'">
        <!-- 冲突警告 -->
        <div v-if="importConflicts.length > 0" class="import-conflicts">
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>
              {{ t('route.importConflictWarning', { count: importConflicts.length }) }}
            </template>
            <template #default>
              <div class="conflict-list">
                <div v-for="(conflict, idx) in importConflicts" :key="idx" class="conflict-item">
                  <el-tag :type="conflict.type === 'exists' ? 'warning' : 'danger'" size="small">
                    {{ conflict.type === 'exists' ? t('route.conflictExists') : t('route.conflictFormat') }}
                  </el-tag>
                  <span class="conflict-route-id">{{ conflict.routeId }}</span>
                  <span class="conflict-message">{{ conflict.message }}</span>
                </div>
              </div>
            </template>
          </el-alert>
        </div>

        <!-- 预览表格 -->
        <div class="import-preview-title">{{ t('route.importPreviewTitle', { count: importPreviewData.length }) }}</div>
        <el-table :data="importPreviewData" max-height="300" stripe border size="small">
          <el-table-column prop="routeId" :label="t('route.routeId')" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="mono-text">{{ row.routeId }}</span>
              <el-tag
                v-if="importConflicts.some(c => c.routeId === row.routeId)"
                type="warning"
                size="small"
                effect="plain"
                class="ml-2"
              >
                {{ t('route.conflict') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="routeName" :label="t('route.routeName')" min-width="120" show-overflow-tooltip />
          <el-table-column prop="uri" :label="t('route.uri')" min-width="150" show-overflow-tooltip />
          <el-table-column prop="orderNum" :label="t('route.order')" width="80" align="center" />
          <el-table-column :label="t('route.predicates')" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.predicates?.length">{{ row.predicates.map((p: any) => p.name).join(', ') }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <div class="import-footer">
          <template v-if="importStep === 'input'">
            <el-button @click="importDialogVisible = false">{{ t('common.cancel') }}</el-button>
            <el-button type="primary" :loading="importPreviewLoading" @click="handleImportPreview">
              {{ t('route.previewImport') }}
            </el-button>
          </template>
          <template v-else>
            <el-button @click="importStep = 'input'">{{ t('common.previous') }}</el-button>
            <el-button @click="handleImportCancel">{{ t('common.cancel') }}</el-button>
            <el-button
              type="primary"
              :loading="importLoading"
              :disabled="importPreviewData.length === 0"
              @click="handleImportSubmit"
            >
              {{ t('route.confirmImport') }}
            </el-button>
          </template>
        </div>
      </template>
    </el-dialog>

    <!-- 新增：克隆路由弹窗 -->
    <el-dialog
      v-model="cloneDialogVisible"
      :title="t('route.cloneRoute')"
      width="500px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
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

    <!-- 路由详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="t('route.routeDetail')"
      width="700px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="route-detail-dialog"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
    >
      <div v-if="currentRouteDetail" class="route-detail-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="t('route.routeId')">
            <span class="mono-text">{{ currentRouteDetail.routeId }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.routeName')">
            {{ currentRouteDetail.routeName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.routeGroup')">
            <el-tag size="small" effect="plain" type="info">{{ currentRouteDetail.routesGroup || 'default' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.uri')">
            <el-tag size="small" effect="plain" type="success">{{ currentRouteDetail.uri }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.order')">
            {{ currentRouteDetail.orderNum || 0 }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('common.status')">
            <el-tag :type="currentRouteDetail.status === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ currentRouteDetail.status === 1 ? t('common.statusEnable') : t('common.statusDisable') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.pushStatus')">
            <el-popover
              placement="top"
              :width="320"
              trigger="hover"
              :disabled="!hasInstancePushStatus(currentRouteDetail.routeId)"
            >
              <template #reference>
                <el-tag
                  :type="getPushStatusType(currentRouteDetail.pushStatus, currentRouteDetail.routeId)"
                  size="small"
                  effect="plain"
                  class="cursor-pointer"
                >
                  {{ getPushStatusText(currentRouteDetail.pushStatus, currentRouteDetail.routeId) }}
                </el-tag>
              </template>
              <div class="instance-push-status-popover">
                <div class="popover-title">{{ t('route.instancePushDetail') }}</div>
                <div class="instance-list">
                  <div
                    v-for="detail in getInstancePushDetails(currentRouteDetail.routeId)"
                    :key="detail.instanceId"
                    class="instance-item"
                  >
                    <span class="instance-id">{{ detail.instanceId }}</span>
                    <el-tag
                      :type="getInstancePushStatusType(detail.pushStatus)"
                      size="small"
                      effect="plain"
                    >
                      {{ detail.pushStatusDesc }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </el-popover>
          </el-descriptions-item>
          <el-descriptions-item :label="t('route.lastPushTime')">
            {{ currentRouteDetail.lastPushTime || '-' }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('common.remark')" :span="2">
            {{ currentRouteDetail.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 断言配置 -->
        <div class="detail-section">
          <div class="section-title">{{ t('route.predicates') }}</div>
          <div v-if="currentRouteDetail.predicates?.length" class="config-list">
            <div v-for="(p, idx) in currentRouteDetail.predicates" :key="idx" class="config-item">
              <el-tag type="primary" effect="light" size="small">{{ p.name }}</el-tag>
              <span class="config-args">{{ formatConfigArgs(p) }}</span>
            </div>
          </div>
          <div v-else class="empty-section">-</div>
        </div>

        <!-- 过滤器配置 -->
        <div class="detail-section">
          <div class="section-title">{{ t('route.filters') }}</div>
          <div v-if="currentRouteDetail.filters?.length" class="config-list">
            <div v-for="(f, idx) in currentRouteDetail.filters" :key="idx" class="config-item">
              <el-tag type="warning" effect="light" size="small">{{ f.name }}</el-tag>
              <span class="config-args">{{ formatConfigArgs(f) }}</span>
            </div>
          </div>
          <div v-else class="empty-section">-</div>
        </div>

        <!-- JSON/YAML 预览 (Spring Cloud Gateway 格式) -->
        <div class="detail-section">
          <div class="section-header">
            <div class="section-title">Spring Cloud Gateway</div>
            <el-radio-group v-model="detailFormat" size="small">
              <el-radio-button value="json">JSON</el-radio-button>
              <el-radio-button value="yaml">YAML</el-radio-button>
            </el-radio-group>
          </div>
          <div class="json-preview">
            <pre>{{ gatewayRoutePreview }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 获取实例路由弹窗 -->
    <el-dialog
      v-model="fetchInstanceDialogVisible"
      :title="t('route.fetchInstanceRoutes')"
      width="900px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="fetch-instance-dialog"
      @open="lockBodyScroll"
      @closed="unlockBodyScroll"
    >
      <div class="fetch-instance-content">
        <!-- 实例选择 -->
        <div class="instance-select-section">
          <el-form label-width="80px">
            <el-form-item :label="t('route.targetInstances')">
              <el-select
                v-model="selectedFetchInstance"
                :placeholder="t('route.selectInstanceToFetch')"
                style="width: 300px"
                :loading="fetchInstanceLoading"
                @change="handleInstanceSelect"
              >
                <el-option
                  v-for="instance in onlineInstances"
                  :key="instance.instanceId"
                  :label="instance.instanceId"
                  :value="instance.instanceId"
                >
                  <div class="instance-option">
                    <span class="instance-id">{{ instance.instanceId }}</span>
                    <el-tag type="success" size="small" effect="plain">{{ instance.uri }}</el-tag>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <!-- 实例路由列表 -->
        <div v-if="instanceRoutes.length > 0" class="instance-routes-section">
          <div class="section-header">
            <span class="section-title">
              {{ t('route.instanceRoutesList', { count: instanceRoutes.length }) }}
            </span>
            <div class="section-actions">
              <el-checkbox v-model="selectAllInstanceRoutes" @change="handleSelectAllInstanceRoutes as any">
                {{ t('common.selectAll') }}
              </el-checkbox>
              <el-button type="primary" size="small" :loading="importingRoutes" @click="handleImportInstanceRoutes">
                {{ t('route.importSelectedRoutes') }} ({{ selectedInstanceRouteIds.length }})
              </el-button>
            </div>
          </div>
          <el-table
            :data="instanceRoutes"
            height="400"
            stripe
            @selection-change="handleInstanceRouteSelection"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column prop="routeId" :label="t('route.routeId')" min-width="140">
              <template #default="{ row }">
                <span class="mono-text">{{ row.routeId }}</span>
                <el-tag v-if="row.existsInRepo" type="warning" size="small" effect="plain" class="ml-2">
                  {{ t('route.alreadyExists') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="uri" :label="t('route.uri')" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tag type="success" effect="plain" size="small">{{ row.uri }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('route.predicates')" min-width="180">
              <template #default="{ row }">
                <div class="predicate-tags">
                  <el-tag
                    v-for="(p, idx) in row.predicates"
                    :key="idx"
                    type="primary"
                    effect="light"
                    size="small"
                  >
                    {{ p.name }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('route.filters')" min-width="140">
              <template #default="{ row }">
                <div class="filter-tags">
                  <el-tag
                    v-for="(f, idx) in row.filters"
                    :key="idx"
                    type="warning"
                    effect="light"
                    size="small"
                  >
                    {{ f.name }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="orderNum" :label="t('route.order')" width="80" align="center" />
          </el-table>
        </div>
        <div v-else-if="selectedFetchInstance && !fetchInstanceRoutesLoading" class="empty-section">
          <el-empty :description="t('route.noInstanceRoutes')" />
        </div>
      </div>
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import yaml from 'js-yaml'
import {
  Search,
  Refresh,
  Plus,
  Edit,
  Delete,
  Clock,
  Promotion,
  View,
  // 新增图标
  Switch,
  UploadFilled,
  Download,
  DocumentCopy,
  Download as FetchInstance,
} from '@element-plus/icons-vue'
import {
  getRouteList,
  saveRoute,
  updateRoute,
  deleteRoute,
  refreshRoutes,
  getRouteHistory,
  rollbackRoute,
  // 新增接口方法
  batchUpdateStatus,
  exportRoutes,
  importRoutes,
  cloneRoute,
  routeApi,
  type RouteDefinition,
  type PredicateConfig,
  type FilterConfig,
  type SaveRouteReq,
  type UpdateRouteReq,
  type DeleteRouteReq,
  type QueryRouteHistoryReq,
  type RollbackRouteReq,
  type RouteHistory,
  type BatchUpdateStatusReq,
  type ImportRoutesReq,
  type ImportRoutesRsp,
  type CloneRouteReq,
  type ExportRoutesReq,
  type GatewayInstanceVO,
  type RouteInstancePushStatusRsp,
} from '@/api/route'
import { ButtonPerms, usePermission } from '@/composables/usePermission'
import SyncInstanceDialog from './components/SyncInstanceDialog.vue'

defineOptions({
  name: 'RouteRepository',
})

const { hasPermission: checkPermission } = usePermission()

const { t } = useI18n()
const route = useRoute()

// 搜索表单
const searchForm = reactive({
  routesGroup: '',
  routeName: '',
})

// 从路由参数获取要定位的路由ID
const highlightRouteId = ref<string | null>(null)

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
// 路由实例推送状态映射 (routeId -> RouteInstancePushStatusRsp)
const routeInstancePushStatusMap = ref<Map<string, RouteInstancePushStatusRsp>>(new Map())

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const editMode = ref<'form' | 'json' | 'yaml'>('form')
const routeJson = ref('')
const routeYaml = ref('')

// 详情弹窗
const detailDialogVisible = ref(false)
const currentRouteDetail = ref<RouteDefinition | null>(null)
const detailFormat = ref<'json' | 'yaml'>('json')

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

/**
 * 将路由数据转换为 Spring Cloud Gateway 路由定义格式
 */
function convertToGatewayRouteDefinition(route: RouteDefinition) {
  const gatewayRoute: {
    id: string
    uri: string
    predicates: Array<{ name: string; args: Record<string, any> }>
    filters: Array<{ name: string; args: Record<string, any> }>
    order: number
    metadata?: Record<string, any>
  } = {
    id: route.routeId,
    uri: route.uri,
    predicates: [],
    filters: [],
    order: route.orderNum || 0,
  }

  // 转换断言配置
  if (route.predicates && Array.isArray(route.predicates)) {
    gatewayRoute.predicates = route.predicates.map(p => ({
      name: p.name,
      args: p.args || {},
    }))
  }

  // 转换过滤器配置
  if (route.filters && Array.isArray(route.filters)) {
    gatewayRoute.filters = route.filters.map(f => ({
      name: f.name,
      args: f.args || {},
    }))
  }

  // 只在有元数据时添加
  if (route.metadata && Object.keys(route.metadata).length > 0) {
    gatewayRoute.metadata = route.metadata
  }

  return gatewayRoute
}

// Gateway 路由预览（JSON/YAML）
const gatewayRoutePreview = computed(() => {
  if (!currentRouteDetail.value) return ''
  const gatewayRoute = convertToGatewayRouteDefinition(currentRouteDetail.value)
  if (detailFormat.value === 'yaml') {
    try {
      return yaml.dump(gatewayRoute, { indent: 2, lineWidth: -1 })
    } catch {
      return JSON.stringify(gatewayRoute, null, 2)
    }
  }
  return JSON.stringify(gatewayRoute, null, 2)
})

// 监听编辑模式切换
watch(editMode, (mode, oldMode) => {
  if (mode === 'json') {
    // 切换到 JSON 模式，序列化表单数据
    routeJson.value = JSON.stringify(formData, null, 2)
  } else if (mode === 'yaml') {
    // 切换到 YAML 模式，序列化表单数据
    try {
      routeYaml.value = yaml.dump(formData, { indent: 2, lineWidth: -1 })
    } catch {
      routeYaml.value = JSON.stringify(formData, null, 2)
    }
  } else if (mode === 'form') {
    // 切换回表单模式，解析当前编辑器的数据
    try {
      if (oldMode === 'yaml') {
        const parsed = yaml.load(routeYaml.value) as object
        Object.assign(formData, parsed)
      } else {
        const parsed = JSON.parse(routeJson.value)
        Object.assign(formData, parsed)
      }
    } catch {
      // 解析错误，忽略
    }
  }
})

/**
 * 加载路由列表数据
 */
const loadData = async () => {
  loading.value = true
  try {
    // 路由仓库始终从数据库查询
    const res = await getRouteList({
      routesGroup: searchForm.routesGroup,
      routeName: searchForm.routeName,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    })
    tableData.value = res.rows || []
    pagination.total = res.total || 0

    // 加载路由实例推送状态
    if (tableData.value.length > 0) {
      await loadRouteInstancePushStatus()
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
 * 加载路由实例推送状态
 */
const loadRouteInstancePushStatus = async () => {
  if (tableData.value.length === 0) return

  try {
    const routeIds = tableData.value.map(r => r.routeId)
    const statusList = await routeApi.getRouteInstancePushStatus({ routeIds })

    // 构建映射
    routeInstancePushStatusMap.value.clear()
    if (Array.isArray(statusList)) {
      statusList.forEach(status => {
        routeInstancePushStatusMap.value.set(status.routeId, status)
      })
    }
  } catch (error) {
    console.error('[RouteManagement] Failed to load route instance push status:', error)
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
  searchForm.routeName = ''
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
 * 处理查看路由详情
 */
const handleDetail = (row: RouteDefinition) => {
  currentRouteDetail.value = row
  detailFormat.value = 'json'
  detailDialogVisible.value = true
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

    // 根据编辑模式解析数据
    let submitData: SaveRouteReq | UpdateRouteReq = JSON.parse(JSON.stringify(formData))
    if (editMode.value === 'json') {
      try {
        submitData = JSON.parse(routeJson.value)
      } catch {
        ElMessage.error(t('route.invalidJsonFormat'))
        submitting.value = false
        return
      }
    } else if (editMode.value === 'yaml') {
      try {
        submitData = yaml.load(routeYaml.value) as SaveRouteReq | UpdateRouteReq
      } catch {
        ElMessage.error(t('route.invalidYamlFormat'))
        submitting.value = false
        return
      }
    }

    // 处理自定义类型的数据转换
    submitData = convertCustomTypes(submitData)

    // 路由仓库始终保存到数据库
    if (isEdit.value) {
      // 更新路由
      await updateRoute(submitData as UpdateRouteReq)
    } else {
      // 新增路由
      await saveRoute(submitData as SaveRouteReq)
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

    // 路由仓库始终从数据库删除
    const req: DeleteRouteReq = {
      routesGroup: searchForm.routesGroup || 'default',
      routeIds: [row.routeId],
    }
    await deleteRoute(req)

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
const batchStatusResult = ref<{ success: boolean; message?: string } | null>(null)

// 导入路由弹窗状态
const importDialogVisible = ref(false)
const importRoutesGroup = ref('default')
const importJsonData = ref('')
const importOverwrite = ref(false)
const importLoading = ref(false)

// 导入预览相关
const importStep = ref<'input' | 'preview'>('input')
const importPreviewData = ref<RouteDefinition[]>([])
const importConflicts = ref<Array<{ routeId: string; type: 'exists' | 'format_error'; message: string }>>([])
const importPreviewLoading = ref(false)

// 克隆路由弹窗状态
const cloneDialogVisible = ref(false)
const cloneSourceRouteId = ref('')
const cloneNewRouteId = ref('')
const cloneNewRouteName = ref('')
const cloneLoading = ref(false)

// 获取实例路由弹窗状态
const fetchInstanceDialogVisible = ref(false)
const onlineInstances = ref<GatewayInstanceVO[]>([])
const selectedFetchInstance = ref('')
const fetchInstanceLoading = ref(false)
const fetchInstanceRoutesLoading = ref(false)
const instanceRoutes = ref<(RouteDefinition & { existsInRepo?: boolean })[]>([])
const selectedInstanceRouteIds = ref<string[]>([])
const selectAllInstanceRoutes = ref(false)
const importingRoutes = ref(false)

/**
 * 获取推送状态类型（用于Tag颜色）
 */
const getPushStatusType = (pushStatus: number | undefined, routeId?: string): 'info' | 'success' | 'danger' | 'warning' => {
  // 如果有实例级别状态，根据实例推送比例判断
  if (routeId) {
    const instanceStatus = routeInstancePushStatusMap.value.get(routeId)
    if (instanceStatus) {
      // 部分成功显示 warning
      if (instanceStatus.pushedInstances > 0 && instanceStatus.pushedInstances < instanceStatus.totalInstances) {
        return 'warning'
      }
      // 全部成功
      if (instanceStatus.pushedInstances === instanceStatus.totalInstances) {
        return 'success'
      }
      // 全部失败
      if (instanceStatus.failedInstances === instanceStatus.totalInstances) {
        return 'danger'
      }
      // 部分失败
      if (instanceStatus.failedInstances > 0) {
        return 'warning'
      }
    }
  }

  // 兼容旧的推送状态
  if (pushStatus === undefined || pushStatus === 0) return 'info'
  if (pushStatus === 1) return 'success'
  if (pushStatus === 2) return 'danger'
  return 'warning'
}

/**
 * 获取推送状态文本
 */
const getPushStatusText = (pushStatus: number | undefined, routeId?: string): string => {
  // 如果有实例级别状态，显示详细格式
  if (routeId) {
    const instanceStatus = routeInstancePushStatusMap.value.get(routeId)
    if (instanceStatus) {
      // 显示格式: "已推送(3/5)" 或 "未推送(0/5)" 或 "部分失败(3/5)"
      if (instanceStatus.pushedInstances === instanceStatus.totalInstances) {
        return t('route.pushStatusPushedFormat', { count: instanceStatus.pushedInstances, total: instanceStatus.totalInstances })
      }
      if (instanceStatus.pushedInstances === 0 && instanceStatus.failedInstances === 0) {
        return t('route.pushStatusNotPushedFormat', { total: instanceStatus.totalInstances })
      }
      if (instanceStatus.failedInstances > 0) {
        return t('route.pushStatusPartialFormat', { success: instanceStatus.pushedInstances, failed: instanceStatus.failedInstances, total: instanceStatus.totalInstances })
      }
      return t('route.pushStatusPushedFormat', { count: instanceStatus.pushedInstances, total: instanceStatus.totalInstances })
    }
  }

  // 兼容旧的推送状态
  if (pushStatus === undefined || pushStatus === 0) return t('route.pushStatusNotPushed')
  if (pushStatus === 1) return t('route.pushStatusPushed')
  if (pushStatus === 2) return t('route.pushStatusFailed')
  return t('route.pushStatusUnknown')
}

/**
 * 判断是否有实例推送状态详情
 */
const hasInstancePushStatus = (routeId: string): boolean => {
  const status = routeInstancePushStatusMap.value.get(routeId)
  return status !== undefined && status.instanceDetails !== undefined && status.instanceDetails.length > 0
}

/**
 * 获取实例推送状态详情
 */
const getInstancePushDetails = (routeId: string) => {
  const status = routeInstancePushStatusMap.value.get(routeId)
  return status?.instanceDetails || []
}

/**
 * 获取实例推送状态类型
 */
const getInstancePushStatusType = (pushStatus: number): 'info' | 'success' | 'danger' | 'warning' => {
  if (pushStatus === 1) return 'success'
  if (pushStatus === 2) return 'danger'
  if (pushStatus === 0) return 'info'
  return 'warning'
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
  batchStatusResult.value = null
  batchStatusDialogVisible.value = true
}

/**
 * 处理批量状态更新提交
 */
const handleBatchStatusSubmit = async () => {
  batchStatusLoading.value = true
  batchStatusResult.value = null
  try {
    const req: BatchUpdateStatusReq = {
      routeIds: selectedRoutes.value.map((r) => r.routeId),
      status: batchStatusValue.value,
      routesGroup: searchForm.routesGroup || 'default',
    }
    await batchUpdateStatus(req)
    batchStatusResult.value = { success: true }
    ElMessage.success(t('message.success'))
    batchStatusDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[RouteManagement] Failed to batch update status:', error)
    batchStatusResult.value = { success: false, message: String(error) }
    ElMessage.error(t('message.operationFailed'))
  } finally {
    batchStatusLoading.value = false
  }
}

/**
 * 处理导入路由按钮点击
 */
const handleImport = () => {
  importRoutesGroup.value = searchForm.routesGroup || 'default'
  importJsonData.value = ''
  importOverwrite.value = false
  importStep.value = 'input'
  importPreviewData.value = []
  importConflicts.value = []
  importDialogVisible.value = true
}

/**
 * 处理导入预览
 */
const handleImportPreview = async () => {
  if (!importJsonData.value.trim()) {
    ElMessage.warning(t('route.importDataRequired'))
    return
  }

  // 验证JSON格式
  let parsedData: RouteDefinition[]
  try {
    parsedData = JSON.parse(importJsonData.value)
    if (!Array.isArray(parsedData)) {
      ElMessage.error(t('route.invalidJsonFormat'))
      return
    }
  } catch {
    ElMessage.error(t('route.invalidJsonFormat'))
    return
  }

  importPreviewLoading.value = true
  try {
    // 解析预览数据
    importPreviewData.value = parsedData
    importConflicts.value = []

    // 检测与现有路由的冲突
    const existingRouteIds = new Set(tableData.value.map((r: RouteDefinition) => r.routeId))
    for (const route of parsedData) {
      if (!route.routeId) {
        importConflicts.value.push({
          routeId: 'unknown',
          type: 'format_error',
          message: t('route.missingRouteId')
        })
        continue
      }
      if (existingRouteIds.has(route.routeId) && !importOverwrite.value) {
        importConflicts.value.push({
          routeId: route.routeId,
          type: 'exists',
          message: t('route.conflictExistsMessage')
        })
      }
    }

    // 切换到预览步骤
    importStep.value = 'preview'
  } catch (error) {
    console.error('[RouteManagement] Failed to preview import:', error)
    ElMessage.error(t('message.operationFailed'))
  } finally {
    importPreviewLoading.value = false
  }
}

/**
 * 处理导入取消
 */
const handleImportCancel = () => {
  importStep.value = 'input'
  importPreviewData.value = []
  importConflicts.value = []
  importDialogVisible.value = false
}

/**
 * 处理导入路由提交
 */
const handleImportSubmit = async () => {
  if (importPreviewData.value.length === 0) {
    ElMessage.warning(t('route.noDataToImport'))
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
    // 重置状态
    importStep.value = 'input'
    importPreviewData.value = []
    importConflicts.value = []
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

// ========== 新增功能：获取实例路由 ==========

/**
 * 打开获取实例路由弹窗
 */
const handleFetchInstanceRoutes = async () => {
  fetchInstanceDialogVisible.value = true
  fetchInstanceLoading.value = true
  selectedFetchInstance.value = ''
  instanceRoutes.value = []
  selectedInstanceRouteIds.value = []
  selectAllInstanceRoutes.value = false

  try {
    const result = await routeApi.getOnlineGatewayInstances()
    onlineInstances.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('[RouteManagement] Failed to load instances:', error)
    onlineInstances.value = []
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    fetchInstanceLoading.value = false
  }
}

/**
 * 选择实例后获取路由
 */
const handleInstanceSelect = async (instanceId: string) => {
  if (!instanceId) {
    instanceRoutes.value = []
    return
  }

  fetchInstanceRoutesLoading.value = true
  instanceRoutes.value = []
  selectedInstanceRouteIds.value = []
  selectAllInstanceRoutes.value = false

  try {
    const result = await routeApi.getInstanceRoutes({ instanceId })
    // getInstanceRoutes 返回 RouteDefinition[] 或 { rows: RouteDefinition[] }
    let routes: RouteDefinition[]
    if (Array.isArray(result)) {
      routes = result
    } else if (result && Array.isArray((result as any).rows)) {
      routes = (result as any).rows
    } else {
      routes = []
    }

    // 检查哪些路由已存在于仓库
    const existingRouteIds = new Set(tableData.value.map(r => r.routeId))
    instanceRoutes.value = routes.map(route => ({
      ...route,
      existsInRepo: existingRouteIds.has(route.routeId),
    }))
  } catch (error) {
    console.error('[RouteManagement] Failed to load instance routes:', error)
    instanceRoutes.value = []
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    fetchInstanceRoutesLoading.value = false
  }
}

/**
 * 处理实例路由选择
 */
const handleInstanceRouteSelection = (selection: (RouteDefinition & { existsInRepo?: boolean })[]) => {
  selectedInstanceRouteIds.value = selection.map(r => r.routeId)
  selectAllInstanceRoutes.value = selection.length === instanceRoutes.value.length
}

/**
 * 全选/取消全选实例路由
 */
const handleSelectAllInstanceRoutes = (val: boolean) => {
  if (val) {
    // 只选择不存在于仓库的路由
    selectedInstanceRouteIds.value = instanceRoutes.value
      .filter(r => !r.existsInRepo)
      .map(r => r.routeId)
  } else {
    selectedInstanceRouteIds.value = []
  }
}

/**
 * 导入选中的实例路由到仓库
 */
const handleImportInstanceRoutes = async () => {
  if (selectedInstanceRouteIds.value.length === 0) {
    ElMessage.warning(t('route.selectRouteToImport'))
    return
  }

  // 过滤掉已存在的路由
  const routesToImport = instanceRoutes.value.filter(
    r => selectedInstanceRouteIds.value.includes(r.routeId) && !r.existsInRepo
  )

  if (routesToImport.length === 0) {
    ElMessage.warning(t('route.allRoutesExist'))
    return
  }

  importingRoutes.value = true
  try {
    // 批量保存路由
    const routesGroup = searchForm.routesGroup || 'default'
    let successCount = 0
    let failCount = 0

    for (const route of routesToImport) {
      try {
        const saveReq: SaveRouteReq = {
          routeId: route.routeId,
          routeName: route.routeName || route.routeId,
          uri: route.uri,
          predicates: route.predicates || [],
          filters: route.filters || [],
          orderNum: route.orderNum || 0,
          routesGroup,
          status: 1,
        }
        await saveRoute(saveReq)
        successCount++
      } catch {
        failCount++
      }
    }

    if (failCount > 0) {
      ElMessage.warning(t('route.importResultPartial', { success: successCount, failed: failCount }))
    } else {
      ElMessage.success(t('route.importResultSuccess', { count: successCount }))
    }

    fetchInstanceDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('[RouteManagement] Failed to import instance routes:', error)
    ElMessage.error(t('message.operationFailed'))
  } finally {
    importingRoutes.value = false
  }
}

// ============================================
// 弹窗防抖动 - 手动锁定滚动条
// ============================================

const lockBodyScroll = () => {
  document.body.classList.add('dialog-open')
}

const unlockBodyScroll = () => {
  document.body.classList.remove('dialog-open')
}

// 组件挂载时加载初始数据
onMounted(() => {
  // 处理从推送历史跳转过来的路由参数
  const routeIdFromQuery = route.query.routeId as string | undefined
  if (routeIdFromQuery) {
    highlightRouteId.value = routeIdFromQuery
  }
  loadData()
})

// 监听数据加载完成后，高亮定位到指定路由
watch(tableData, () => {
  if (highlightRouteId.value && tableData.value.length > 0) {
    const targetRoute = tableData.value.find((r: RouteDefinition) => r.routeId === highlightRouteId.value)
    if (targetRoute) {
      // 打开路由详情
      handleDetail(targetRoute)
      // 清除高亮标记
      highlightRouteId.value = null
    }
  }
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
  flex-wrap: wrap;
  gap: 4px 8px;

  :deep(.el-button) {
    margin: 0;
  }
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

// 导入预览相关样式
.import-conflicts {
  margin-bottom: 16px;

  .conflict-list {
    margin-top: 8px;

    .conflict-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 0;
      font-size: 13px;

      .conflict-route-id {
        font-family: 'Monaco', 'Menlo', monospace;
        color: var(--el-text-color-primary);
      }

      .conflict-message {
        color: var(--el-text-color-secondary);
      }
    }
  }
}

.import-preview-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
}

.import-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.time-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

// 实例推送状态 Popover 样式
.instance-push-status-popover {
  .popover-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .instance-list {
    max-height: 300px;
    overflow-y: auto;
  }

  .instance-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    border-bottom: 1px dashed var(--el-border-color-lighter);

    &:last-child {
      border-bottom: none;
    }

    .instance-id {
      font-size: 12px;
      font-family: 'Monaco', 'Menlo', monospace;
      color: var(--el-text-color-regular);
    }
  }
}

.cursor-pointer {
  cursor: pointer;
}

// 获取实例路由弹窗样式
.fetch-instance-dialog {
  .fetch-instance-content {
    .instance-select-section {
      margin-bottom: 16px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--el-border-color-lighter);
    }

    .instance-option {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;

      .instance-id {
        font-family: 'Monaco', 'Menlo', monospace;
      }
    }

    .instance-routes-section {
      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .section-title {
          font-size: 14px;
          font-weight: 500;
        }

        .section-actions {
          display: flex;
          align-items: center;
          gap: 16px;
        }
      }

      .ml-2 {
        margin-left: 8px;
      }
    }
  }
}

// 历史弹窗操作按钮样式
.history-operation-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;

  .operation-placeholder {
    width: 40px;
    display: inline-block;
    text-align: center;
    color: var(--el-text-color-placeholder);
  }
}

// 路由详情弹窗样式
.route-detail-dialog {
  .route-detail-content {
    .mono-text {
      font-family: 'Monaco', 'Menlo', monospace;
    }

    .detail-section {
      margin-top: 16px;

      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
      }

      .section-title {
        font-size: 13px;
        font-weight: 600;
        color: var(--el-text-color-primary);
        padding-left: 8px;
        border-left: 3px solid var(--el-color-primary);
      }

      .empty-section {
        color: var(--el-text-color-secondary);
        font-size: 12px;
        padding: 8px 12px;
        background: var(--el-fill-color-lighter);
        border-radius: 4px;
      }

      .config-list {
        display: flex;
        flex-direction: column;
        gap: 8px;

        .config-item {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 8px 12px;
          background: var(--el-fill-color-light);
          border-radius: 6px;

          .config-args {
            font-size: 12px;
            color: var(--el-text-color-secondary);
            font-family: 'Monaco', 'Menlo', monospace;
          }
        }
      }

      .json-preview {
        background: var(--el-fill-color-light);
        border-radius: 8px;
        padding: 12px;
        overflow: auto;
        max-height: 250px;

        pre {
          margin: 0;
          font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
          font-size: 12px;
          line-height: 1.6;
          white-space: pre-wrap;
          word-break: break-all;
        }
      }
    }
  }
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