<template>
  <el-popover
    ref="popoverRef"
    placement="bottom-start"
    :width="popoverWidth"
    trigger="click"
    popper-class="icon-selector-popover"
  >
    <template #reference>
      <div class="icon-selector-trigger">
        <BlinkIcon v-if="modelValue" :icon="modelValue" size="18" class="selected-icon" />
        <span v-else class="placeholder">{{ placeholder || t('iconSelector.placeholder') }}</span>
        <el-icon class="arrow-icon"><ArrowDown /></el-icon>
      </div>
    </template>

    <div class="icon-selector">
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

      <div class="icon-tabs">
        <el-tabs v-model="activeTab" size="small">
          <el-tab-pane
            v-for="group in visibleGroups"
            :key="group.name"
            :label="group.label"
            :name="group.name"
          >
            <div class="icon-grid">
              <div
                v-for="icon in filteredIconsMap[group.name] || []"
                :key="icon"
                class="icon-item"
                :class="{ 'is-active': modelValue === icon }"
                @click="handleSelectIcon(icon)"
              >
                <BlinkIcon :icon="icon" size="20" class="icon-preview" />
                <span class="icon-name">{{ getIconShortName(icon) }}</span>
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
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown, Search } from '@element-plus/icons-vue'
import BlinkIcon from '../BlinkIcon/index.vue'

export interface IconGroup {
  name: string
  label: string
  icons: string[]
}

const { t } = useI18n()

const DEFAULT_ELEMENT_ICONS = [
  'HomeFilled', 'Home', 'Setting', 'User', 'UserFilled', 'Menu', 'Key', 'Monitor',
  'Edit', 'Delete', 'Plus', 'Minus', 'Check', 'Close', 'Search', 'Refresh',
  'Folder', 'FolderOpened', 'Document', 'DocumentCopy', 'Files',
  'Bell', 'BellFilled', 'Message', 'ChatDotRound', 'ChatLineRound',
  'Star', 'StarFilled', 'Heart', 'HeartFilled',
  'Calendar', 'Clock', 'Timer', 'AlarmClock',
  'Location', 'MapLocation', 'Compass', 'Guide',
  'Picture', 'Camera', 'VideoCamera', 'Microphone',
  'Lock', 'Unlock', 'View', 'Hide', 'Eye', 'EyeOpen',
  'Upload', 'Download', 'Link', 'Disconnect', 'Share',
  'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight',
  'More', 'MoreFilled', 'Operation', 'Tools', 'MagicStick',
  'Warning', 'WarningFilled', 'CircleCheck', 'CircleClose', 'CirclePlus',
  'InfoFilled', 'QuestionFilled', 'SuccessFilled', 'CircleCheckFilled',
  'Grid', 'List', 'Histogram', 'DataLine', 'DataBoard', 'PieChart',
  'TrendCharts', 'DataAnalysis', 'Coin', 'Money', 'Wallet', 'ShoppingCart',
  'Goods', 'GoodsFilled', 'ShoppingBag', 'Shop', 'Box', 'Present',
  'Phone', 'PhoneFilled', 'Iphone', 'Cellphone', 'Mouse',
  'Cpu', 'Connection', 'Platform', 'Notebook',
]

const DEFAULT_COMMON_ICONS = [
  'mdi:home', 'mdi:home-outline', 'mdi:cog', 'mdi:cog-outline', 'mdi:account', 'mdi:account-outline',
  'mdi:account-group', 'mdi:account-group-outline', 'mdi:file-document', 'mdi:file-document-outline',
  'mdi:folder', 'mdi:folder-outline', 'mdi:plus', 'mdi:minus', 'mdi:close', 'mdi:check',
  'mdi:delete', 'mdi:delete-outline', 'mdi:pencil', 'mdi:pencil-outline', 'mdi:refresh',
  'mdi:magnify', 'mdi:upload', 'mdi:download', 'mdi:link', 'mdi:link-variant',
  'mdi:star', 'mdi:star-outline', 'mdi:heart', 'mdi:heart-outline',
  'mdi:bell', 'mdi:bell-outline', 'mdi:email', 'mdi:email-outline',
  'mdi:calendar', 'mdi:calendar-outline', 'mdi:clock', 'mdi:clock-outline',
  'mdi:map-marker', 'mdi:map-marker-outline', 'mdi:image', 'mdi:image-outline',
  'mdi:lock', 'mdi:lock-outline', 'mdi:key', 'mdi:eye', 'mdi:eye-outline',
  'mdi:alert', 'mdi:alert-outline', 'mdi:check-circle', 'mdi:check-circle-outline',
  'mdi:information', 'mdi:information-outline', 'mdi:help-circle', 'mdi:help-circle-outline',
  'mdi:filter', 'mdi:filter-outline', 'mdi:sort', 'mdi:fullscreen', 'mdi:fullscreen-exit',
]

const DEFAULT_SYSTEM_ICONS = [
  'mdi:monitor', 'mdi:cellphone', 'mdi:phone', 'mdi:phone-outline',
  'mdi:mouse', 'mdi:cpu', 'mdi:chart-line', 'mdi:chart-bar', 'mdi:chart-pie',
  'mdi:database', 'mdi:server', 'mdi:cloud', 'mdi:cloud-outline',
  'mdi:shield', 'mdi:shield-outline', 'mdi:security', 'mdi:lock',
  'mdi:alert-circle', 'mdi:alert-circle-outline', 'mdi:warning', 'mdi:information',
  'mdi:power', 'mdi:power-plug', 'mdi:power-plug-off',
  'mdi:cog', 'mdi:cog-outline', 'mdi:wrench', 'mdi:wrench-outline',
  'mdi:code-tags', 'mdi:code-braces', 'mdi:xml',
]

const DEFAULT_MEDIA_ICONS = [
  'mdi:camera', 'mdi:camera-outline', 'mdi:video', 'mdi:video-outline',
  'mdi:microphone', 'mdi:microphone-outline', 'mdi:headphones', 'mdi:volume-high',
  'mdi:play', 'mdi:pause', 'mdi:stop', 'mdi:skip-next', 'mdi:skip-previous',
  'mdi:image', 'mdi:image-outline', 'mdi:file-image', 'mdi:file-video',
  'mdi:music', 'mdi:music-note', 'mdi:playlist-music',
  'mdi:message', 'mdi:message-outline', 'mdi:chat', 'mdi:chat-outline',
  'mdi:comment', 'mdi:comment-outline', 'mdi:send', 'mdi:send-outline',
]

const DEFAULT_LIFESTYLE_ICONS = [
  'mdi:weather-sunny', 'mdi:weather-night', 'mdi:weather-cloudy', 'mdi:weather-rainy',
  'mdi:weather-snowy', 'mdi:weather-windy', 'mdi:thermometer',
  'mdi:food', 'mdi:food-apple', 'mdi:coffee', 'mdi:tea', 'mdi:cup',
  'mdi:cake', 'mdi:ice-cream', 'mdi:candy',
  'mdi:car', 'mdi:car-outline', 'mdi:bike', 'mdi:walk',
  'mdi:run', 'mdi:swim', 'mdi:bike-fast',
  'mdi:heart', 'mdi:heart-outline', 'mdi:gift', 'mdi:gift-outline',
  'mdi:party-popper', 'mdi:firework',
]

const DEFAULT_BUSINESS_ICONS = [
  'mdi:cart', 'mdi:cart-outline', 'mdi:shopping', 'mdi:shopping-outline',
  'mdi:store', 'mdi:store-outline', 'mdi:briefcase', 'mdi:briefcase-outline',
  'mdi:currency-usd', 'mdi:currency-eur', 'mdi:currency-cny',
  'mdi:credit-card', 'mdi:credit-card-outline', 'mdi:wallet', 'mdi:wallet-outline',
  'mdi:bank', 'mdi:bank-outline', 'mdi:cash', 'mdi:cash-multiple',
  'mdi:chart-line', 'mdi:chart-bar', 'mdi:trending-up', 'mdi:trending-down',
  'mdi:trophy', 'mdi:trophy-outline', 'mdi:medal', 'mdi:ribbon',
  'mdi:package', 'mdi:package-variant', 'mdi:truck', 'mdi:truck-outline',
]

const createDefaultGroups = (): IconGroup[] => [
  { name: 'element', label: t('iconSelector.elementIcons'), icons: DEFAULT_ELEMENT_ICONS },
  { name: 'common', label: t('iconSelector.commonIcons'), icons: DEFAULT_COMMON_ICONS },
  { name: 'system', label: t('iconSelector.systemIcons'), icons: DEFAULT_SYSTEM_ICONS },
  { name: 'media', label: t('iconSelector.mediaIcons'), icons: DEFAULT_MEDIA_ICONS },
  { name: 'lifestyle', label: t('iconSelector.lifestyleIcons'), icons: DEFAULT_LIFESTYLE_ICONS },
  { name: 'business', label: t('iconSelector.businessIcons'), icons: DEFAULT_BUSINESS_ICONS },
]

interface Props {
  modelValue?: string
  placeholder?: string
  groups?: IconGroup[]
  popoverWidth?: number
  defaultTab?: string
  searchable?: boolean
  searchPlaceholder?: string
  noDataText?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '',
  groups: () => createDefaultGroups(),
  popoverWidth: 450,
  defaultTab: '',
  searchable: true,
  searchPlaceholder: '',
  noDataText: '',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const popoverRef = ref()
const searchKeyword = ref('')
const activeTab = ref('')

const visibleGroups = computed(() => props.groups.filter(group => group.icons.length > 0))

const filteredIconsMap = computed<Record<string, string[]>>(() => {
  const keyword = searchKeyword.value.toLowerCase()
  return visibleGroups.value.reduce<Record<string, string[]>>((result, group) => {
    result[group.name] = keyword
      ? group.icons.filter(icon => icon.toLowerCase().includes(keyword))
      : group.icons
    return result
  }, {})
})

const getInitialTab = () => props.defaultTab || visibleGroups.value[0]?.name || ''

watch(
  () => [props.defaultTab, props.groups],
  () => {
    if (!visibleGroups.value.some(group => group.name === activeTab.value)) {
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

  .selected-icon {
    font-size: 18px;
    color: var(--primary-color);
  }

  .placeholder {
    color: var(--text-color-placeholder);
    font-size: 14px;
  }

  .arrow-icon {
    color: var(--text-color-secondary);
    font-size: 12px;
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
  grid-template-columns: repeat(auto-fill, minmax(84px, 1fr));
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
  padding: 4px 2px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 8px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out-expo);

  &:hover,
  &.is-active {
    border-color: var(--primary-color);
    background: rgba(59, 130, 246, 0.08);
  }
}

.icon-preview {
  color: var(--text-color-primary);
}

.icon-name {
  font-size: 12px;
  color: var(--text-color-secondary);
  text-align: center;
  word-break: break-word;
}
</style>
