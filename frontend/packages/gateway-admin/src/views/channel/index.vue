<template>
  <!-- 渠道管理页面 -->
  <div class="channel-management table-page-container">
    <!-- 搜索卡片 -->
    <el-card class="search-card shrink-0" shadow="never">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item :label="t('channel.channelName')">
          <el-input v-model.trim="searchForm.channelName" :placeholder="t('common.pleaseInput') + t('channel.channelName')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="searchForm.enable" :placeholder="t('common.pleaseSelect')" clearable style="width: 100px">
            <el-option :label="t('common.enabled')" :value="0" />
            <el-option :label="t('common.disabled')" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleSearch">
            <el-icon><Search /></el-icon>{{ t('common.search') }}
          </el-button>
          <el-button style="height: 28px; padding: 0 12px; font-size: 13px" @click="handleReset">
            <el-icon><Refresh /></el-icon>{{ t('common.reset') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格卡片 -->
    <el-card class="table-card flex-1 flex flex-col overflow-hidden" shadow="never">
      <template #header>
        <div class="table-header">
          <div class="header-left">
            <AuthButton :perm="ButtonPerms.Channel.Add" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>{{ t('common.add') }}
            </AuthButton>
            <AuthButton :perm="ButtonPerms.Channel.Edit" type="success" :disabled="syncDisabled" @click="handleSyncSelected">
              <el-icon><Refresh /></el-icon>{{ t('channel.sync') }}
            </AuthButton>
            <span v-if="selectedChannelIds.length > 0" class="selected-info">
              {{ t('channel.selectedCount', { count: selectedChannelIds.length }) }}
            </span>
          </div>
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
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column prop="channelId" :label="t('channel.channelId')" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="channel-id">{{ row.channelId || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="channelName" :label="t('channel.channelName')" min-width="140">
            <template #default="{ row }">
              {{ row.channelName || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="appKey" :label="t('channel.appKey')" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="app-key">{{ row.appKey || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('channel.encryptionSwitch')" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="row.encryptionSwitch"
                :active-value="0"
                :inactive-value="1"
                :before-change="() => handleEncryptionBeforeChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('common.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.enable === 0" type="success">{{ t('common.enabled') }}</el-tag>
              <el-tag v-else type="danger">{{ t('common.disabled') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('common.createTime')" min-width="160">
            <template #default="{ row }">
              {{ row.createTime || '-' }}
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" min-width="320" fixed="right">
            <template #default="{ row }">
              <div class="operation-buttons">
                <AuthButton :perm="ButtonPerms.Channel.Edit" type="primary" link size="small" @click="handleDetail(row)">
                  <el-icon><View /></el-icon>{{ t('common.detail') }}
                </AuthButton>
                <AuthButton :perm="ButtonPerms.Channel.Edit" type="primary" link size="small" @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon>{{ t('common.edit') }}
                </AuthButton>
                <AuthButton :perm="ButtonPerms.Channel.Delete" type="danger" link size="small" @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>{{ t('common.delete') }}
                </AuthButton>
                <el-dropdown @command="(cmd: string) => handleCommand(cmd, row)">
                  <el-button type="info" link size="small">
                    <el-icon><MoreFilled /></el-icon>{{ t('common.more') }}
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="refreshChannelKey">
                        <el-icon><Refresh /></el-icon>{{ t('channel.refreshChannelKey') }}
                      </el-dropdown-item>
                      <el-dropdown-item command="refreshSystemKey">
                        <el-icon><Refresh /></el-icon>{{ t('channel.refreshSystemKey') }}
                      </el-dropdown-item>
                      <el-dropdown-item command="issueToken">
                        <el-icon><Key /></el-icon>{{ t('channel.issueToken') }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :total="total"
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
      width="560px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" class="dialog-form">
        <el-form-item :label="t('channel.channelName')" prop="channelName">
          <el-input v-model.trim="formData.channelName" :placeholder="t('common.pleaseInput') + t('channel.channelName')" />
        </el-form-item>
        <el-form-item :label="t('channel.relatedUser')" prop="relaUserId">
          <el-input
            v-model="selectedUserDisplayName"
            readonly
            :placeholder="t('channel.selectRelatedUser')"
            @click="openUserSelector"
          >
            <template #suffix>
              <el-icon class="cursor-pointer" @click="openUserSelector">
                <User />
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-switch v-model="formData.enable" :active-value="0" :inactive-value="1" />
        </el-form-item>
        <el-form-item :label="t('channel.encryptionSwitch')">
          <el-switch v-model="formData.encryptionSwitch" :active-value="0" :inactive-value="1" />
        </el-form-item>
        <el-form-item :label="t('channel.authoritySwitch')">
          <el-switch v-model="formData.authoritySwitch" :active-value="0" :inactive-value="1" />
        </el-form-item>
        <el-form-item :label="t('channel.tokenType')">
          <el-radio-group v-model="formData.tokenType">
            <el-radio :value="0">{{ t('channel.tokenTypeStateful') }}</el-radio>
            <el-radio :value="1">{{ t('channel.tokenTypeJwt') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input
            v-model.trim="formData.remark"
            type="textarea"
            :rows="3"
            :placeholder="t('common.pleaseInput') + t('common.remark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- User Selector Dialog -->
    <el-dialog
      v-model="userSelectorVisible"
      :title="t('channel.userSelector')"
      width="700px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <div class="user-selector-content">
        <!-- 搜索框 -->
        <el-input
          v-model="userSearchKeyword"
          :placeholder="t('channel.userSelectorPlaceholder')"
          clearable
          style="margin-bottom: 16px"
          @keyup.enter="searchUsers"
        >
          <template #append>
            <el-button @click="searchUsers">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>

        <!-- 用户列表 -->
        <el-table
          v-loading="userSelectorLoading"
          :data="userList"
          height="300"
          stripe
          highlight-current-row
          @current-change="handleUserSelect"
        >
          <el-table-column prop="userId" :label="'ID'" width="80" />
          <el-table-column prop="loginName" :label="t('channel.loginName')" min-width="120" />
          <el-table-column prop="username" :label="t('channel.username')" min-width="120" />
          <el-table-column :label="t('common.operation')" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewUserPermission(row)">
                <el-icon><View /></el-icon>{{ t('channel.viewPermission') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <el-pagination
          v-model:current-page="userSearchForm.pageNum"
          v-model:page-size="userSearchForm.pageSize"
          :total="userTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          style="margin-top: 16px; justify-content: flex-end"
          @size-change="searchUsers"
          @current-change="searchUsers"
        />
      </div>
      <template #footer>
        <el-button @click="userSelectorVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :disabled="!selectedUser" @click="confirmUserSelect">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- User Permission Detail Dialog -->
    <el-dialog
      v-model="permissionDetailVisible"
      :title="t('channel.userPermissionDetail')"
      width="800px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-tabs v-model="permissionActiveTab">
        <!-- 角色列表 -->
        <el-tab-pane :label="t('channel.roleList')" name="roles">
          <el-table v-loading="permissionLoading" :data="permissionDetail.roles" height="300" stripe>
            <el-table-column prop="roleId" label="ID" width="80" />
            <el-table-column prop="roleName" :label="t('system.role.roleName')" min-width="140" />
            <el-table-column prop="roleEnName" :label="t('system.role.roleEnName')" min-width="140" />
            <el-table-column :label="t('common.status')" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="success">{{ t('common.enabled') }}</el-tag>
                <el-tag v-else type="danger">{{ t('common.disabled') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" :label="t('common.remark')" min-width="140" show-overflow-tooltip />
          </el-table>
          <el-empty v-if="!permissionLoading && permissionDetail.roles?.length === 0" :description="t('channel.noPermissionData')" />
        </el-tab-pane>

        <!-- 接口权限 -->
        <el-tab-pane :label="t('channel.apiPermission')" name="permissions">
          <el-table v-loading="permissionLoading" :data="permissionDetail.permissions" height="300" stripe>
            <el-table-column prop="acId" label="ID" width="80" />
            <el-table-column prop="acName" :label="t('system.permission.acName')" min-width="140" />
            <el-table-column prop="acEnName" :label="t('system.permission.acEnName')" min-width="140" />
            <el-table-column prop="acUrl" :label="t('system.permission.url')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="t('common.status')" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="success">{{ t('common.enabled') }}</el-tag>
                <el-tag v-else type="danger">{{ t('common.disabled') }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!permissionLoading && permissionDetail.permissions?.length === 0" :description="t('channel.noPermissionData')" />
        </el-tab-pane>

        <!-- 数据过滤权限 -->
        <el-tab-pane :label="t('channel.dataFilterPermission')" name="dataFilters">
          <el-table v-loading="permissionLoading" :data="permissionDetail.dataFilters" height="300" stripe>
            <el-table-column prop="dataFilterId" label="ID" width="80" />
            <el-table-column prop="dataFilterName" :label="t('dataScope.filterName')" min-width="140" />
            <el-table-column prop="dataFilterEnName" :label="t('dataScope.filterEnName')" min-width="140" />
            <el-table-column prop="entityClass" :label="t('dataScope.entityClass')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="tableName" :label="t('dataScope.tableName')" min-width="140" />
            <el-table-column prop="ruleType" :label="t('dataScope.ruleType')" min-width="120" />
            <el-table-column :label="t('common.status')" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="success">{{ t('common.enabled') }}</el-tag>
                <el-tag v-else type="danger">{{ t('common.disabled') }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!permissionLoading && permissionDetail.dataFilters?.length === 0" :description="t('channel.noPermissionData')" />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="permissionDetailVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- Issue Token Result Dialog -->
    <el-dialog
      v-model="tokenDialogVisible"
      :title="t('channel.issueToken')"
      width="480px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-form label-width="100px" class="dialog-form">
        <el-form-item :label="t('channel.token')">
          <el-input v-model="tokenResult.token" readonly class="token-input">
            <template #append>
              <el-button @click="copyToken">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('channel.expireTime')">
          <el-input v-model="tokenResult.expireTime" readonly />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tokenDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- Channel Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="t('channel.channelInfo')"
      width="680px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <el-descriptions :column="2" border class="channel-detail">
        <el-descriptions-item :label="t('channel.channelId')">
          <span class="mono-text">{{ detailData.channelId || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.channelName')">
          {{ detailData.channelName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.appKey')">
          <span class="mono-text">{{ detailData.appKey || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.appSecret')">
          <el-button type="primary" link size="small" @click="showSecret('appSecret')">
            <el-icon><View /></el-icon>{{ t('common.show') }}
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.systemPublickey')">
          <el-button type="primary" link size="small" @click="showSecret('systemPublickey')">
            <el-icon><View /></el-icon>{{ t('common.show') }}
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.systemPrivatekey')">
          <el-button type="primary" link size="small" @click="showSecret('systemPrivatekey')">
            <el-icon><View /></el-icon>{{ t('common.show') }}
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.channelPublickey')">
          <el-button type="primary" link size="small" @click="showSecret('channelPublickey')">
            <el-icon><View /></el-icon>{{ t('common.show') }}
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.channelPrivatekey')">
          <el-button type="primary" link size="small" @click="showSecret('channelPrivatekey')">
            <el-icon><View /></el-icon>{{ t('common.show') }}
          </el-button>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.status')">
          <el-tag v-if="detailData.enable === 0" type="success">{{ t('common.enabled') }}</el-tag>
          <el-tag v-else type="danger">{{ t('common.disabled') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.encryptionSwitch')">
          <el-tag v-if="detailData.encryptionSwitch === 0" type="success">{{ t('common.enabled') }}</el-tag>
          <el-tag v-else type="danger">{{ t('common.disabled') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.tokenType')">
          {{ detailData.tokenType === 0 ? t('channel.tokenTypeStateful') : t('channel.tokenTypeJwt') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('channel.authoritySwitch')">
          <el-tag v-if="detailData.authoritySwitch === 0" type="success">{{ t('common.enabled') }}</el-tag>
          <el-tag v-else type="danger">{{ t('common.disabled') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.createTime')">
          {{ detailData.createTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.updateTime')">
          {{ detailData.updateTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.remark')" :span="2">
          {{ detailData.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>

    <!-- Secret View Dialog -->
    <el-dialog
      v-model="secretDialogVisible"
      :title="secretTitle"
      width="600px"
      :close-on-click-modal="false"
      :lock-scroll="false"
    >
      <div class="secret-content">
        <el-input
          v-model="secretContent"
          type="textarea"
          :rows="8"
          readonly
          class="secret-textarea"
        />
        <div class="secret-actions">
          <el-button type="primary" @click="copySecret">
            <el-icon><DocumentCopy /></el-icon>{{ t('common.copy') }}
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="secretDialogVisible = false">{{ t('common.close') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 渠道管理页面
 * 管理网关渠道，包括创建、编辑、密钥刷新和令牌签发
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, MoreFilled, DocumentCopy, Key, User, View } from '@element-plus/icons-vue'
import {
  getChannelList,
  getChannel,
  getChannelSecret,
  saveChannel,
  updateChannel,
  deleteChannel,
  refreshChannelKey,
  refreshSystemKey,
  issueChannelToken,
  type QueryChannelParams,
  type AddChannelParams,
  type UpdateChannelParams,
  type ChannelInfo,
  type IssueTokenResult,
  type GetChannelSecretParams
} from '@/api/channel'
import {
  syncChannelData,
  type SyncChannelDataParams
} from '@/api/dataSync'
import {
  getSimpleUserList,
  getUserPermissionDetail,
  type QuerySimpleUserParams,
  type SimpleUserInfo,
  type UserPermissionDetail
} from '@/api/channelUser'
import { ButtonPerms } from '@/composables/usePermission'

defineOptions({
  name: 'ChannelManagement'
})

const { t } = useI18n()

// 搜索表单
const searchForm = reactive<QueryChannelParams>({
  pageNum: 1,
  pageSize: 10,
  channelName: '',
  enable: undefined
})

// 表格数据
const loading = ref(false)
const tableData = ref<ChannelInfo[]>([])
const total = ref(0)

// 选择状态
const selectedChannelIds = ref<string[]>([])
const syncDisabled = computed(() => selectedChannelIds.value.length === 0)

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const formData = reactive<AddChannelParams & { channelId?: string }>({
  channelId: '',
  channelName: '',
  relaUserId: '',
  enable: 0,
  encryptionSwitch: 0,
  authoritySwitch: 0,
  tokenType: 0,
  remark: ''
})

const formRules = {
  channelName: [{ required: true, message: () => t('channel.channelName') + t('common.required'), trigger: 'blur' }]
}

const dialogTitle = computed(() =>
  isEdit.value ? t('common.edit') + t('channel.channelInfo') : t('common.add') + t('channel.channelInfo')
)

// 令牌弹窗
const tokenDialogVisible = ref(false)
const tokenResult = reactive<IssueTokenResult>({
  token: '',
  expireTime: '',
  expiresIn: 0
})

// 用户选择器
const userSelectorVisible = ref(false)
const userSelectorLoading = ref(false)
const userSearchKeyword = ref('')
const userSearchForm = reactive<QuerySimpleUserParams>({
  pageNum: 1,
  pageSize: 10
})
const userList = ref<SimpleUserInfo[]>([])
const userTotal = ref(0)
const selectedUser = ref<SimpleUserInfo | null>(null)
const selectedUserDisplayName = computed(() => {
  if (selectedUser.value) {
    return `${selectedUser.value.username} (${selectedUser.value.loginName})`
  }
  return ''
})

// 用户权限详情
const permissionDetailVisible = ref(false)
const permissionLoading = ref(false)
const permissionActiveTab = ref('roles')
const permissionDetail = reactive<UserPermissionDetail>({
  roles: [],
  permissions: [],
  dataFilters: []
})

// 渠道详情弹窗
const detailDialogVisible = ref(false)
const detailData = reactive<ChannelInfo>({
  channelId: '',
  channelName: '',
  appKey: '',
  appSecret: '',
  relaUserId: '',
  accessToken: '',
  systemPublickey: '',
  systemPrivatekey: '',
  channelPublickey: '',
  channelPrivatekey: '',
  enable: 0,
  encryptionSwitch: 0,
  tokenType: 0,
  authoritySwitch: 0,
  remark: '',
  createBy: '',
  updateBy: '',
  createTime: '',
  updateTime: ''
})

// 敏感信息查看弹窗
const secretDialogVisible = ref(false)
const secretTitle = ref('')
const secretContent = ref('')
const secretFieldMap: Record<string, string> = {
  appSecret: 'channel.appSecret',
  systemPublickey: 'channel.systemPublickey',
  systemPrivatekey: 'channel.systemPrivatekey',
  channelPublickey: 'channel.channelPublickey',
  channelPrivatekey: 'channel.channelPrivatekey'
}

/**
 * 加载渠道列表数据
 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await getChannelList(searchForm)
    tableData.value = res.rows || []
    total.value = res.total || 0
    // 清空选择
    selectedChannelIds.value = []
  } catch (error) {
    console.error('[ChannelManagement] Failed to load channel list:', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/**
 * 处理选择变化
 */
const handleSelectionChange = (selection: ChannelInfo[]) => {
  selectedChannelIds.value = selection.map(item => item.channelId)
}

/**
 * 处理同步所选渠道
 */
const handleSyncSelected = async () => {
  if (selectedChannelIds.value.length === 0) {
    ElMessage.warning(t('channel.selectChannelToSync'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('channel.syncConfirm'),
      t('message.tips'),
      { type: 'info' }
    )

    loading.value = true
    await syncChannelData({
      channelIds: selectedChannelIds.value
    })
    ElMessage.success(t('channel.syncSuccess'))
    // 清空选择
    selectedChannelIds.value = []
  } catch (error) {
    if (error !== 'cancel') {
      console.error('[ChannelManagement] Failed to sync channels:', error)
    }
  } finally {
    loading.value = false
  }
}

/**
 * 处理搜索操作
 */
const handleSearch = () => {
  searchForm.pageNum = 1
  loadData()
}

/**
 * 处理重置搜索表单
 */
const handleReset = () => {
  searchForm.channelName = ''
  searchForm.enable = undefined
  searchForm.pageNum = 1
  searchForm.pageSize = 10
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
 * 处理新增渠道
 */
const handleAdd = () => {
  isEdit.value = false
  formData.channelId = ''
  formData.channelName = ''
  formData.relaUserId = ''
  formData.enable = 0
  formData.encryptionSwitch = 0
  formData.authoritySwitch = 0
  formData.tokenType = 0
  formData.remark = ''
  selectedUser.value = null
  dialogVisible.value = true
}

/**
 * 处理编辑渠道
 * @param row - 渠道信息
 */
const handleEdit = (row: ChannelInfo) => {
  isEdit.value = true
  formData.channelId = row.channelId
  formData.channelName = row.channelName
  formData.relaUserId = row.relaUserId
  formData.enable = row.enable
  formData.encryptionSwitch = row.encryptionSwitch
  formData.authoritySwitch = row.authoritySwitch
  formData.tokenType = row.tokenType
  formData.remark = row.remark || ''
  // 设置显示名称
  selectedUser.value = row.relaUserId ? {
    userId: 0,
    loginName: row.relaUserId,
    username: row.relaUserId
  } : null
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
      if (isEdit.value) {
        await updateChannel(formData as UpdateChannelParams)
        // 更新列表中对应的数据
        const index = tableData.value.findIndex(item => item.channelId === formData.channelId)
        if (index > -1) {
          tableData.value[index] = { ...tableData.value[index], ...formData }
        }
      } else {
        const addParams: AddChannelParams = {
          channelName: formData.channelName,
          relaUserId: formData.relaUserId || 'admin',
          enable: formData.enable,
          encryptionSwitch: formData.encryptionSwitch,
          tokenType: formData.tokenType,
          authoritySwitch: formData.authoritySwitch,
          remark: formData.remark
        }
        await saveChannel(addParams)
        // 新增渠道需要重新查询
        loadData()
        return
      }
      ElMessage.success(t('message.success'))
      dialogVisible.value = false
    } catch (error) {
      console.error('[ChannelManagement] Failed to submit form:', error)
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
 * 处理加密开关变更前的确认
 * @param row - 渠道信息
 */
const handleEncryptionBeforeChange = async (row: ChannelInfo): Promise<boolean> => {
  const willEnable = row.encryptionSwitch === 1
  const confirmMsg = willEnable ? t('channel.encryptionEnableConfirm') : t('channel.encryptionDisableConfirm')

  try {
    await ElMessageBox.confirm(confirmMsg, t('message.tips'), { type: 'warning' })
  } catch {
    // 用户取消
    return false
  }

  // 用户确认，执行更新
  const newSwitchValue = willEnable ? 0 : 1
  try {
    await updateChannel({
      channelId: row.channelId,
      channelName: row.channelName,
      relaUserId: row.relaUserId,
      enable: row.enable,
      encryptionSwitch: newSwitchValue,
      tokenType: row.tokenType,
      authoritySwitch: row.authoritySwitch,
      remark: row.remark
    })
    ElMessage.success(t('message.success'))
    return true
  } catch (error) {
    console.error('[ChannelManagement] Failed to update encryption switch:', error)
    return false
  }
}

/**
 * 处理删除渠道
 * @param row - 渠道信息
 */
const handleDelete = async (row: ChannelInfo) => {
  try {
    await ElMessageBox.confirm(
      t('common.confirm') + t('common.delete') + '?',
      t('message.tips'),
      { type: 'warning' }
    )
    await deleteChannel({ channelId: row.channelId })
    // 从列表中移除删除的数据
    const index = tableData.value.findIndex(item => item.channelId === row.channelId)
    if (index > -1) {
      tableData.value.splice(index, 1)
      total.value--
    }
    ElMessage.success(t('message.deleteSuccess'))
  } catch {
    // 用户取消删除
  }
}

/**
 * 处理下拉菜单命令
 * @param command - 命令类型
 * @param row - 渠道信息
 */
const handleCommand = async (command: string, row: ChannelInfo) => {
  switch (command) {
    case 'refreshChannelKey':
      try {
        const res = await refreshChannelKey({ channelId: row.channelId })
        // 更新列表中对应的数据
        const index = tableData.value.findIndex(item => item.channelId === row.channelId)
        if (index > -1) {
          tableData.value[index] = res
        }
        ElMessage.success(t('message.success'))
      } catch (error) {
        console.error('[ChannelManagement] Failed to refresh channel key:', error)
      }
      break
    case 'refreshSystemKey':
      try {
        const res = await refreshSystemKey({ channelId: row.channelId })
        // 更新列表中对应的数据
        const index = tableData.value.findIndex(item => item.channelId === row.channelId)
        if (index > -1) {
          tableData.value[index] = res
        }
        ElMessage.success(t('message.success'))
      } catch (error) {
        console.error('[ChannelManagement] Failed to refresh system key:', error)
      }
      break
    case 'issueToken':
      try {
        const res = await issueChannelToken({ channelId: row.channelId, expireMinutes: 60 })
        tokenResult.token = res.token
        tokenResult.expireTime = res.expireTime
        tokenResult.expiresIn = res.expiresIn
        tokenDialogVisible.value = true
      } catch (error) {
        console.error('[ChannelManagement] Failed to issue token:', error)
      }
      break
  }
}

/**
 * 复制令牌到剪贴板
 */
const copyToken = () => {
  navigator.clipboard.writeText(tokenResult.token)
  ElMessage.success(t('common.copy') + t('message.success'))
}

/**
 * 打开用户选择器弹窗
 */
const openUserSelector = () => {
  userSelectorVisible.value = true
  userSearchKeyword.value = ''
  userSearchForm.pageNum = 1
  selectedUser.value = null
  searchUsers()
}

/**
 * 搜索用户列表
 */
const searchUsers = async () => {
  userSelectorLoading.value = true
  try {
    const params: QuerySimpleUserParams = {
      pageNum: userSearchForm.pageNum,
      pageSize: userSearchForm.pageSize
    }
    if (userSearchKeyword.value) {
      params.keyword = userSearchKeyword.value
    }
    const res = await getSimpleUserList(params)
    userList.value = res.rows || []
    userTotal.value = res.total || 0
  } catch (error) {
    console.error('[ChannelManagement] Failed to search users:', error)
    userList.value = []
    userTotal.value = 0
  } finally {
    userSelectorLoading.value = false
  }
}

/**
 * 处理用户选择
 */
const handleUserSelect = (row: SimpleUserInfo | null) => {
  selectedUser.value = row
}

/**
 * 确认用户选择
 */
const confirmUserSelect = () => {
  if (selectedUser.value) {
    formData.relaUserId = selectedUser.value.loginName
    userSelectorVisible.value = false
  }
}

/**
 * 查看用户权限详情
 */
const viewUserPermission = async (user: SimpleUserInfo) => {
  permissionDetailVisible.value = true
  permissionLoading.value = true
  permissionActiveTab.value = 'roles'

  try {
    const res = await getUserPermissionDetail(user.userId)
    permissionDetail.roles = res.roles || []
    permissionDetail.permissions = res.permissions || []
    permissionDetail.dataFilters = res.dataFilters || []
  } catch (error) {
    console.error('[ChannelManagement] Failed to get user permission detail:', error)
    permissionDetail.roles = []
    permissionDetail.permissions = []
    permissionDetail.dataFilters = []
  } finally {
    permissionLoading.value = false
  }
}

/**
 * 处理查看渠道详情
 */
const handleDetail = async (row: ChannelInfo) => {
  try {
    // 只获取渠道基本信息，密钥信息在点击查看时才获取
    const res = await getChannel({ channelId: row.channelId })
    Object.assign(detailData, res)
    // 清空密钥信息
    detailData.appSecret = ''
    detailData.systemPublickey = ''
    detailData.systemPrivatekey = ''
    detailData.channelPublickey = ''
    detailData.channelPrivatekey = ''

    detailDialogVisible.value = true
  } catch (error) {
    console.error('[ChannelManagement] Failed to get channel detail:', error)
  }
}

/**
 * 显示敏感信息
 */
const showSecret = async (field: string) => {
  try {
    const res = await getChannelSecret({
      channelId: detailData.channelId,
      secretField: field as GetChannelSecretParams['secretField']
    })
    secretTitle.value = t(secretFieldMap[field] || field)
    secretContent.value = res.secretValue
    secretDialogVisible.value = true
  } catch (error) {
    console.error('[ChannelManagement] Failed to get channel secret:', error)
  }
}

/**
 * 复制敏感信息
 */
const copySecret = () => {
  navigator.clipboard.writeText(secretContent.value)
  ElMessage.success(t('common.copy') + t('message.success'))
}

// 组件挂载时加载初始数据
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
/* 渠道管理页面 - 继承全局 table-page-container 样式 */

.table-header {
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .selected-info {
    color: var(--el-text-color-secondary);
    font-size: 13px;
    margin-left: 8px;
  }
}

.channel-id,
.app-key {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
}

.mono-text {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
}

.token-input {
  :deep(.el-input-group__append) {
    padding: 0 12px;
  }
}

.dialog-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

.channel-detail {
  :deep(.el-descriptions__label) {
    width: 140px;
    font-weight: 500;
  }

  :deep(.el-descriptions__content) {
    word-break: break-all;
  }
}

.secret-content {
  .secret-textarea {
    :deep(.el-textarea__inner) {
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 12px;
      line-height: 1.5;
    }
  }

  .secret-actions {
    margin-top: 12px;
    text-align: right;
  }
}
</style>