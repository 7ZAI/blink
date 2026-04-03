<template>
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="900px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <div v-loading="loading" class="role-form-container">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="role-form"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="t('role.roleName')" prop="roleName">
              <el-input v-model.trim="form.roleName" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('role.roleEnName')" prop="roleEnName">
              <el-input v-model.trim="form.roleEnName" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('role.roleCode')" prop="roleCode">
              <el-input v-model.trim="form.roleCode" :placeholder="t('common.pleaseInput')" :disabled="props.type === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('common.status')" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="0">{{ t('role.statusEnable') }}</el-radio>
                <el-radio :value="1">{{ t('role.statusDisable') }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template v-if="props.type === 'edit'">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane :label="t('role.permissionList')" name="permissions">
            <!-- 权限子页签 -->
            <el-tabs v-model="permissionSubTab" class="sub-tabs">
              <el-tab-pane :label="t('role.apiPermission')" name="api">
                <el-table :data="apiPermissions" stripe border max-height="200">
                  <el-table-column prop="acName" :label="t('permission.acName')" min-width="120" />
                  <el-table-column prop="acIdentity" :label="t('permission.acIdentity')" min-width="140" />
                  <el-table-column prop="url" label="URL" min-width="180" />
                </el-table>
                <el-empty v-if="!apiPermissions.length" :description="t('common.noData')" />
              </el-tab-pane>
              <el-tab-pane :label="t('role.dataPermission')" name="data">
                <el-table :data="dataPermissions" stripe border max-height="200">
                  <el-table-column prop="acName" :label="t('permission.acName')" min-width="120" />
                  <el-table-column prop="acIdentity" :label="t('permission.acIdentity')" min-width="140" />
                  <el-table-column prop="dataFilterName" :label="t('permission.dataFilterId')" min-width="140" />
                </el-table>
                <el-empty v-if="!dataPermissions.length" :description="t('common.noData')" />
              </el-tab-pane>
            </el-tabs>
          </el-tab-pane>

          <el-tab-pane :label="t('role.menuList')" name="menus">
            <el-table
              :data="menuTreeData"
              stripe
              border
              max-height="250"
              row-key="menuId"
              :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
              default-expand-all
            >
              <el-table-column prop="menuName" :label="t('menu.menuName')" min-width="180" />
              <el-table-column prop="url" label="URL" min-width="150" />
              <el-table-column prop="type" :label="t('menu.type')" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.type === 1" type="info">{{ t('menu.typeDirectory') }}</el-tag>
                  <el-tag v-else-if="row.type === 2" type="primary">{{ t('menu.typeMenu') }}</el-tag>
                  <el-tag v-else-if="row.type === 3" type="warning">{{ t('menu.typeButton') }}</el-tag>
                  <el-tag v-else type="info">-</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" :label="t('common.status')" width="80" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.status === 0" type="success">{{ t('common.show') }}</el-tag>
                  <el-tag v-else type="info">{{ t('common.hide') }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!roleDetail?.menus?.length" :description="t('common.noData')" />
          </el-tab-pane>

          <el-tab-pane :label="t('role.userList')" name="users">
            <el-table :data="roleDetail?.users || []" stripe border max-height="200">
              <el-table-column prop="loginName" :label="t('user.loginName')" min-width="120" />
              <el-table-column prop="username" :label="t('user.username')" min-width="120" />
              <el-table-column prop="phone" :label="t('user.phone')" min-width="120" />
              <el-table-column prop="email" :label="t('user.email')" min-width="150" />
              <el-table-column prop="locked" :label="t('user.locked')" width="80" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.locked === 0" type="success">{{ t('user.unlocked') }}</el-tag>
                  <el-tag v-else type="danger">{{ t('user.locked') }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!roleDetail?.users?.length" :description="t('common.noData')" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { addRole, updateRole, getRoleDetail, type RoleInfo, type RoleDetailRsp, type MenuInfo } from '@/api/role'
import { useSubmitGuard } from '@/composables/useSubmitGuard'

interface Props {
  modelValue: boolean
  type: 'add' | 'edit'
  data: RoleInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const dialogTitle = computed(() =>
  props.type === 'add' ? t('role.addRole') : t('role.editRole')
)

// 接口权限列表（ac_type=1）
const apiPermissions = computed(() =>
  (roleDetail.value?.permissions || []).filter((p) => p.acType === 1)
)

// 数据权限列表（ac_type=2）
const dataPermissions = computed(() =>
  (roleDetail.value?.permissions || []).filter((p) => p.acType === 2)
)

/**
 * 构建菜单树形数据
 */
const menuTreeData = computed(() => {
  const menus = roleDetail.value?.menus || []
  if (!menus.length) return []

  // 创建菜单映射
  const menuMap = new Map<number, MenuInfo>()
  menus.forEach(menu => {
    menuMap.set(menu.menuId, { ...menu, children: [] })
  })

  // 构建树形结构
  const tree: MenuInfo[] = []
  menuMap.forEach(menu => {
    const parentId = menu.parentId
    if (!parentId || parentId === 0) {
      // 根节点
      tree.push(menu)
    } else {
      // 子节点
      const parent = menuMap.get(parentId)
      if (parent) {
        // 父节点在列表中，添加为子节点
        if (!parent.children) {
          parent.children = []
        }
        parent.children.push(menu)
      } else {
        // 父节点不在列表中，作为根节点显示
        tree.push(menu)
      }
    }
  })

  return tree
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const activeTab = ref('permissions')
const permissionSubTab = ref<'api' | 'data'>('api')
const roleDetail = ref<RoleDetailRsp | null>(null)

const { isSubmitting, submitGuard } = useSubmitGuard()

const form = reactive({
  roleId: undefined as number | undefined,
  roleName: '',
  roleEnName: '',
  roleCode: '',
  status: 0,
  roleType: 2,
})

const rules: FormRules = {
  roleName: [
    { required: true, message: t('common.pleaseInput') + t('role.roleName'), trigger: 'blur' },
  ],
  roleCode: [
    { required: true, message: t('common.pleaseInput') + t('role.roleCode'), trigger: 'blur' },
  ],
}

const fetchRoleDetail = async () => {
  if (!props.data?.roleId) return

  loading.value = true
  try {
    roleDetail.value = await getRoleDetail(props.data.roleId)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    await submitGuard(async () => {
      if (props.type === 'add') {
        await addRole({
          roleName: form.roleName,
          roleEnName: form.roleEnName || undefined,
          roleCode: form.roleCode,
          status: form.status,
          roleType: form.roleType,
        })
        ElMessage.success(t('message.success'))
      } else {
        await updateRole({
          roleId: form.roleId!,
          roleName: form.roleName,
          roleEnName: form.roleEnName || undefined,
          roleCode: form.roleCode,
          status: form.status,
          roleType: form.roleType,
        })
        ElMessage.success(t('message.success'))
      }
      visible.value = false
      emit('success')
    })
  })
}

const handleClose = () => {
  formRef.value?.resetFields()
  form.roleId = undefined
  form.roleName = ''
  form.roleEnName = ''
  form.roleCode = ''
  form.status = 0
  form.roleType = 2
  roleDetail.value = null
  activeTab.value = 'permissions'
  permissionSubTab.value = 'api'
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && props.type === 'edit' && props.data) {
      form.roleId = props.data.roleId
      form.roleName = props.data.roleName
      form.roleEnName = props.data.roleEnName || ''
      form.roleCode = props.data.roleCode
      form.status = props.data.status || 0
      form.roleType = props.data.roleType || 2
      fetchRoleDetail()
    }
  }
)
</script>

<style scoped lang="scss">
.role-form-container {
  .role-form {
    padding: 0 0 16px 0;
  }

  .detail-tabs {
    margin-top: 16px;

    .sub-tabs {
      :deep(.el-tabs__header) {
        margin-bottom: 12px;
      }

      :deep(.el-tabs__item) {
        font-size: 13px;
      }
    }
  }
}
</style>
