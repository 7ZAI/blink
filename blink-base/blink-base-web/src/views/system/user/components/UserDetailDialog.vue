<template>
  <el-dialog
    v-model="visible"
    title="用户详情"
    width="600px"
    :close-on-click-modal="false"
    :lock-scroll="false"
  >
    <el-descriptions :column="2" border v-if="userDetail" v-loading="loading">
      <el-descriptions-item label="用户ID">{{ userDetail.userId }}</el-descriptions-item>
      <el-descriptions-item label="登录名">{{ userDetail.loginName }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ userDetail.username }}</el-descriptions-item>
      <el-descriptions-item label="头像">
        <el-avatar :size="40" :src="getAvatarUrl(userDetail.avatar, userDetail.avatarStyle, userDetail.loginName)" />
      </el-descriptions-item>
      <el-descriptions-item label="性别">
        <el-tag v-if="userDetail.sex === 1" type="primary">男</el-tag>
        <el-tag v-else-if="userDetail.sex === 2" type="danger">女</el-tag>
        <el-tag v-else type="info">不确定</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="电话">{{ userDetail.phone }}</el-descriptions-item>
      <el-descriptions-item label="邮箱" :span="2">{{ userDetail.email }}</el-descriptions-item>
      <el-descriptions-item label="锁定状态">
        <el-tag v-if="userDetail.locked === 0" type="success">正常</el-tag>
        <el-tag v-else-if="userDetail.locked === 1" type="danger">管理员锁定</el-tag>
        <el-tag v-else type="warning">密码锁定</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="超级管理员">
        <el-tag v-if="userDetail.superFlag === 1" type="warning">是</el-tag>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="密码重试次数">{{ userDetail.pswRetry }}</el-descriptions-item>
      <el-descriptions-item label="最后登录时间">{{ userDetail.lastLoginTime }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ userDetail.createBy }}</el-descriptions-item>
      <el-descriptions-item label="更新者">{{ userDetail.updateBy }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ userDetail.createTime }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ userDetail.updateTime }}</el-descriptions-item>
      <el-descriptions-item label="锁定时间">{{ userDetail.lockTime || '-' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ userDetail.remark || '-' }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button type="primary" @click="handleViewPermission">
        <el-icon><View /></el-icon>查看角色权限
      </el-button>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 用户权限弹窗 -->
  <UserPermissionDialog
    v-model="permissionVisible"
    :user-id="userId"
  />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getUserDetail, type UserDetail } from '@/api/user'
import { getAvatarUrl } from '@/utils/avatar'
import { View } from '@element-plus/icons-vue'
import UserPermissionDialog from './UserPermissionDialog.vue'

interface Props {
  modelValue: boolean
  loginName: string
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const loading = ref(false)
const userDetail = ref<UserDetail | null>(null)
const permissionVisible = ref(false)

const userId = computed(() => userDetail.value?.userId || null)

const loadUserDetail = async () => {
  if (!props.loginName) return
  loading.value = true
  try {
    userDetail.value = await getUserDetail(props.loginName)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  visible.value = false
}

const handleViewPermission = () => {
  permissionVisible.value = true
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.loginName) {
      loadUserDetail()
    }
  }
)
</script>
