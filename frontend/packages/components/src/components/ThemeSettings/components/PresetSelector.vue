<!-- src/components/ThemeSettings/components/PresetSelector.vue -->
<template>
  <div class="preset-selector">
    <h4 class="section-title">{{ t('settings.presetThemes') }}</h4>
    <div class="preset-grid">
      <div
        v-for="preset in presets"
        :key="preset.id"
        class="preset-card"
        :class="{ active: currentPresetId === preset.id }"
        @click="handleSelect(preset.id)"
      >
        <div
          class="preset-color"
          :style="{ backgroundColor: preset.colors.primary }"
        />
        <span class="preset-name">{{ locale === 'zh_cn' ? preset.name : preset.nameEn }}</span>
      </div>
    </div>
    <div v-if="$slots.footer" class="preset-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { PresetTheme } from '../types'

defineOptions({
  name: 'PresetSelector',
})

interface Props {
  presets: PresetTheme[]
  currentPresetId?: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', presetId: string): void
}>()

const { t, locale } = useI18n()

const handleSelect = (presetId: string) => {
  emit('select', presetId)
}
</script>

<style scoped lang="scss">
.preset-selector {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-color-primary);
    margin-bottom: 16px;
  }

  .preset-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
    gap: 12px;

    .preset-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px;
      border: 2px solid var(--border-color-light);
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        border-color: var(--primary-color);
        transform: translateY(-2px);
      }

      &.active {
        border-color: var(--primary-color);
        background: var(--primary-color-light-9);
      }

      .preset-color {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        margin-bottom: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      }

      .preset-name {
        font-size: 12px;
        color: var(--text-color-regular);
      }
    }
  }

  .preset-footer {
    margin-top: 12px;
  }
}
</style>