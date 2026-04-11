<template>
  <!-- 配置管理页面 - 多实例设计 -->
  <div class="config-page">
    <!-- 左侧面板：实例列表 -->
    <div class="left-panel">
      <div class="panel-header">
        <div class="header-title">
          <div class="title-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="20" rx="2" />
              <circle cx="12" cy="12" r="3" />
              <path d="M12 2v4M12 18v4M2 12h4M18 12h4" />
            </svg>
          </div>
          <h3>{{ t('config.instanceList') }}</h3>
        </div>
        <el-button
          type="primary"
          size="small"
          :icon="Refresh"
          class="refresh-btn"
          @click="loadInstances"
          :loading="instanceLoading"
        >
          {{ t('common.refresh') }}
        </el-button>
      </div>

      <!-- 实例统计 -->
      <div class="instance-stats">
        <div class="stat-item">
          <span class="stat-label">{{ t('config.instancesTotal', { count: instances.length }) }}</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value online">{{ onlineInstances.length }}</span>
          <span class="stat-label">{{ t('config.online') }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-value offline">{{ instances.length - onlineInstances.length }}</span>
          <span class="stat-label">{{ t('config.offline') }}</span>
        </div>
      </div>

      <div class="panel-content">
        <el-scrollbar>
          <div
            v-for="instance in instances"
            :key="instance.instanceId"
            class="instance-item"
            :class="{
              active: selectedInstance?.instanceId === instance.instanceId,
              offline: instance.status !== 0
            }"
            @click="selectInstance(instance)"
          >
            <div class="instance-indicator" :class="instance.status === 0 ? 'online' : 'offline'"></div>
            <div class="instance-info">
              <div class="instance-id">{{ instance.instanceId }}</div>
              <div class="instance-meta">
                <span class="instance-address">{{ instance.uri }}</span>
              </div>
            </div>
            <div class="instance-status-badge">
              <el-tag
                :type="instance.status === 0 ? 'success' : 'danger'"
                size="small"
                effect="dark"
              >
                {{ instance.status === 0 ? t('config.online') : t('config.offline') }}
              </el-tag>
            </div>
          </div>
          <el-empty
            v-if="instances.length === 0 && !instanceLoading"
            :description="t('config.noOnlineInstances')"
            class="empty-state"
          />
        </el-scrollbar>
      </div>
    </div>

    <!-- 右侧面板：配置内容 -->
    <div class="right-panel">
      <!-- 配置分组导航 -->
      <div class="config-nav">
        <div class="nav-header">
          <div class="header-left">
            <div class="title-icon config-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </div>
            <h3>{{ t('config.gatewayConfig') }}</h3>
            <span v-if="selectedInstance" class="selected-instance-badge">
              {{ selectedInstance.instanceId }}
            </span>
          </div>
          <div class="header-right">
            <el-button
              type="primary"
              :icon="Promotion"
              class="push-btn"
              @click="handlePushConfig"
              :loading="pushLoading"
            >
              {{ t('config.syncToInstance') }}
            </el-button>
          </div>
        </div>

        <div class="nav-tabs">
          <div
            v-for="group in configGroups"
            :key="group.key"
            class="nav-tab"
            :class="{ active: activeGroup === group.key }"
            @click="handleGroupChange(group.key)"
          >
            <BlinkIcon :icon="group.icon" size="16" class="tab-icon" />
            <span class="tab-label">{{ group.label }}</span>
          </div>
        </div>
      </div>

      <!-- 配置内容区域 -->
      <div class="config-content">
        <div v-loading="configLoading" class="content-body">
          <div v-if="filteredConfigs.length === 0" class="empty-state">
            <el-empty :description="t('config.noConfig')" />
          </div>

          <div v-else class="config-table">
            <div class="config-row header-row">
              <div class="col-name">{{ t('config.configName') }}</div>
              <div class="col-value">{{ t('config.configValue') }}</div>
              <div class="col-desc">{{ t('config.description') }}</div>
            </div>

            <div
              v-for="config in filteredConfigs"
              :key="config.id"
              class="config-row"
              :class="{ 'is-modified': isModified(config.id) }"
            >
              <div class="col-name">
                <span class="name-text">{{ config.configName }}</span>
              </div>
              <div class="col-value">
                <SmartConfigInput
                  :config-key="config.configKey"
                  :config-type="config.configType"
                  :model-value="configStates[config.id]"
                  size="small"
                  @update:model-value="(val) => handleValueUpdate(config.id, val)"
                  @change="markAsModified(config.id)"
                />
              </div>
              <div class="col-desc">{{ config.description }}</div>
            </div>

            <!-- 底部操作按钮 -->
            <div class="config-row action-row" v-if="hasChanges">
              <div class="col-name">
                <span class="changes-count">
                  {{ t('config.modifiedCount', { count: modifiedIds.length }) }}
                </span>
              </div>
              <div class="col-value">
                <div class="action-buttons">
                  <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
                  <AuthButton
                    :has-permission="() => checkPermission(ButtonPerms.Config.Update)"
                    type="primary"
                    :loading="saving"
                    @click="handleBatchSave"
                  >
                    <el-icon><Check /></el-icon>
                    {{ t('common.save') }}
                  </AuthButton>
                </div>
              </div>
              <div class="col-desc"></div>
            </div>
          </div>
        </div>

        <!-- 配置同步提示 -->
        <div class="config-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ t('config.syncConfigTip') }}</span>
        </div>
      </div>
    </div>

    <!-- 推送配置弹窗 -->
    <el-dialog
      v-model="pushDialogVisible"
      :title="t('config.syncToInstance')"
      width="500px"
      class="push-dialog"
      :close-on-click-modal="false"
    >
      <div class="dialog-tip">{{ t('config.syncConfigTip') }}</div>
      <el-form label-width="100px" class="push-form">
        <el-form-item :label="t('config.pushConfig')">
          <el-radio-group v-model="pushMode" class="push-mode-radio">
            <el-radio value="broadcast">
              <div class="radio-content">
                <span class="radio-label">{{ t('instanceRoute.broadcastMode') }}</span>
                <span class="radio-desc">{{ t('instanceRoute.broadcastToAll') }}</span>
              </div>
            </el-radio>
            <el-radio value="specified">
              <div class="radio-content">
                <span class="radio-label">{{ t('instanceRoute.specifiedMode') }}</span>
                <span class="radio-desc">{{ t('instanceRoute.specifiedInstances') }}</span>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="pushMode === 'specified'" :label="t('config.selectInstance')">
          <el-select v-model="targetInstanceIds" multiple style="width: 100%" class="form-select">
            <el-option
              v-for="inst in onlineInstances"
              :key="inst.instanceId"
              :label="inst.instanceId"
              :value="inst.instanceId"
            >
              <div class="instance-option">
                <span class="option-id">{{ inst.instanceId }}</span>
                <span class="option-uri">{{ inst.uri }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pushDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleConfirmPush" :loading="pushLoading">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 配置管理页面 - 多实例设计
 * 左边显示实例列表，右边显示配置信息
 *
 * @author binblink
 * @since 2024-01-01
 */
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Promotion, Check, InfoFilled } from '@element-plus/icons-vue'
import {
  configApi,
  getConfigsByGroupKey,
  batchUpdateConfigs,
  type ConfigItem,
  type GatewayConfig,
  type GatewayInstanceVO,
} from '@/api/config'
import { routeApi } from '@/api/route'
import { ButtonPerms, usePermission } from '@/composables/usePermission'
import SmartConfigInput from '@/components/SmartConfigInput.vue'

defineOptions({
  name: 'ConfigManagement',
})

const { t } = useI18n()
const { hasPermission: checkPermission } = usePermission()

// 实例状态
const instances = ref<GatewayInstanceVO[]>([])
const selectedInstance = ref<GatewayInstanceVO | null>(null)
const instanceLoading = ref(false)

// 配置状态
const activeGroup = ref('gateway-security')
const currentConfigs = ref<ConfigItem[]>([])
const configStates = ref<Record<number, any>>({})
const originalStates = ref<Record<number, any>>({})
const modifiedIds = ref<number[]>([])
const configLoading = ref(false)
const saving = ref(false)

// 推送状态
const pushDialogVisible = ref(false)
const pushLoading = ref(false)
const pushMode = ref<'broadcast' | 'specified'>('broadcast')
const targetInstanceIds = ref<string[]>([])

// 在线实例计算属性
const onlineInstances = computed(() =>
  instances.value.filter(inst => inst.status === 0)
)

// 配置分组
const configGroups = computed(() => [
  { key: 'gateway-security', label: t('config.groupSecurity'), icon: 'mdi:shield-check' },
  { key: 'gateway-ip', label: t('config.groupIpFilter'), icon: 'mdi:filter' },
  { key: 'gateway-route', label: t('config.groupRoute'), icon: 'mdi:routes' },
  { key: 'gateway-system', label: t('config.groupSystem'), icon: 'mdi:cog' },
])

// 配置键前缀映射
const groupKeyMapping: Record<string, string[]> = {
  'gateway-security': ['gateway:signature', 'gateway:replay'],
  'gateway-ip': ['gateway:ip'],
  'gateway-route': ['gateway:route'],
  'gateway-system': ['gateway:event', 'gateway:local', 'gateway:api'],
}

const filteredConfigs = computed(() => {
  const prefixes = groupKeyMapping[activeGroup.value] || []
  let configs = currentConfigs.value

  // 按前缀过滤
  if (prefixes.length > 0) {
    configs = configs.filter((config) =>
      prefixes.some((prefix) => config.configKey.startsWith(prefix))
    )
  }

  return configs
})

const hasChanges = computed(() => modifiedIds.value.length > 0)

const isModified = (id: number) => modifiedIds.value.includes(id)

const markAsModified = (id: number) => {
  if (!modifiedIds.value.includes(id)) {
    modifiedIds.value.push(id)
  }
}

// 加载实例列表
async function loadInstances() {
  instanceLoading.value = true
  try {
    const result = await routeApi.getOnlineGatewayInstances()
    instances.value = result || []
    // 自动选择第一个在线实例
    const firstInstance = result && result[0]
    if (firstInstance && firstInstance.status === 0 && !selectedInstance.value) {
      selectInstance(firstInstance)
    }
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    instanceLoading.value = false
  }
}

// 选择实例
function selectInstance(instance: GatewayInstanceVO) {
  selectedInstance.value = instance
}

// 加载配置
async function loadConfigs() {
  configLoading.value = true
  modifiedIds.value = []
  try {
    // 获取 gateway 分组的所有配置
    const data = await getConfigsByGroupKey('gateway')
    currentConfigs.value = data?.configs || []

    const states: Record<number, any> = {}
    const originals: Record<number, any> = {}
    currentConfigs.value.forEach((config) => {
      if (config.configType === 2) {
        states[config.id] = config.configValue === 'true'
        originals[config.id] = config.configValue === 'true'
      } else if (config.configType === 1) {
        states[config.id] = parseInt(config.configValue) || 0
        originals[config.id] = parseInt(config.configValue) || 0
      } else {
        states[config.id] = config.configValue
        originals[config.id] = config.configValue
      }
    })
    configStates.value = states
    originalStates.value = originals
  } catch (error) {
    currentConfigs.value = []
  } finally {
    configLoading.value = false
  }
}

// 处理分组切换
function handleGroupChange(groupKey: string) {
  if (hasChanges.value) {
    ElMessage.warning(t('config.pleaseSaveOrCancel'))
    return
  }
  activeGroup.value = groupKey
}

// 处理值更新
function handleValueUpdate(configId: number, value: any) {
  configStates.value[configId] = value
}

// 批量保存
async function handleBatchSave() {
  if (modifiedIds.value.length === 0) {
    ElMessage.warning(t('config.noChanges'))
    return
  }

  saving.value = true
  try {
    const configs = modifiedIds.value.map((id) => {
      const config = currentConfigs.value.find((c) => c.id === id)
      let value = configStates.value[id]
      if (config?.configType === 2) {
        value = value ? 'true' : 'false'
      } else if (config?.configType === 1) {
        value = String(value)
      }
      return {
        id,
        configKey: config!.configKey,
        configValue: value,
      }
    })

    await batchUpdateConfigs(configs)
    ElMessage.success(t('message.saveSuccess'))
    modifiedIds.value = []

    // 更新原始状态
    currentConfigs.value.forEach((config) => {
      originalStates.value[config.id] = configStates.value[config.id]
    })
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    saving.value = false
  }
}

// 取消修改
function handleCancel() {
  configStates.value = { ...originalStates.value }
  modifiedIds.value = []
}

// 打开推送弹窗
function handlePushConfig() {
  if (hasChanges.value) {
    ElMessage.warning(t('config.pleaseSaveOrCancel'))
    return
  }
  pushMode.value = 'broadcast'
  targetInstanceIds.value = []
  pushDialogVisible.value = true
}

// 确认推送
async function handleConfirmPush() {
  if (pushMode.value === 'specified' && targetInstanceIds.value.length === 0) {
    ElMessage.warning(t('config.selectInstance'))
    return
  }

  pushLoading.value = true
  try {
    await configApi.pushConfigToInstance({
      instanceIds: pushMode.value === 'specified' ? targetInstanceIds.value : undefined,
      broadcast: pushMode.value === 'broadcast',
    })
    ElMessage.success(t('config.syncSuccess'))
    pushDialogVisible.value = false
  } catch (error) {
    ElMessage.error(t('config.syncFailed'))
  } finally {
    pushLoading.value = false
  }
}

// 初始化
onMounted(() => {
  loadInstances()
  loadConfigs()
})
</script>

<style scoped lang="scss">
// 配置管理页面 - 多实例设计
.config-page {
  display: flex;
  gap: 20px;
  height: 100%;
  padding: 20px;
  background: var(--el-bg-color-page);
}

// 左侧面板
.left-panel {
  width: 300px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);

  .panel-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--el-border-color-light);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      display: flex;
      align-items: center;
      gap: 10px;

      .title-icon {
        width: 32px;
        height: 32px;
        background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;

        svg {
          width: 18px;
          height: 18px;
          color: white;
        }
      }

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }
    }

    .refresh-btn {
      border-radius: 8px;
    }
  }

  .instance-stats {
    padding: 12px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    background: var(--el-fill-color-light);
    border-bottom: 1px solid var(--el-border-color-lighter);

    .stat-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .stat-label {
        font-size: 13px;
        color: var(--el-text-color-secondary);
      }

      .stat-value {
        font-size: 18px;
        font-weight: 600;
        font-family: 'SF Mono', 'Monaco', monospace;

        &.online { color: var(--el-color-success); }
        &.offline { color: var(--el-color-danger); }
      }
    }

    .stat-divider {
      width: 1px;
      height: 20px;
      background: var(--el-border-color);
    }
  }

  .panel-content {
    flex: 1;
    overflow: hidden;
  }

  .instance-item {
    padding: 14px 20px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 12px;
    position: relative;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.active {
      background: var(--el-color-primary-light-9);
      border-left: 3px solid var(--el-color-primary);
      padding-left: 17px;
    }

    &.offline {
      opacity: 0.7;
    }

    .instance-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      transition: all 0.3s ease;

      &.online {
        background: var(--el-color-success);
        box-shadow: 0 0 8px var(--el-color-success-light-3);
      }

      &.offline {
        background: var(--el-color-danger);
      }
    }

    .instance-info {
      flex: 1;
      min-width: 0;

      .instance-id {
        font-size: 14px;
        font-weight: 500;
        color: var(--el-text-color-primary);
        margin-bottom: 4px;
        font-family: 'SF Mono', 'Monaco', monospace;
      }

      .instance-meta {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .instance-status-badge {
      :deep(.el-tag) {
        border-radius: 6px;
      }
    }
  }

  .empty-state {
    padding: 40px 20px;
  }
}

// 右侧面板
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.config-nav {
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--el-border-color-light);
  margin-bottom: 16px;

  .nav-header {
    padding: 16px 20px;
    border-bottom: 1px solid var(--el-border-color-light);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .title-icon {
        width: 32px;
        height: 32px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;

        svg {
          width: 18px;
          height: 18px;
          color: white;
        }

        &.config-icon {
          background: linear-gradient(135deg, var(--el-color-warning), var(--el-color-warning-light-3));
        }
      }

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: var(--el-text-color-primary);
      }

      .selected-instance-badge {
        padding: 4px 10px;
        background: var(--el-color-primary-light-9);
        border-radius: 6px;
        font-size: 12px;
        font-family: 'SF Mono', 'Monaco', monospace;
        color: var(--el-color-primary);
      }
    }

    .header-right {
      .push-btn {
        border-radius: 8px;
      }
    }
  }

  .nav-tabs {
    display: flex;
    padding: 8px 16px;
    gap: 12px;

    .nav-tab {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 16px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
      color: var(--el-text-color-regular);
      font-size: 14px;

      &:hover {
        background: var(--el-fill-color-light);
        color: var(--el-color-primary);
      }

      &.active {
        background: var(--el-color-primary-light-9);
        color: var(--el-color-primary);
        font-weight: 500;
      }

      .tab-icon {
        opacity: 0.8;
      }
    }
  }
}

.config-content {
  flex: 1;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .content-body {
    flex: 1;
    overflow-y: auto;
    padding: 16px 20px;
  }

  .config-tip {
    padding: 12px 20px;
    background: var(--el-color-info-light-9);
    border-top: 1px solid var(--el-border-color-light);
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--el-text-color-secondary);

    .el-icon {
      color: var(--el-color-info);
    }
  }
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}

.config-table {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  overflow: hidden;

  .config-row {
    display: flex;
    align-items: center;
    border-bottom: 1px solid var(--el-border-color-lighter);
    transition: background 0.2s ease;

    &:last-child {
      border-bottom: none;
    }

    &:hover:not(.header-row) {
      background: var(--el-fill-color);
    }

    &.is-modified {
      background: var(--el-color-primary-light-9);
      border-left: 3px solid var(--el-color-primary);

      .col-name {
        padding-left: 9px;
      }
    }

    &.header-row {
      background: var(--el-fill-color);
      font-weight: 500;
      color: var(--el-text-color-secondary);
      font-size: 12px;

      .col-name,
      .col-desc,
      .col-value {
        padding: 10px 12px;
      }
    }

    &.action-row {
      background: var(--el-color-primary-light-9);
      border-top: 2px solid var(--el-color-primary-light-7);

      .col-name {
        .changes-count {
          font-size: 13px;
          color: var(--el-text-color-secondary);
        }
      }

      .col-value {
        .action-buttons {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }
    }

    .col-name {
      width: 200px;
      flex-shrink: 0;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 2px;

      .name-text {
        font-size: 13px;
        font-weight: 500;
        color: var(--el-text-color-primary);
      }
    }

    .col-value {
      width: 280px;
      flex-shrink: 0;
      padding: 12px;
      display: flex;
      align-items: center;
      justify-content: flex-start;
    }

    .col-desc {
      flex: 1;
      padding: 12px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      line-height: 1.5;
    }
  }
}

// 实例选项
.instance-option {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .option-id {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-weight: 500;
  }

  .option-uri {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>

<style lang="scss">
// 推送弹窗样式（非 scoped）
.push-dialog {
  .dialog-tip {
    padding: 12px 16px;
    background: var(--el-color-primary-light-9);
    border-radius: 8px;
    margin-bottom: 16px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    border-left: 3px solid var(--el-color-primary);
  }

  .push-form {
    .push-mode-radio {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .el-radio {
        height: auto;
        padding: 12px 16px;
        background: var(--el-fill-color-light);
        border-radius: 8px;
        margin-right: 0;

        &.is-checked {
          background: var(--el-color-primary-light-9);
          border: 1px solid var(--el-color-primary);
        }
      }

      .radio-content {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .radio-label {
          font-size: 14px;
          font-weight: 500;
          color: var(--el-text-color-primary);
        }

        .radio-desc {
          font-size: 12px;
          color: var(--el-text-color-secondary);
        }
      }
    }
  }
}
</style>