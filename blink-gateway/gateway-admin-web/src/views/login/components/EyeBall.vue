<template>
  <div
    ref="eyeRef"
    class="eyeball"
    :style="eyeStyle"
  >
    <div
      v-if="!isBlinking"
      class="pupil"
      :style="pupilStyle"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  size?: number
  pupilSize?: number
  maxDistance?: number
  eyeColor?: string
  pupilColor?: string
  isBlinking?: boolean
  forceLookX?: number
  forceLookY?: number
}

const props = withDefaults(defineProps<Props>(), {
  size: 48,
  pupilSize: 16,
  maxDistance: 10,
  eyeColor: 'white',
  pupilColor: 'black',
  isBlinking: false
})

const mouseX = ref(0)
const mouseY = ref(0)
const eyeRef = ref<HTMLElement | null>(null)

const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

const pupilPosition = computed(() => {
  if (!eyeRef.value) return { x: 0, y: 0 }

  // If forced look direction is provided, use that instead of mouse tracking
  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return { x: props.forceLookX, y: props.forceLookY }
  }

  const eye = eyeRef.value.getBoundingClientRect()
  const eyeCenterX = eye.left + eye.width / 2
  const eyeCenterY = eye.top + eye.height / 2

  const deltaX = mouseX.value - eyeCenterX
  const deltaY = mouseY.value - eyeCenterY
  const distance = Math.min(Math.sqrt(deltaX ** 2 + deltaY ** 2), props.maxDistance)

  const angle = Math.atan2(deltaY, deltaX)
  const x = Math.cos(angle) * distance
  const y = Math.sin(angle) * distance

  return { x, y }
})

const eyeStyle = computed(() => ({
  width: `${props.size}px`,
  height: props.isBlinking ? '2px' : `${props.size}px`,
  backgroundColor: props.eyeColor
}))

const pupilStyle = computed(() => ({
  width: `${props.pupilSize}px`,
  height: `${props.pupilSize}px`,
  backgroundColor: props.pupilColor,
  transform: `translate(${pupilPosition.value.x}px, ${pupilPosition.value.y}px)`,
  transition: 'transform 0.1s ease-out'
}))

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<style scoped lang="scss">
.eyeball {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: height 0.15s ease-out;
}

.pupil {
  border-radius: 50%;
}
</style>