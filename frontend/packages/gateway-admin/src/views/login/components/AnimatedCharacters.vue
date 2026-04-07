<template>
  <div class="animated-characters" ref="containerRef">
    <!-- Purple tall rectangle character - Back layer -->
    <div
      ref="purpleRef"
      class="character character-purple"
      :style="purpleStyle"
    >
      <!-- Eyes -->
      <div class="eyes" :style="purpleEyesStyle">
        <EyeBall
          :size="18"
          :pupilSize="7"
          :maxDistance="5"
          eyeColor="white"
          pupilColor="#2D2D2D"
          :isBlinking="isPurpleBlinking"
          :forceLookX="purpleForceLookX"
          :forceLookY="purpleForceLookY"
        />
        <EyeBall
          :size="18"
          :pupilSize="7"
          :maxDistance="5"
          eyeColor="white"
          pupilColor="#2D2D2D"
          :isBlinking="isPurpleBlinking"
          :forceLookX="purpleForceLookX"
          :forceLookY="purpleForceLookY"
        />
      </div>
    </div>

    <!-- Black tall rectangle character - Middle layer -->
    <div
      ref="blackRef"
      class="character character-black"
      :style="blackStyle"
    >
      <!-- Eyes -->
      <div class="eyes" :style="blackEyesStyle">
        <EyeBall
          :size="16"
          :pupilSize="6"
          :maxDistance="4"
          eyeColor="white"
          pupilColor="#2D2D2D"
          :isBlinking="isBlackBlinking"
          :forceLookX="blackForceLookX"
          :forceLookY="blackForceLookY"
        />
        <EyeBall
          :size="16"
          :pupilSize="6"
          :maxDistance="4"
          eyeColor="white"
          pupilColor="#2D2D2D"
          :isBlinking="isBlackBlinking"
          :forceLookX="blackForceLookX"
          :forceLookY="blackForceLookY"
        />
      </div>
    </div>

    <!-- Orange semi-circle character - Front left -->
    <div
      ref="orangeRef"
      class="character character-orange"
      :style="orangeStyle"
    >
      <!-- Eyes - just pupils, no white -->
      <div class="eyes" :style="orangeEyesStyle">
        <Pupil
          :size="12"
          :maxDistance="5"
          pupilColor="#2D2D2D"
          :forceLookX="orangeForceLookX"
          :forceLookY="orangeForceLookY"
        />
        <Pupil
          :size="12"
          :maxDistance="5"
          pupilColor="#2D2D2D"
          :forceLookX="orangeForceLookX"
          :forceLookY="orangeForceLookY"
        />
      </div>
    </div>

    <!-- Yellow tall rectangle character - Front right -->
    <div
      ref="yellowRef"
      class="character character-yellow"
      :style="yellowStyle"
    >
      <!-- Eyes - just pupils, no white -->
      <div class="eyes" :style="yellowEyesStyle">
        <Pupil
          :size="12"
          :maxDistance="5"
          pupilColor="#2D2D2D"
          :forceLookX="yellowForceLookX"
          :forceLookY="yellowForceLookY"
        />
        <Pupil
          :size="12"
          :maxDistance="5"
          pupilColor="#2D2D2D"
          :forceLookX="yellowForceLookX"
          :forceLookY="yellowForceLookY"
        />
      </div>
      <!-- Mouth -->
      <div class="mouth" :style="yellowMouthStyle"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import Pupil from './Pupil.vue'
import EyeBall from './EyeBall.vue'

interface Props {
  isTyping?: boolean
  showPassword?: boolean
  passwordLength?: number
}

const props = withDefaults(defineProps<Props>(), {
  isTyping: false,
  showPassword: false,
  passwordLength: 0
})

// Mouse position
const mouseX = ref(0)
const mouseY = ref(0)

// Blinking states
const isPurpleBlinking = ref(false)
const isBlackBlinking = ref(false)

// Interaction states
const isLookingAtEachOther = ref(false)
const isPurplePeeking = ref(false)

// Character refs
const containerRef = ref<HTMLElement | null>(null)
const purpleRef = ref<HTMLElement | null>(null)
const blackRef = ref<HTMLElement | null>(null)
const orangeRef = ref<HTMLElement | null>(null)
const yellowRef = ref<HTMLElement | null>(null)

// Mouse move handler
const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

// Calculate character position based on mouse
const calculatePosition = (ref: { value: HTMLElement | null }) => {
  if (!ref.value) return { faceX: 0, faceY: 0, bodySkew: 0 }

  const rect = ref.value.getBoundingClientRect()
  const centerX = rect.left + rect.width / 2
  const centerY = rect.top + rect.height / 3

  const deltaX = mouseX.value - centerX
  const deltaY = mouseY.value - centerY

  const faceX = Math.max(-15, Math.min(15, deltaX / 20))
  const faceY = Math.max(-10, Math.min(10, deltaY / 30))
  const bodySkew = Math.max(-6, Math.min(6, -deltaX / 120))

  return { faceX, faceY, bodySkew }
}

// Character positions
const purplePos = computed(() => calculatePosition(purpleRef))
const blackPos = computed(() => calculatePosition(blackRef))
const orangePos = computed(() => calculatePosition(orangeRef))
const yellowPos = computed(() => calculatePosition(yellowRef))

// Derived state
const isHidingPassword = computed(() => props.passwordLength > 0 && !props.showPassword)
const isShowingPassword = computed(() => props.passwordLength > 0 && props.showPassword)

// Purple character styles
const purpleStyle = computed(() => ({
  height: (props.isTyping || isHidingPassword.value) ? '440px' : '400px',
  transform: isShowingPassword.value
    ? 'skewX(0deg)'
    : (props.isTyping || isHidingPassword.value)
      ? `skewX(${(purplePos.value.bodySkew || 0) - 12}deg) translateX(40px)`
      : `skewX(${purplePos.value.bodySkew || 0}deg)`
}))

const purpleEyesStyle = computed(() => ({
  left: isShowingPassword.value ? '20px' : isLookingAtEachOther.value ? '55px' : `${45 + purplePos.value.faceX}px`,
  top: isShowingPassword.value ? '35px' : isLookingAtEachOther.value ? '65px' : `${40 + purplePos.value.faceY}px`
}))

const purpleForceLookX = computed(() => {
  if (isShowingPassword.value) return isPurplePeeking.value ? 4 : -4
  if (isLookingAtEachOther.value) return 3
  return undefined
})

const purpleForceLookY = computed(() => {
  if (isShowingPassword.value) return isPurplePeeking.value ? 5 : -4
  if (isLookingAtEachOther.value) return 4
  return undefined
})

// Black character styles
const blackStyle = computed(() => ({
  transform: isShowingPassword.value
    ? 'skewX(0deg)'
    : isLookingAtEachOther.value
      ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
      : (props.isTyping || isHidingPassword.value)
        ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5}deg)`
        : `skewX(${blackPos.value.bodySkew || 0}deg)`
}))

const blackEyesStyle = computed(() => ({
  left: isShowingPassword.value ? '10px' : isLookingAtEachOther.value ? '32px' : `${26 + blackPos.value.faceX}px`,
  top: isShowingPassword.value ? '28px' : isLookingAtEachOther.value ? '12px' : `${32 + blackPos.value.faceY}px`
}))

const blackForceLookX = computed(() => {
  if (isShowingPassword.value) return -4
  if (isLookingAtEachOther.value) return 0
  return undefined
})

const blackForceLookY = computed(() => {
  if (isShowingPassword.value) return -4
  if (isLookingAtEachOther.value) return -4
  return undefined
})

// Orange character styles
const orangeStyle = computed(() => ({
  transform: isShowingPassword.value ? 'skewX(0deg)' : `skewX(${orangePos.value.bodySkew || 0}deg)`
}))

const orangeEyesStyle = computed(() => ({
  left: isShowingPassword.value ? '50px' : `${82 + (orangePos.value.faceX || 0)}px`,
  top: isShowingPassword.value ? '85px' : `${90 + (orangePos.value.faceY || 0)}px`
}))

const orangeForceLookX = computed(() => isShowingPassword.value ? -5 : undefined)
const orangeForceLookY = computed(() => isShowingPassword.value ? -4 : undefined)

// Yellow character styles
const yellowStyle = computed(() => ({
  transform: isShowingPassword.value ? 'skewX(0deg)' : `skewX(${yellowPos.value.bodySkew || 0}deg)`
}))

const yellowEyesStyle = computed(() => ({
  left: isShowingPassword.value ? '20px' : `${52 + (yellowPos.value.faceX || 0)}px`,
  top: isShowingPassword.value ? '35px' : `${40 + (yellowPos.value.faceY || 0)}px`
}))

const yellowMouthStyle = computed(() => ({
  left: isShowingPassword.value ? '10px' : `${40 + (yellowPos.value.faceX || 0)}px`,
  top: isShowingPassword.value ? '88px' : `${88 + (yellowPos.value.faceY || 0)}px`
}))

const yellowForceLookX = computed(() => isShowingPassword.value ? -5 : undefined)
const yellowForceLookY = computed(() => isShowingPassword.value ? -4 : undefined)

// Blinking effect for purple character
let purpleBlinkTimeout: ReturnType<typeof setTimeout> | null = null
const schedulePurpleBlink = () => {
  const getRandomBlinkInterval = () => Math.random() * 4000 + 3000
  purpleBlinkTimeout = setTimeout(() => {
    isPurpleBlinking.value = true
    setTimeout(() => {
      isPurpleBlinking.value = false
      schedulePurpleBlink()
    }, 150)
  }, getRandomBlinkInterval())
}

// Blinking effect for black character
let blackBlinkTimeout: ReturnType<typeof setTimeout> | null = null
const scheduleBlackBlink = () => {
  const getRandomBlinkInterval = () => Math.random() * 4000 + 3000
  blackBlinkTimeout = setTimeout(() => {
    isBlackBlinking.value = true
    setTimeout(() => {
      isBlackBlinking.value = false
      scheduleBlackBlink()
    }, 150)
  }, getRandomBlinkInterval())
}

// Looking at each other animation when typing starts
watch(() => props.isTyping, (newVal) => {
  if (newVal) {
    isLookingAtEachOther.value = true
    setTimeout(() => {
      isLookingAtEachOther.value = false
    }, 800)
  } else {
    isLookingAtEachOther.value = false
  }
})

// Purple sneaky peeking animation when password is visible
let purplePeekTimeout: ReturnType<typeof setTimeout> | null = null
watch([() => props.passwordLength, () => props.showPassword, isPurplePeeking], ([newLength, newShow, newPeeking]) => {
  if (newLength > 0 && newShow && !newPeeking) {
    const schedulePeek = () => {
      purplePeekTimeout = setTimeout(() => {
        isPurplePeeking.value = true
        setTimeout(() => {
          isPurplePeeking.value = false
        }, 800)
      }, Math.random() * 3000 + 2000)
    }
    schedulePeek()
  } else if (!newShow) {
    isPurplePeeking.value = false
    if (purplePeekTimeout) {
      clearTimeout(purplePeekTimeout)
      purplePeekTimeout = null
    }
  }
})

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove)
  schedulePurpleBlink()
  scheduleBlackBlink()
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove)
  if (purpleBlinkTimeout) clearTimeout(purpleBlinkTimeout)
  if (blackBlinkTimeout) clearTimeout(blackBlinkTimeout)
  if (purplePeekTimeout) clearTimeout(purplePeekTimeout)
})
</script>

<style scoped lang="scss">
.animated-characters {
  position: relative;
  width: 550px;
  height: 400px;
}

.character {
  position: absolute;
  bottom: 0;
  transition: all 0.7s ease-in-out;
  transform-origin: bottom center;
}

.character-purple {
  left: 70px;
  width: 180px;
  height: 400px;
  background-color: #6C3FF5;
  border-radius: 10px 10px 0 0;
  z-index: 1;
}

.character-black {
  left: 240px;
  width: 120px;
  height: 310px;
  background-color: #2D2D2D;
  border-radius: 8px 8px 0 0;
  z-index: 2;
}

.character-orange {
  left: 0;
  width: 240px;
  height: 200px;
  background-color: #FF9B6B;
  border-radius: 120px 120px 0 0;
  z-index: 3;
}

.character-yellow {
  left: 310px;
  width: 140px;
  height: 230px;
  background-color: #E8D754;
  border-radius: 70px 70px 0 0;
  z-index: 4;
}

.eyes {
  position: absolute;
  display: flex;
  gap: 32px;
  transition: all 0.2s ease-out;
}

.character-purple .eyes {
  gap: 32px;
}

.character-black .eyes {
  gap: 24px;
}

.character-orange .eyes {
  gap: 32px;
}

.character-yellow .eyes {
  gap: 24px;
}

.mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  background-color: #2D2D2D;
  border-radius: 999px;
  transition: all 0.2s ease-out;
}

// Responsive adjustments
@media (max-width: 1200px) {
  .animated-characters {
    width: 480px;
    height: 360px;
  }

  .character-purple {
    left: 60px;
    width: 160px;
    height: 360px;
  }

  .character-black {
    left: 210px;
    width: 100px;
    height: 280px;
  }

  .character-orange {
    width: 200px;
    height: 180px;
  }

  .character-yellow {
    left: 270px;
    width: 120px;
    height: 200px;
  }
}

@media (max-width: 992px) {
  .animated-characters {
    width: 420px;
    height: 320px;
  }

  .character-purple {
    left: 55px;
    width: 140px;
    height: 320px;
  }

  .character-black {
    left: 180px;
    width: 85px;
    height: 250px;
  }

  .character-orange {
    width: 170px;
    height: 160px;
  }

  .character-yellow {
    left: 235px;
    width: 100px;
    height: 180px;
  }
}
</style>