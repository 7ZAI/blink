<template>
  <el-dialog
    v-model="visible"
    title="同步到网关实例"
    width="500px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    class="sync-instance-dialog"
    @open="lockBodyScroll"
    @closed="unlockBodyScroll"
  >
    <el-form label-width="100px">
      <!-- 推送方式选择 -->
      <el-form-item :label="t('route.pushMode')">
        <el-radio-group v-model="pushMode">
          <el-radio value="broadcast">
            <div class="push-mode-option">
              <strong>{{ t('route.broadcastPush') }}</strong>
              <small>{{ t('route.broadcastPushDesc') }}</small>
            </div>
          </el-radio>
          <el-radio value="specified">
            <div class="push-mode-option">
              <strong>{{ t('route.specifiedPush') }}</strong>
              <small>{{ t('route.specifiedPushDesc') }}</small>
            </div>
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 实例选择（仅指定实例模式显示） -->
      <el-form-item v-if="pushMode === 'specified'" :label="t('route.targetInstances')">
        <div v-loading="loadingInstances" class="instance-list">
          <el-checkbox-group v-model="selectedInstances">
            <div v-for="instance in onlineInstances" :key="instance.instanceId" class="instance-item">
              <el-checkbox :value="instance.instanceId">
                <div class="instance-info">
                  <span class="instance-name">{{ instance.instanceId }}</span>
                  <span class="instance-address">{{ instance.host }}:{{ instance.port }}</span>
                  <el-tag type="success" size="small" effect="plain">{{ t('common.online') }}</el-tag>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <el-empty v-if="onlineInstances.length === 0" :description="t('common.noOnlineInstances')" />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="pushMode === 'specified' && selectedInstances.length === 0"
        @click="handleSubmit"
      >
        {{ t('route.confirmPush') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  getOnlineGatewayInstances,
  syncRoutesToInstances,
  type GatewayInstanceVO,
  type SyncRoutesReq,
} from '@/api/route'

const props = defineProps<{
  modelValue: boolean
  routesGroup?: string
  routeIds?: string[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const pushMode = ref<'broadcast' | 'specified'>('broadcast')
const selectedInstances = ref<string[]>([])
const onlineInstances = ref<GatewayInstanceVO[]>([])
const loadingInstances = ref(false)
const submitting = ref(false)

// 监听弹窗打开，加载在线实例
watch(visible, async (val) => {
  if (val) {
    pushMode.value = 'broadcast'
    selectedInstances.value = []
    await loadOnlineInstances()
  }
})

const loadOnlineInstances = async () => {
  loadingInstances.value = true
  try {
    const instances = await getOnlineGatewayInstances()
    onlineInstances.value = Array.isArray(instances) ? instances : []
  } catch (error) {
    console.error('[SyncInstanceDialog] 加载实例列表失败:', error)
    onlineInstances.value = []
  } finally {
    loadingInstances.value = false
  }
}

const handleCancel = () => {
  visible.value = false
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    // 不再传递 storageMode，由后端根据实例配置自动决定
    const req: SyncRoutesReq = {
      pushMode: pushMode.value,
      routeIds: props.routeIds,
      routesGroup: props.routesGroup || 'default',
    }

    if (pushMode.value === 'specified') {
      req.targetInstanceIds = selectedInstances.value
    }

    await syncRoutesToInstances(req)
    ElMessage.success(t('message.success'))
    emit('success')
    visible.value = false
  } catch (error) {
    console.error('[SyncInstanceDialog] 同步失败:', error)
    ElMessage.error(t('message.syncFailed') || '同步失败')
  } finally {
    submitting.value = false
  }
}

// 弹窗防抖动 - 手动锁定滚动条
const lockBodyScroll = () => {
  document.body.classList.add('dialog-open')
}

const unlockBodyScroll = () => {
  document.body.classList.remove('dialog-open')
}
</script>

<style scoped lang="scss">
.sync-instance-dialog {
  .push-mode-option {
    display: flex;
    flex-direction: column;
    small {
      color: #909399;
      font-size: 12px;
    }
  }

  .instance-list {
    max-height: 300px;
    overflow-y: auto;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 8px;

    .instance-item {
      padding: 8px;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .instance-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .instance-name {
          font-weight: 500;
        }

        .instance-address {
          color: #909399;
          font-size: 12px;
        }
      }
    }
  }
}
</style>