<template>
  <el-popover
    ref="popoverRef"
    placement="bottom-start"
    :width="popoverWidth"
    trigger="click"
    popper-class="icon-selector-popover"
  >
    <template #reference>
      <div class="icon-selector-trigger" :class="{ 'is-empty': !modelValue }">
        <BlinkIcon v-if="modelValue" :icon="modelValue" :size="18" class="selected-icon" />
        <span v-else class="placeholder">{{ placeholder || t('iconSelector.placeholder') }}</span>
        <el-icon class="arrow-icon"><ArrowDown /></el-icon>
      </div>
    </template>

    <div class="icon-selector">
      <!-- 搜索框 -->
      <div v-if="searchable" class="search-box">
        <el-input
          v-model.trim="searchKeyword"
          :placeholder="searchPlaceholder || t('common.search')"
          clearable
          size="small"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 图标分组标签页 -->
      <div class="icon-tabs">
        <el-tabs v-model="activeTab" size="small">
          <el-tab-pane
            v-for="group in visibleGroups"
            :key="group.name"
            :label="group.label"
            :name="group.name"
          >
            <div class="icon-grid" :style="{ maxHeight: `${gridMaxHeight}px` }">
              <div
                v-for="icon in filteredIconsMap[group.name] || []"
                :key="icon"
                class="icon-item"
                :class="{ 'is-active': modelValue === icon }"
                :title="getIconShortName(icon)"
                @click="handleSelectIcon(icon)"
              >
                <BlinkIcon :icon="icon" :size="previewSize" class="icon-preview" />
                <span v-if="showIconName" class="icon-name">{{ getIconShortName(icon) }}</span>
              </div>
              <el-empty
                v-if="(filteredIconsMap[group.name] || []).length === 0"
                :description="noDataText || t('common.noData')"
                :image-size="60"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 最近使用 -->
      <div v-if="showRecent && recentIcons.length > 0" class="recent-section">
        <div class="recent-title">{{ t('iconSelector.recentlyUsed') }}</div>
        <div class="recent-icons">
          <div
            v-for="icon in recentIcons"
            :key="`recent-${icon}`"
            class="recent-icon-item"
            :class="{ 'is-active': modelValue === icon }"
            @click="handleSelectIcon(icon)"
          >
            <BlinkIcon :icon="icon" :size="18" />
          </div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script lang="ts">
/**
 * 图标分组接口
 */
export interface IconGroup {
  /** 分组名称（唯一标识） */
  name: string
  /** 分组显示标签 */
  label: string
  /** 图标列表 */
  icons: string[]
}

/**
 * Element Plus 图标列表（常用）
 */
export const DEFAULT_ELEMENT_ICONS = [
  'HomeFilled',
  'Home',
  'Setting',
  'User',
  'UserFilled',
  'Menu',
  'Key',
  'Monitor',
  'Edit',
  'Delete',
  'Plus',
  'Minus',
  'Check',
  'Close',
  'Search',
  'Refresh',
  'Folder',
  'FolderOpened',
  'Document',
  'DocumentCopy',
  'Files',
  'Bell',
  'BellFilled',
  'Message',
  'ChatDotRound',
  'ChatLineRound',
  'Star',
  'StarFilled',
  'Heart',
  'HeartFilled',
  'Calendar',
  'Clock',
  'Timer',
  'AlarmClock',
  'Location',
  'MapLocation',
  'Compass',
  'Guide',
  'Picture',
  'Camera',
  'VideoCamera',
  'Microphone',
  'Lock',
  'Unlock',
  'View',
  'Hide',
  'Eye',
  'EyeOpen',
  'Upload',
  'Download',
  'Link',
  'Disconnect',
  'Share',
  'ArrowUp',
  'ArrowDown',
  'ArrowLeft',
  'ArrowRight',
  'More',
  'MoreFilled',
  'Operation',
  'Tools',
  'MagicStick',
  'Warning',
  'WarningFilled',
  'CircleCheck',
  'CircleClose',
  'CirclePlus',
  'InfoFilled',
  'QuestionFilled',
  'SuccessFilled',
  'CircleCheckFilled',
  'Grid',
  'List',
  'Histogram',
  'DataLine',
  'DataBoard',
  'PieChart',
  'TrendCharts',
  'DataAnalysis',
  'Coin',
  'Money',
  'Wallet',
  'ShoppingCart',
  'Goods',
  'GoodsFilled',
  'ShoppingBag',
  'Shop',
  'Box',
  'Present',
  'Phone',
  'PhoneFilled',
  'Iphone',
  'Cellphone',
  'Mouse',
  'Cpu',
  'Connection',
  'Platform',
  'Notebook',
  'OfficeBuilding',
  'Avatar',
  'Stamp',
  'Medal',
  'Trophy',
  'TrendCharts',
]

/**
 * 导航类 MDI 图标
 */
export const NAVIGATION_ICONS = [
  'mdi:view-dashboard',
  'mdi:view-dashboard-outline',
  'mdi:menu',
  'mdi:menu-open',
  'mdi:sitemap',
  'mdi:folder-outline',
  'mdi:file-tree',
  'mdi:compass-outline',
  'mdi:map-marker-path',
  'mdi:table-large',
  'mdi:widgets-outline',
  'mdi:bookmark-outline',
  'mdi:bookmark-multiple-outline',
  'mdi:format-list-bulleted',
  'mdi:format-list-checkbox',
]

/**
 * 网关/API 类图标
 */
export const GATEWAY_ICONS = [
  'mdi:router-network',
  'mdi:router-wireless',
  'mdi:transit-connection-variant',
  'mdi:lan-connect',
  'mdi:source-branch',
  'mdi:shuffle-variant',
  'mdi:api',
  'mdi:api-off',
  'mdi:web',
  'mdi:cloud-outline',
  'mdi:server-network',
  'mdi:server-network-off',
  'mdi:network-outline',
  'mdi:network-pos',
  'mdi:signal',
  'mdi:signal-off',
  'mdi:trending-up',
  'mdi:trending-down',
]

/**
 * 常用 MDI 图标列表
 */
export const DEFAULT_COMMON_ICONS = [
  'mdi:home',
  'mdi:home-outline',
  'mdi:cog',
  'mdi:cog-outline',
  'mdi:account',
  'mdi:account-outline',
  'mdi:account-group',
  'mdi:account-group-outline',
  'mdi:file-document',
  'mdi:file-document-outline',
  'mdi:folder',
  'mdi:folder-outline',
  'mdi:plus',
  'mdi:minus',
  'mdi:close',
  'mdi:check',
  'mdi:delete',
  'mdi:delete-outline',
  'mdi:pencil',
  'mdi:pencil-outline',
  'mdi:refresh',
  'mdi:magnify',
  'mdi:upload',
  'mdi:download',
  'mdi:link',
  'mdi:link-variant',
  'mdi:star',
  'mdi:star-outline',
  'mdi:heart',
  'mdi:heart-outline',
  'mdi:bell',
  'mdi:bell-outline',
  'mdi:email',
  'mdi:email-outline',
  'mdi:calendar',
  'mdi:calendar-outline',
  'mdi:clock',
  'mdi:clock-outline',
  'mdi:map-marker',
  'mdi:map-marker-outline',
  'mdi:image',
  'mdi:image-outline',
  'mdi:lock',
  'mdi:lock-outline',
  'mdi:key',
  'mdi:eye',
  'mdi:eye-outline',
  'mdi:alert',
  'mdi:alert-outline',
  'mdi:check-circle',
  'mdi:check-circle-outline',
  'mdi:filter',
  'mdi:filter-outline',
]

/**
 * 系统 MDI 图标列表
 */
export const DEFAULT_SYSTEM_ICONS = [
  'mdi:monitor',
  'mdi:cellphone',
  'mdi:phone',
  'mdi:phone-outline',
  'mdi:mouse',
  'mdi:cpu-64-bit',
  'mdi:chart-line',
  'mdi:chart-bar',
  'mdi:chart-pie',
  'mdi:database',
  'mdi:database-outline',
  'mdi:server',
  'mdi:server-outline',
  'mdi:cloud',
  'mdi:cloud-outline',
  'mdi:shield',
  'mdi:shield-outline',
  'mdi:security',
  'mdi:lock-alert',
  'mdi:alert-circle',
  'mdi:alert-circle-outline',
  'mdi:warning',
  'mdi:information',
  'mdi:power',
  'mdi:power-plug',
  'mdi:power-plug-off',
  'mdi:cog',
  'mdi:cog-outline',
  'mdi:wrench',
  'mdi:wrench-outline',
  'mdi:code-tags',
  'mdi:code-braces',
  'mdi:xml',
  'mdi:source-commit',
]

/**
 * 媒体 MDI 图标列表
 */
export const DEFAULT_MEDIA_ICONS = [
  'mdi:camera',
  'mdi:camera-outline',
  'mdi:video',
  'mdi:video-outline',
  'mdi:microphone',
  'mdi:microphone-outline',
  'mdi:headphones',
  'mdi:volume-high',
  'mdi:play',
  'mdi:pause',
  'mdi:stop',
  'mdi:skip-next',
  'mdi:skip-previous',
  'mdi:image',
  'mdi:image-outline',
  'mdi:file-image',
  'mdi:file-video',
  'mdi:music',
  'mdi:music-note',
  'mdi:playlist-music',
  'mdi:message',
  'mdi:message-outline',
  'mdi:chat',
  'mdi:chat-outline',
  'mdi:comment',
  'mdi:comment-outline',
  'mdi:send',
  'mdi:send-outline',
]

/**
 * 生活 MDI 图标列表
 */
export const DEFAULT_LIFESTYLE_ICONS = [
  'mdi:weather-sunny',
  'mdi:weather-night',
  'mdi:weather-cloudy',
  'mdi:weather-rainy',
  'mdi:weather-snowy',
  'mdi:weather-windy',
  'mdi:thermometer',
  'mdi:food',
  'mdi:food-apple',
  'mdi:coffee',
  'mdi:tea',
  'mdi:car',
  'mdi:car-outline',
  'mdi:bike',
  'mdi:walk',
  'mdi:run',
  'mdi:heart',
  'mdi:heart-outline',
  'mdi:gift',
  'mdi:gift-outline',
]

/**
 * 商业 MDI 图标列表
 */
export const DEFAULT_BUSINESS_ICONS = [
  'mdi:cart',
  'mdi:cart-outline',
  'mdi:shopping',
  'mdi:shopping-outline',
  'mdi:store',
  'mdi:store-outline',
  'mdi:briefcase',
  'mdi:briefcase-outline',
  'mdi:currency-usd',
  'mdi:currency-eur',
  'mdi:currency-cny',
  'mdi:credit-card',
  'mdi:credit-card-outline',
  'mdi:wallet',
  'mdi:wallet-outline',
  'mdi:bank',
  'mdi:bank-outline',
  'mdi:cash',
  'mdi:cash-multiple',
  'mdi:chart-line',
  'mdi:chart-bar',
  'mdi:trending-up',
  'mdi:trending-down',
  'mdi:trophy',
  'mdi:trophy-outline',
  'mdi:medal',
  'mdi:package-variant',
]

/**
 * 图标选择器 Props 接口
 */
export interface Props {
  /** 当前选中的图标 */
  modelValue?: string
  /** 占位文本 */
  placeholder?: string
  /** 自定义图标分组 */
  groups?: IconGroup[]
  /** 弹出层宽度 */
  popoverWidth?: number
  /** 图标网格最大高度 */
  gridMaxHeight?: number
  /** 默认激活的标签页 */
  defaultTab?: string
  /** 是否可搜索 */
  searchable?: boolean
  /** 搜索框占位文本 */
  searchPlaceholder?: string
  /** 无数据提示文本 */
  noDataText?: string
  /** 图标预览大小 */
  previewSize?: number
  /** 是否显示图标名称 */
  showIconName?: boolean
  /** 是否显示最近使用 */
  showRecent?: boolean
  /** 最近使用图标数量上限 */
  maxRecent?: number
  /** 最近使用存储键（用于持久化） */
  recentStorageKey?: string
}

/**
 * 标签 key 映射（用于 i18n）
 */
export const LABEL_KEY_MAP: Record<string, string> = {
  element: 'iconSelector.elementIcons',
  common: 'iconSelector.commonIcons',
  system: 'iconSelector.systemIcons',
  media: 'iconSelector.mediaIcons',
  lifestyle: 'iconSelector.lifestyleIcons',
  business: 'iconSelector.businessIcons',
  navigation: 'iconSelector.navigationIcons',
  gateway: 'iconSelector.gatewayIcons',
}

/**
 * 创建默认图标分组
 */
export const createDefaultGroups = (): IconGroup[] => [
  { name: 'element', label: 'Element Icons', icons: DEFAULT_ELEMENT_ICONS },
  { name: 'common', label: 'Common Icons', icons: DEFAULT_COMMON_ICONS },
  { name: 'system', label: 'System Icons', icons: DEFAULT_SYSTEM_ICONS },
  { name: 'media', label: 'Media Icons', icons: DEFAULT_MEDIA_ICONS },
  { name: 'lifestyle', label: 'Lifestyle Icons', icons: DEFAULT_LIFESTYLE_ICONS },
  { name: 'business', label: 'Business Icons', icons: DEFAULT_BUSINESS_ICONS },
]

/**
 * 创建菜单图标分组（适用于后台管理系统）
 */
export const createMenuIconGroups = (): IconGroup[] => [
  {
    name: 'navigation',
    label: 'Navigation',
    icons: NAVIGATION_ICONS,
  },
  {
    name: 'gateway',
    label: 'Gateway',
    icons: GATEWAY_ICONS,
  },
  {
    name: 'element',
    label: 'Element Icons',
    icons: [
      'HomeFilled',
      'Home',
      'Setting',
      'User',
      'UserFilled',
      'Menu',
      'Key',
      'Monitor',
      'Tools',
      'Operation',
      'Document',
      'Folder',
      'FolderOpened',
      'Files',
      'Lock',
      'Unlock',
      'View',
      'Connection',
      'Platform',
      'OfficeBuilding',
    ],
  },
  {
    name: 'common',
    label: 'Common',
    icons: [
      'mdi:cog-outline',
      'mdi:shield-outline',
      'mdi:database-outline',
      'mdi:account-group-outline',
      'mdi:chart-line',
      'mdi:bell-outline',
      'mdi:check-circle-outline',
      'mdi:alert-circle-outline',
      'mdi:flash-outline',
    ],
  },
]
</script>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown, Search } from '@element-plus/icons-vue'
import BlinkIcon from '../BlinkIcon/index.vue'

const { t } = useI18n()

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '',
  groups: () => createDefaultGroups(),
  popoverWidth: 450,
  gridMaxHeight: 320,
  defaultTab: '',
  searchable: true,
  searchPlaceholder: '',
  noDataText: '',
  previewSize: 20,
  showIconName: true,
  showRecent: true,
  maxRecent: 12,
  recentStorageKey: 'blink-icon-recent',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}>()

const popoverRef = ref()
const searchKeyword = ref('')
const activeTab = ref('')
const recentIcons = ref<string[]>([])

// 加载最近使用的图标
const loadRecentIcons = () => {
  if (props.showRecent && props.recentStorageKey) {
    try {
      const stored = localStorage.getItem(props.recentStorageKey)
      if (stored) {
        recentIcons.value = JSON.parse(stored).slice(0, props.maxRecent)
      }
    } catch {
      recentIcons.value = []
    }
  }
}

// 保存最近使用的图标
const saveRecentIcon = (icon: string) => {
  if (!props.showRecent || !props.recentStorageKey) return

  // 移除已存在的，添加到头部
  const icons = recentIcons.value.filter((i) => i !== icon)
  icons.unshift(icon)
  recentIcons.value = icons.slice(0, props.maxRecent)

  try {
    localStorage.setItem(props.recentStorageKey, JSON.stringify(recentIcons.value))
  } catch {
    // ignore
  }
}

// 初始化加载
loadRecentIcons()

const visibleGroups = computed(() => {
  return props.groups
    .filter((group) => group.icons.length > 0)
    .map((group) => {
      const labelKey = LABEL_KEY_MAP[group.name]
      return {
        ...group,
        label: labelKey ? t(labelKey) : group.label,
      }
    })
})

const filteredIconsMap = computed<Record<string, string[]>>(() => {
  const keyword = searchKeyword.value.toLowerCase()
  return visibleGroups.value.reduce<Record<string, string[]>>((result, group) => {
    result[group.name] = keyword
      ? group.icons.filter((icon) => icon.toLowerCase().includes(keyword))
      : group.icons
    return result
  }, {})
})

const getInitialTab = () => props.defaultTab || visibleGroups.value[0]?.name || ''

watch(
  () => [props.defaultTab, props.groups],
  () => {
    if (!visibleGroups.value.some((group) => group.name === activeTab.value)) {
      activeTab.value = getInitialTab()
    }
  },
  { immediate: true, deep: true }
)

const getIconShortName = (icon: string): string => {
  const parts = icon.split(':')
  return parts[1] || icon
}

const handleSelectIcon = (icon: string) => {
  emit('update:modelValue', icon)
  emit('change', icon)
  saveRecentIcon(icon)
  popoverRef.value?.hide()
}
</script>

<style scoped lang="scss">
.icon-selector-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 32px;
  padding: 0 11px;
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--primary-color);
  }

  &.is-empty {
    .arrow-icon {
      margin-left: auto;
    }
  }

  .selected-icon {
    color: var(--primary-color);
  }

  .placeholder {
    color: var(--text-color-placeholder);
    font-size: 14px;
  }

  .arrow-icon {
    color: var(--text-color-secondary);
    font-size: 12px;
    margin-left: 8px;
  }
}

.icon-selector {
  .search-box {
    margin-bottom: 12px;
  }

  .icon-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 12px;
    }

    :deep(.el-tabs__item) {
      font-size: 13px;
      color: var(--text-color-regular);

      &.is-active,
      &:hover {
        color: var(--primary-color);
      }
    }

    :deep(.el-tabs__nav-wrap::after) {
      background-color: var(--border-color-light);
    }
  }
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
  gap: 8px;
  overflow-y: auto;
  padding: 4px 2px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 6px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out-expo);

  &:hover,
  &.is-active {
    border-color: var(--primary-color);
    background: rgba(59, 130, 246, 0.08);
  }

  &.is-active {
    .icon-preview {
      color: var(--primary-color);
    }
  }
}

.icon-preview {
  color: var(--text-color-primary);
}

.icon-name {
  font-size: 11px;
  color: var(--text-color-secondary);
  text-align: center;
  word-break: break-word;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color-light);

  .recent-title {
    font-size: 12px;
    color: var(--text-color-secondary);
    margin-bottom: 8px;
  }

  .recent-icons {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .recent-icon-item {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: 1px solid var(--border-color-light);
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover,
    &.is-active {
      border-color: var(--primary-color);
      background: rgba(59, 130, 246, 0.08);
      color: var(--primary-color);
    }
  }
}
</style>
