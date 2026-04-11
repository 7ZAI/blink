<template>
  <!-- 实例配置页面 - 查看各实例的独立配置 -->
  <div class="instance-config-page">
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

    <!-- 右侧面板：实例配置详情 -->
    <div class="right-panel">
      <div class="config-section">
        <div class="section-header">
          <div class="header-title">
            <div class="title-icon config-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3" />
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
              </svg>
            </div>
            <h3>{{ t('config.instanceConfig') }}</h3>
            <span v-if="selectedInstance" class="selected-instance-badge">
              {{ selectedInstance.instanceId }}
            </span>
          </div>
        </div>

        <div class="section-content">
          <el-empty v-if="!selectedInstance" :description="t('config.selectInstance')" class="empty-state" />

          <div v-else v-loading="configLoading" class="config-detail">
            <div class="config-tabs">
              <div
                v-for="group in configGroups"
                :key="group.key"
                class="config-tab"
                :class="{ active: activeGroup === group.key }"
                @click="activeGroup = group.key"
              >
                <BlinkIcon :icon="group.icon" size="16" class="tab-icon" />
                <span class="tab-label">{{ group.label }}</span>
              </div>
            </div>

            <div class="config-list">
              <div
                v-for="config in filteredConfigs"
                :key="config.configKey"
                class="config-item"
              >
                <div class="config-label">
                  <span class="label-text">{{ config.configName }}</span>
                  <span class="label-key">{{ config.configKey }}</span>
                </div>
                <div class="config-value">
                  <SmartConfigInput
                    :config-key="config.configKey"
                    :config-type="config.configType"
                    :model-value="getConfigValue(config.configKey)"
                    size="small"
                    readonly
                  />
                </div>
                <div class="config-desc">{{ config.description }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { routeApi, type GatewayInstanceVO } from '@/api/route'
import { type ConfigItem } from '@/api/config'
import SmartConfigInput from '@/components/SmartConfigInput.vue'

const { t } = useI18n()

// 实例状态
const instances = ref<GatewayInstanceVO[]>([])
const selectedInstance = ref<GatewayInstanceVO | null>(null)
const instanceLoading = ref(false)

// 配置状态
const activeGroup = ref('security')
const configLoading = ref(false)
const instanceConfigs = ref<Record<string, any>>({})

// 配置仓库数据（用于显示配置定义）
const repositoryConfigs = ref<ConfigItem[]>([])

// 在线实例计算属性
const onlineInstances = computed(() =>
  instances.value.filter(inst => inst.status === 0)
)

// 配置分组
const configGroups = computed(() => [
  { key: 'security', label: t('config.groupSecurity'), icon: 'mdi:shield-check' },
  { key: 'ip', label: t('config.groupIpFilter'), icon: 'mdi:filter' },
  { key: 'route', label: t('config.groupRoute'), icon: 'mdi:routes' },
  { key: 'system', label: t('config.groupSystem'), icon: 'mdi:cog' },
])

const filteredConfigs = computed(() => {
  return repositoryConfigs.value.filter(c => {
    if (activeGroup.value === 'security') return c.configKey?.startsWith('gateway:signature') || c.configKey?.startsWith('gateway:replay')
    if (activeGroup.value === 'ip') return c.configKey?.startsWith('gateway:ip')
    if (activeGroup.value === 'route') return c.configKey?.startsWith('gateway:route')
    if (activeGroup.value === 'system') return c.configKey?.startsWith('gateway:event') || c.configKey?.startsWith('gateway:local') || c.configKey?.startsWith('gateway:api')
    return true
  })
})

function getConfigValue(configKey: string): any {
  const value = instanceConfigs.value[configKey]
  if (value === undefined) return ''

  // 根据配置类型解析值
  const config = repositoryConfigs.value.find(c => c.configKey === configKey)
  if (config?.configType === 2) {
    return value === 'true' || value === true
  }
  return value
}

// 加载实例列表
async function loadInstances() {
  instanceLoading.value = true
  try {
    const result = await routeApi.getOnlineGatewayInstances()
    instances.value = result || []
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    instanceLoading.value = false
  }
}

// 选择实例
async function selectInstance(instance: GatewayInstanceVO) {
  selectedInstance.value = instance
  configLoading.value = true
  try {
    // 获取实例配置 - 这里需要后端接口支持
    // const result = await configApi.getInstanceConfig({ instanceId: instance.instanceId })
    // instanceConfigs.value = result || {}
    instanceConfigs.value = {} // 临时处理
  } catch (error) {
    ElMessage.error(t('message.fetchFailed'))
  } finally {
    configLoading.value = false
  }
}

// 加载配置仓库（获取配置定义）
async function loadRepositoryConfigs() {
  // 后端接口待实现
  repositoryConfigs.value = []
}

onMounted(() => {
  loadInstances()
  loadRepositoryConfigs()
})
</script>

<style scoped lang="scss">
.instance-config-page {
  display: flex;
  gap: 20px;
  height: 100%;
  padding: 20px;
  background: var(--el-bg-color-page);
}

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
  }

  .instance-stats {
    padding: 12px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    background: var(--el-fill-color-light);

    .stat-item {
      display: flex;
      align-items: center;
      gap: 6px;
      .stat-label { font-size: 13px; color: var(--el-text-color-secondary); }
      .stat-value {
        font-size: 18px;
        font-weight: 600;
        font-family: 'SF Mono', 'Monaco', monospace;
        &.online { color: var(--el-color-success); }
      }
    }

    .stat-divider {
      width: 1px;
      height: 20px;
      background: var(--el-border-color);
    }
  }

  .panel-content { flex: 1; overflow: hidden; }

  .instance-item {
    padding: 14px 20px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 12px;

    &:hover { background: var(--el-fill-color-light); }

    &.active {
      background: var(--el-color-primary-light-9);
      border-left: 3px solid var(--el-color-primary);
      padding-left: 17px;
    }

    .instance-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      &.online { background: var(--el-color-success); box-shadow: 0 0 8px var(--el-color-success-light-3); }
      &.offline { background: var(--el-color-danger); }
    }

    .instance-info {
      flex: 1;
      .instance-id { font-size: 14px; font-weight: 500; font-family: 'SF Mono', 'Monaco', monospace; }
      .instance-meta { font-size: 12px; color: var(--el-text-color-secondary); }
    }
  }

  .empty-state { padding: 40px 20px; }
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.config-section {
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  height: 100%;

  .section-header {
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
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        svg { width: 18px; height: 18px; color: white; }
        &.config-icon { background: linear-gradient(135deg, var(--el-color-info), var(--el-color-info-light-3)); }
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
  }

  .section-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px 20px;
  }
}

.config-detail {
  .config-tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;

    .config-tab {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 12px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 14px;
      color: var(--el-text-color-regular);
      transition: all 0.2s;

      &:hover { background: var(--el-fill-color-light); color: var(--el-color-primary); }
      &.active { background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-weight: 500; }
    }
  }

  .config-list {
    .config-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      background: var(--el-fill-color-lighter);
      border-radius: 8px;
      margin-bottom: 8px;

      .config-label {
        width: 180px;
        display: flex;
        flex-direction: column;
        gap: 2px;
        .label-text { font-size: 14px; font-weight: 500; color: var(--el-text-color-primary); }
        .label-key { font-size: 12px; font-family: 'SF Mono', 'Monaco', monospace; color: var(--el-text-color-secondary); }
      }

      .config-value {
        width: 200px;
      }

      .config-desc {
        flex: 1;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}
</style>