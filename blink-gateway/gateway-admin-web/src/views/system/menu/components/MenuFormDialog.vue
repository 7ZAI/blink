<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="600px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
    @close="resetForm"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item :label="t('menu.parentMenu')" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
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
        <el-input v-model="formData.menuName" :placeholder="t('common.pleaseInput') + t('menu.menuName')" />
      </el-form-item>
      <el-form-item :label="t('menu.menuEnName')" prop="menuEnName">
        <el-input v-model="formData.menuEnName" :placeholder="t('common.pleaseInput') + t('menu.menuEnName')" />
      </el-form-item>
      <el-form-item :label="t('menu.type')" prop="type">
        <el-radio-group v-model="formData.type">
          <el-radio :label="1">{{ t('menu.typeDirectory') }}</el-radio>
          <el-radio :label="2">{{ t('menu.typeMenu') }}</el-radio>
          <el-radio :label="3">{{ t('menu.typeButton') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="formData.type !== 3" :label="t('menu.icon')" prop="icon">
        <div class="icon-input-wrapper">
          <IconSelector
            v-model="formData.icon"
            :placeholder="t('iconSelector.placeholder')"
            class="icon-selector"
          />
          <el-button
            v-if="formData.icon"
            type="danger"
            link
            size="small"
            class="clear-btn"
            @click="formData.icon = ''"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </el-form-item>
      <el-form-item v-if="formData.type !== 3" :label="t('menu.url')" prop="url">
        <el-input v-model="formData.url" placeholder="/system/user" />
      </el-form-item>
      <el-form-item v-if="formData.type === 2" :label="t('menu.componentPath')" prop="componentPath">
        <el-input v-model="formData.componentPath" placeholder="system/user/index" />
      </el-form-item>
      <el-form-item v-if="formData.type === 3" :label="t('menu.permIdentity')" prop="permIdentity">
        <el-input v-model="formData.permIdentity" placeholder="system:user:add" />
      </el-form-item>
      <el-form-item :label="t('menu.orderNumber')" prop="orderNumber">
        <el-input-number v-model="formData.orderNumber" :min="0" />
      </el-form-item>
      <el-form-item :label="t('common.status')">
        <el-radio-group v-model="formData.status">
          <el-radio :label="0">{{ t('menu.statusShow') }}</el-radio>
          <el-radio :label="1">{{ t('menu.statusHide') }}</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { getMenuList, addMenu, updateMenu, type MenuInfo } from '@/api/menu'
import IconSelector from '@/components/IconSelector/index.vue'

const props = defineProps<{
  modelValue: boolean
  type: 'add' | 'edit'
  data: MenuInfo | null
  parentMenu: MenuInfo | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': [parentId?: number]
}>()

const { t } = useI18n()

const formRef = ref()
const submitting = ref(false)
const menuTreeData = ref<MenuInfo[]>([])

const formData = reactive({
  menuId: undefined as number | undefined,
  menuName: '',
  menuEnName: '',
  parentId: 0,
  type: 2,
  icon: '',
  url: '',
  componentPath: '',
  permIdentity: '',
  orderNumber: 0,
  status: 0
})

const dialogTitle = computed(() =>
  props.type === 'add' ? t('menu.addChild') : t('common.edit')
)

const formRules = {
  menuName: [{ required: true, message: t('common.pleaseInput') + t('menu.menuName'), trigger: 'blur' }],
  type: [{ required: true, message: t('common.pleaseSelect') + t('menu.type'), trigger: 'change' }]
}

// 获取菜单树数据
const fetchMenuTree = async () => {
  try {
    const res = await getMenuList()
    const menuList = res?.rows || []
    // 添加根目录选项
    menuTreeData.value = [{ menuId: 0, menuName: t('menu.rootMenu'), children: menuList }] as MenuInfo[]
  } catch (error) {
    console.error('Fetch menu tree error:', error)
    menuTreeData.value = [{ menuId: 0, menuName: t('menu.rootMenu'), children: [] }] as MenuInfo[]
  }
}

const findMenuById = (menus: MenuInfo[], menuId: number): MenuInfo | null => {
  for (const menu of menus) {
    if (menu.menuId === menuId) return menu
    if (menu.children) {
      const found = findMenuById(menu.children, menuId)
      if (found) return found
    }
  }
  return null
}

const loadMenuDetail = async () => {
  if (props.data?.menuId) {
    try {
      const res = await getMenuList()
      const menu = findMenuById(res.rows || [], props.data.menuId)
      if (menu) {
        formData.menuId = menu.menuId
        formData.menuName = menu.menuName
        formData.menuEnName = menu.menuEnName || ''
        formData.parentId = menu.parentId || 0
        formData.type = menu.type
        formData.url = menu.url || ''
        formData.componentPath = menu.componentPath || ''
        formData.permIdentity = menu.permIdentity || ''
        formData.icon = menu.icon || ''
        formData.orderNumber = menu.orderNumber || 0
        formData.status = menu.status
      }
    } catch (error) {
      console.error('Load menu detail error:', error)
    }
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  formData.menuId = undefined
  formData.menuName = ''
  formData.menuEnName = ''
  formData.parentId = 0
  formData.type = 2
  formData.url = ''
  formData.componentPath = ''
  formData.permIdentity = ''
  formData.icon = ''
  formData.orderNumber = 0
  formData.status = 0
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    // 记录父菜单ID，用于刷新后展开
    const savedParentId = formData.parentId || 0

    if (props.type === 'add') {
      await addMenu({
        menuName: formData.menuName,
        menuEnName: formData.menuEnName || undefined,
        parentId: formData.parentId || 0,
        type: formData.type,
        url: formData.url || undefined,
        componentPath: formData.componentPath || undefined,
        permIdentity: formData.permIdentity || undefined,
        icon: formData.icon || undefined,
        orderNumber: formData.orderNumber,
        status: formData.status
      })
    } else if (formData.menuId) {
      await updateMenu({
        menuId: formData.menuId,
        menuName: formData.menuName,
        menuEnName: formData.menuEnName || undefined,
        parentId: formData.parentId,
        type: formData.type,
        url: formData.url || undefined,
        componentPath: formData.componentPath || undefined,
        permIdentity: formData.permIdentity || undefined,
        icon: formData.icon || undefined,
        orderNumber: formData.orderNumber,
        status: formData.status
      })
    }

    ElMessage.success(t('common.success'))
    emit('update:modelValue', false)
    emit('success', savedParentId)
  } catch (error) {
    console.error('Submit error:', error)
  } finally {
    submitting.value = false
  }
}

watch(() => props.modelValue, (val) => {
  if (val) {
    fetchMenuTree()
    if (props.type === 'edit' && props.data) {
      loadMenuDetail()
    } else if (props.type === 'add') {
      resetForm()
      // 如果有父菜单，设置 parentId
      if (props.parentMenu) {
        formData.parentId = props.parentMenu.menuId
        // 根据父菜单类型设置默认类型
        formData.type = props.parentMenu.type === 1 ? 2 : 3
      }
    }
  }
})
</script>

<style scoped lang="scss">
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
</style>