<template>
  <el-dialog
    :title="t('system.user.userDetail')"
    v-model="visible"
    width="700px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <div v-loading="loading" class="user-detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('system.user.loginName')">
          {{ userDetail?.loginName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.username')">
          {{ userDetail?.username || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.sex')">
          <span v-if="userDetail?.sex === 1">{{ t('system.user.male') }}</span>
          <span v-else-if="userDetail?.sex === 2">{{ t('system.user.female') }}</span>
          <span v-else>{{ t('system.user.unknown') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.phone')">
          {{ userDetail?.phone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.email')">
          {{ userDetail?.email || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.locked')">
          <el-tag v-if="userDetail?.locked === 0" type="success">
            {{ t('system.user.unlocked') }}
          </el-tag>
          <el-tag v-else type="danger">{{ t('system.user.locked') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.lastLoginTime')">
          {{ userDetail?.lastLoginTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.createTime')">
          {{ userDetail?.createTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('system.user.remark')" :span="2">
          {{ userDetail?.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <template #footer>
      <el-button type="primary" @click="handleViewPermission">
        <el-icon><View /></el-icon>
        {{ t('system.user.viewRolePermission') }}
      </el-button>
      <el-button @click="visible = false">{{ t('common.close') }}</el-button>
    </template>
  </el-dialog>

  <!-- 用户权限弹窗 -->
  <UserPermissionDialog v-model="permissionVisible" :user-id="userId" />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { View } from '@element-plus/icons-vue'
import { getUserDetail, type UserDetail } from '@/api/user'
import UserPermissionDialog from './UserPermissionDialog.vue'

interface Props {
  modelValue: boolean
  loginName: string
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const userDetail = ref<UserDetail | null>(null)
const permissionVisible = ref(false)

const userId = computed(() => userDetail.value?.userId || null)

const fetchUserDetail = async () => {
  if (!props.loginName) return

  loading.value = true
  try {
    userDetail.value = await getUserDetail(props.loginName)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  userDetail.value = null
}

const handleViewPermission = () => {
  permissionVisible.value = true
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchUserDetail()
    }
  }
)
</script>

<style scoped lang="scss">
.user-detail {
  :deep(.el-descriptions) {
    .el-descriptions__label {
      width: 120px;
    }
  }
}
</style>
