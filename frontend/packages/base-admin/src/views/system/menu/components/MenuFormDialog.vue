<template>
  <el-dialog
    :title="dialogTitle"
    v-model="visible"
    width="600px"
    :close-on-click-modal="false"
    :lock-scroll="false"
    @closed="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="menu-form">
      <el-form-item :label="t('menu.parentMenu')" prop="parentId">
        <el-tree-select
          v-model="form.parentId"
          :data="menuTreeData"
          :props="{ label: 'menuName', value: 'menuId', children: 'children' }"
          :placeholder="t('common.pleaseSelect')"
          clearable
          check-strictly
          :render-after-expand="false"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item :label="t('menu.menuName')" prop="menuName">
        <el-input v-model.trim="form.menuName" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item :label="t('menu.menuEnName')" prop="menuEnName">
        <el-input v-model.trim="form.menuEnName" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item :label="t('menu.type')" prop="type">
        <el-radio-group v-model="form.type">
          <el-radio :value="1">{{ t('menu.typeDirectory') }}</el-radio>
          <el-radio :value="2">{{ t('menu.typeMenu') }}</el-radio>
          <el-radio :value="3">{{ t('menu.typeButton') }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="form.type !== 3" :label="t('menu.icon')" prop="icon">
        <div class="icon-input-wrapper">
          <IconSelector
            v-model="form.icon"
            :placeholder="t('menu.selectIcon')"
            class="icon-selector"
          />
          <el-button
            v-if="form.icon"
            type="danger"
            link
            size="small"
            class="clear-btn"
            @click="form.icon = ''"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </el-form-item>

      <el-form-item v-if="form.type !== 3" :label="t('menu.url')" prop="url">
        <el-input v-model.trim="form.url" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item v-if="form.type === 2" :label="t('menu.componentPath')" prop="componentPath">
        <el-input v-model.trim="form.componentPath" :placeholder="t('common.pleaseInput')" />
      </el-form-item>

      <el-form-item :label="t('menu.orderNumber')" prop="orderNumber">
        <el-input-number v-model="form.orderNumber" :min="0" :max="999" />
      </el-form-item>

      <el-form-item :label="t('common.status')" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="0">{{ t('menu.statusShow') }}</el-radio>
          <el-radio :value="1">{{ t('menu.statusHide') }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 关联权限选择器（仅页面和按钮显示） -->
      <el-form-item v-if="form.type === 2 || form.type === 3" :label="t('menu.relatedPermission')">
        <div class="permission-select-wrapper">
          <el-input
            :model-value="selectedPermDisplay"
            :placeholder="t('common.pleaseSelect')"
            readonly
            style="flex: 1"
          />
          <el-button type="primary" @click="openPermDialog">
            {{ t('common.select') }}
          </el-button>
          <el-button v-if="form.permId" type="danger" link @click="clearPermSelection">
            {{ t('common.clear') }}
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
        {{ t('common.confirm') }}
      </el-button>
    </template>

    <!-- 权限选择弹窗 -->
    <PermissionSelectDialog
      v-model="permDialogVisible"
      :selected-id="form.permId"
      @confirm="handlePermConfirm"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  addMenu,
  updateMenu,
  getMenuList,
  checkMenuRoleAssignment,
  type MenuInfo,
  type PermissionInfo,
} from '@/api/menu'
import { IconSelector } from '@blink/components'
import PermissionSelectDialog from './PermissionSelectDialog.vue'
import { useSubmitGuard } from '@blink/components'

interface Props {
  modelValue: boolean
  type: 'add' | 'edit'
  data: MenuInfo | null
  parentMenu: MenuInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'success'])

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

const dialogTitle = computed(() => (props.type === 'add' ? t('menu.addMenu') : t('menu.editMenu')))

const formRef = ref<FormInstance>()
const { isSubmitting, submitGuard } = useSubmitGuard()
const menuTreeData = ref<MenuInfo[]>([])
const permDialogVisible = ref(false)
const selectedPerm = ref<PermissionInfo | null>(null)

const form = reactive({
  menuId: undefined as number | undefined,
  menuName: '',
  menuEnName: '',
  type: 1,
  icon: '',
  url: '',
  componentPath: '',
  orderNumber: 0,
  status: 0,
  parentId: undefined as number | undefined,
  permId: undefined as number | undefined,
})

// 已选权限显示文本
const selectedPermDisplay = computed(() => {
  if (selectedPerm.value) {
    return `${selectedPerm.value.acIdentity} - ${selectedPerm.value.acName}`
  }
  return ''
})

const rules: FormRules = {
  menuName: [
    { required: true, message: t('common.pleaseInput') + t('menu.menuName'), trigger: 'blur' },
  ],
  type: [{ required: true, message: t('common.pleaseSelect') + t('menu.type'), trigger: 'change' }],
  url: [
    {
      validator: (rule, value, callback) => {
        // 只有页面菜单(type=2)URL必填，目录(type=1)不需要URL
        if (form.type === 2 && !value?.trim()) {
          callback(new Error(t('common.pleaseInput') + t('menu.url')))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  componentPath: [
    {
      validator: (rule, value, callback) => {
        // 页面菜单组件路径必填
        if (form.type === 2 && !value?.trim()) {
          callback(new Error(t('common.pleaseInput') + t('menu.componentPath')))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const fetchMenuTree = async () => {
  const res = await getMenuList()
  // API 返回的是 { rows: [...] } 结构
  const menuList = res?.rows || []
  menuTreeData.value = [
    { menuId: 0, menuName: t('menu.rootMenu'), children: menuList },
  ] as MenuInfo[]
}

// 打开权限选择弹窗
const openPermDialog = () => {
  permDialogVisible.value = true
}

// 权限选择确认
const handlePermConfirm = (perm: PermissionInfo | null) => {
  if (perm) {
    selectedPerm.value = perm
    form.permId = perm.acId
  }
}

// 清除权限选择
const clearPermSelection = () => {
  selectedPerm.value = null
  form.permId = undefined
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    await submitGuard(async () => {
      if (props.type === 'add') {
        await addMenu({
          menuName: form.menuName,
          menuEnName: form.menuEnName || undefined,
          type: form.type,
          icon: form.icon || undefined,
          url: form.url || undefined,
          componentPath: form.componentPath || undefined,
          orderNumber: form.orderNumber,
          status: form.status,
          parentId: form.parentId || 0,
          permId: form.type === 2 || form.type === 3 ? form.permId : undefined,
        })
        ElMessage.success(t('message.success'))
      } else {
        // 编辑模式：检查权限是否变更
        const effectivePermId = form.type === 2 || form.type === 3 ? form.permId : undefined
        const originalPermId = props.data?.permId

        // 如果权限发生变更，先检查菜单是否已分配给角色
        if (effectivePermId !== originalPermId) {
          const checkResult = await checkMenuRoleAssignment({
            menuId: form.menuId!,
            newPermId: effectivePermId,
          })

          if (checkResult.assigned && checkResult.roles && checkResult.roles.length > 0) {
            const roleNames = checkResult.roles.map((r) => r.roleName).join('、')
            const confirmMsg = t('menu.permChangeConfirmWithRoles', { roles: roleNames })

            try {
              await ElMessageBox.confirm(confirmMsg, t('message.tips'), {
                type: 'warning',
                confirmButtonText: t('common.confirm'),
                cancelButtonText: t('common.cancel'),
              })
            } catch {
              // 用户取消操作
              return
            }
          }
        }

        await updateMenu({
          menuId: form.menuId!,
          menuName: form.menuName,
          menuEnName: form.menuEnName || undefined,
          type: form.type,
          icon: form.icon || undefined,
          url: form.url || undefined,
          componentPath: form.componentPath || undefined,
          orderNumber: form.orderNumber,
          status: form.status,
          parentId: form.parentId,
          permId: effectivePermId,
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
  form.menuId = undefined
  form.menuName = ''
  form.menuEnName = ''
  form.type = 1
  form.icon = ''
  form.url = ''
  form.componentPath = ''
  form.orderNumber = 0
  form.status = 0
  form.parentId = undefined
  form.permId = undefined
  selectedPerm.value = null
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      fetchMenuTree()
      if (props.type === 'edit' && props.data) {
        form.menuId = props.data.menuId
        form.menuName = props.data.menuName
        form.menuEnName = props.data.menuEnName || ''
        form.type = props.data.type
        form.icon = props.data.icon || ''
        form.url = props.data.url || ''
        form.componentPath = props.data.componentPath || ''
        form.orderNumber = props.data.orderNumber || 0
        form.status = props.data.status || 0
        form.parentId = props.data.parentId || undefined
        form.permId = props.data.permId || undefined
        // 回显关联权限信息
        if (props.data.permId && props.data.permIdentity && props.data.permName) {
          selectedPerm.value = {
            acId: props.data.permId,
            acIdentity: props.data.permIdentity,
            acName: props.data.permName,
            acType: 1,
            url: '',
          }
        } else {
          selectedPerm.value = null
        }
      } else if (props.type === 'add' && props.parentMenu) {
        form.parentId = props.parentMenu.menuId
        form.type = props.parentMenu.type === 1 ? 2 : 3
      }
    }
  }
)
</script>

<style scoped lang="scss">
.menu-form {
  padding: 20px 20px 0;

  .icon-input-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;

    .icon-selector {
      flex: 1;
    }

    .clear-btn {
      flex-shrink: 0;
    }
  }

  .permission-select-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
  }
}
</style>
