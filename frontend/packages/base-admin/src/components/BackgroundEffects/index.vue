<template>
  <div class="background-container" ref="containerRef" :class="{ 'light-mode': isLightMode }">
    <canvas ref="canvasRef" class="bg-canvas"></canvas>

    <!-- 科技网格叠加层 -->
    <div class="grid-overlay"></div>

    <!-- 光晕效果层 -->
    <div class="glow-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <!-- 交互提示 -->
    <div class="interaction-hint">
      <span>⚡ 移动鼠标探索</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const isLightMode = computed(() => themeStore.theme === 'light')

const containerRef = ref<HTMLDivElement>()
const canvasRef = ref<HTMLCanvasElement>()
let animationId: number
let ctx: CanvasRenderingContext2D | null = null

// 鼠标状态
const mouse = {
  x: 0,
  y: 0,
  radius: 180,
  isMoving: false,
  lastMoveTime: 0,
}

// 粒子系统
interface Particle {
  x: number
  y: number
  baseX: number
  baseY: number
  vx: number
  vy: number
  size: number
  color: string
  alpha: number
  pulsePhase: number
  type: 'circle' | 'star' | 'diamond'
}

// 流星
interface Meteor {
  x: number
  y: number
  length: number
  speed: number
  angle: number
  alpha: number
  color: string
}

// 数据流
interface DataStream {
  x: number
  y: number
  speed: number
  chars: string[]
  alpha: number
}

let particles: Particle[] = []
let meteors: Meteor[] = []
let dataStreams: DataStream[] = []
let canvasWidth = 0
let canvasHeight = 0
let time = 0

// 性能优化：RGBA颜色缓存
const rgbaCache: Map<string, Map<number, string>> = new Map()

// 性能优化：帧率控制
let lastFrameTime = 0
const targetFPS = 60
const frameInterval = 1000 / targetFPS

// 性能优化：设备检测
const isLowEndDevice = navigator.hardwareConcurrency ? navigator.hardwareConcurrency <= 4 : false
const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
  navigator.userAgent
)

// 颜色主题 - 科技风增强
const colors = {
  primary: '#3b82f6',
  secondary: '#8b5cf6',
  accent: '#00d4ff',
  pink: '#ec4899',
  green: '#10b981',
  orange: '#f97316',
  neonBlue: '#00f5ff',
  neonPurple: '#bf00ff',
}

// 初始化粒子
const initParticles = () => {
  particles = []
  // 性能优化：低性能设备减少粒子数量
  const baseCount = Math.floor((canvasWidth * canvasHeight) / 6000)
  const maxParticles = isLowEndDevice || isMobile ? 80 : 200
  const particleCount = Math.min(maxParticles, baseCount)

  for (let i = 0; i < particleCount; i++) {
    const x = Math.random() * canvasWidth
    const y = Math.random() * canvasHeight
    const colorKeys = Object.keys(colors) as (keyof typeof colors)[]
    const randomColorKey = colorKeys[Math.floor(Math.random() * colorKeys.length)]
    const randomColor = randomColorKey ? colors[randomColorKey] : colors.primary
    const types: Particle['type'][] = ['circle', 'star', 'diamond']

    const randomType = types[Math.floor(Math.random() * types.length)]
    particles.push({
      x,
      y,
      baseX: x,
      baseY: y,
      vx: (Math.random() - 0.5) * 0.8,
      vy: (Math.random() - 0.5) * 0.8,
      size: Math.random() * 4 + 1.5,
      color: randomColor,
      alpha: Math.random() * 0.6 + 0.3,
      pulsePhase: Math.random() * Math.PI * 2,
      type: randomType ?? 'circle',
    })
  }

  // 初始化流星 - 低性能设备减少数量
  meteors = []
  const meteorCount = isLowEndDevice ? 1 : 3
  for (let i = 0; i < meteorCount; i++) {
    meteors.push(createMeteor())
  }

  // 初始化数据流 - 低性能设备减少数量
  dataStreams = []
  const streamCount = isLowEndDevice ? 2 : 5
  for (let i = 0; i < streamCount; i++) {
    dataStreams.push(createDataStream())
  }
}

// 创建流星
const createMeteor = (): Meteor => ({
  x: Math.random() * canvasWidth,
  y: -50,
  length: Math.random() * 80 + 50,
  speed: Math.random() * 8 + 5,
  angle: Math.PI / 4 + (Math.random() - 0.5) * 0.3,
  alpha: Math.random() * 0.5 + 0.5,
  color: Math.random() > 0.5 ? colors.neonBlue : colors.neonPurple,
})

// 创建数据流
const createDataStream = (): DataStream => ({
  x: Math.random() * canvasWidth,
  y: Math.random() * canvasHeight,
  speed: Math.random() * 2 + 1,
  chars: Array.from({ length: 10 }, () => String.fromCharCode(0x30a0 + Math.random() * 96)),
  alpha: Math.random() * 0.3 + 0.1,
})

// 绘制多边形粒子
const drawShape = (
  x: number,
  y: number,
  size: number,
  type: Particle['type'],
  color: string,
  alpha: number
) => {
  ctx!.beginPath()

  if (type === 'star') {
    // 五角星
    for (let i = 0; i < 5; i++) {
      const angle = (i * 4 * Math.PI) / 5 - Math.PI / 2
      const px = x + Math.cos(angle) * size
      const py = y + Math.sin(angle) * size
      if (i === 0) ctx!.moveTo(px, py)
      else ctx!.lineTo(px, py)
    }
    ctx!.closePath()
  } else if (type === 'diamond') {
    // 菱形
    ctx!.moveTo(x, y - size)
    ctx!.lineTo(x + size * 0.7, y)
    ctx!.lineTo(x, y + size)
    ctx!.lineTo(x - size * 0.7, y)
    ctx!.closePath()
  } else {
    // 圆形
    ctx!.arc(x, y, size, 0, Math.PI * 2)
  }

  ctx!.fillStyle = hexToRgba(color, alpha)
  ctx!.fill()
}

// 更新粒子
const updateParticles = () => {
  const currentTime = Date.now()

  particles.forEach((p) => {
    // 脉冲效果
    p.pulsePhase += 0.03
    const pulse = Math.sin(p.pulsePhase) * 0.4 + 1

    // 鼠标交互
    const dx = mouse.x - p.x
    const dy = mouse.y - p.y
    const dist = Math.sqrt(dx * dx + dy * dy)

    if (dist < mouse.radius && mouse.isMoving) {
      const force = (mouse.radius - dist) / mouse.radius
      const angle = Math.atan2(dy, dx)

      // 排斥力
      p.vx -= Math.cos(angle) * force * 3
      p.vy -= Math.sin(angle) * force * 3

      // 颜色变化
      if (force > 0.3) {
        p.color = colors.accent
        p.alpha = Math.min(1, p.alpha + 0.15)
      }
    }

    // 回归原位
    p.vx += (p.baseX - p.x) * 0.008
    p.vy += (p.baseY - p.y) * 0.008

    // 阻尼
    p.vx *= 0.94
    p.vy *= 0.94

    // 更新位置
    p.x += p.vx
    p.y += p.vy

    // 边界检查
    if (p.x < 0 || p.x > canvasWidth) p.vx *= -1
    if (p.y < 0 || p.y > canvasHeight) p.vy *= -1

    // 逐渐恢复原始颜色和透明度
    if (!mouse.isMoving || dist > mouse.radius) {
      p.alpha = Math.max(0.3, p.alpha - 0.008)
    }
  })

  // 更新流星
  meteors.forEach((m, index) => {
    m.x += Math.cos(m.angle) * m.speed
    m.y += Math.sin(m.angle) * m.speed
    m.alpha -= 0.005

    if (m.y > canvasHeight + 100 || m.alpha <= 0) {
      meteors[index] = createMeteor()
    }
  })

  // 更新数据流
  dataStreams.forEach((ds) => {
    ds.y += ds.speed
    if (ds.y > canvasHeight + 200) {
      ds.y = -200
      ds.x = Math.random() * canvasWidth
    }
  })

  // 检测鼠标是否停止移动
  if (currentTime - mouse.lastMoveTime > 100) {
    mouse.isMoving = false
  }
}

// 绘制连接线
const drawConnections = () => {
  const maxDist = 150

  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const p1 = particles[i]
      const p2 = particles[j]
      if (!p1 || !p2) continue

      const dx = p1.x - p2.x
      const dy = p1.y - p2.y
      const dist = Math.sqrt(dx * dx + dy * dy)

      if (dist < maxDist) {
        const opacity = (1 - dist / maxDist) * 0.4

        ctx!.beginPath()
        ctx!.moveTo(p1.x, p1.y)
        ctx!.lineTo(p2.x, p2.y)

        const gradient = ctx!.createLinearGradient(p1.x, p1.y, p2.x, p2.y)
        gradient.addColorStop(0, hexToRgba(p1.color, opacity))
        gradient.addColorStop(1, hexToRgba(p2.color, opacity))

        ctx!.strokeStyle = gradient
        ctx!.lineWidth = opacity * 1.5
        ctx!.stroke()
      }
    }
  }
}

// 绘制流星
const drawMeteors = () => {
  meteors.forEach((m) => {
    const gradient = ctx!.createLinearGradient(
      m.x,
      m.y,
      m.x - Math.cos(m.angle) * m.length,
      m.y - Math.sin(m.angle) * m.length
    )
    gradient.addColorStop(0, hexToRgba(m.color, m.alpha))
    gradient.addColorStop(0.3, hexToRgba(m.color, m.alpha * 0.5))
    gradient.addColorStop(1, 'transparent')

    ctx!.beginPath()
    ctx!.moveTo(m.x, m.y)
    ctx!.lineTo(m.x - Math.cos(m.angle) * m.length, m.y - Math.sin(m.angle) * m.length)
    ctx!.strokeStyle = gradient
    ctx!.lineWidth = 2
    ctx!.stroke()

    // 流星头部发光
    const headGlow = ctx!.createRadialGradient(m.x, m.y, 0, m.x, m.y, 8)
    headGlow.addColorStop(0, hexToRgba(m.color, m.alpha))
    headGlow.addColorStop(1, 'transparent')
    ctx!.beginPath()
    ctx!.arc(m.x, m.y, 8, 0, Math.PI * 2)
    ctx!.fillStyle = headGlow
    ctx!.fill()
  })
}

// 绘制数据流
const drawDataStreams = () => {
  ctx!.font = '12px monospace'

  dataStreams.forEach((ds) => {
    ds.chars.forEach((char, index) => {
      const y = ds.y - index * 15
      const charAlpha = ds.alpha * (1 - index / ds.chars.length)

      ctx!.fillStyle = hexToRgba(colors.accent, charAlpha)
      ctx!.fillText(char, ds.x, y)
    })
  })
}

// 绘制粒子
const drawParticles = () => {
  particles.forEach((p) => {
    const speed = Math.sqrt(p.vx * p.vx + p.vy * p.vy)
    const size = p.size * (1 + speed * 0.15)

    // 发光效果
    const gradient = ctx!.createRadialGradient(p.x, p.y, 0, p.x, p.y, size * 4)
    gradient.addColorStop(0, hexToRgba(p.color, p.alpha * 0.8))
    gradient.addColorStop(0.4, hexToRgba(p.color, p.alpha * 0.3))
    gradient.addColorStop(1, 'transparent')

    ctx!.beginPath()
    ctx!.arc(p.x, p.y, size * 4, 0, Math.PI * 2)
    ctx!.fillStyle = gradient
    ctx!.fill()

    // 绘制形状
    drawShape(p.x, p.y, size, p.type, p.color, p.alpha + 0.3)
  })
}

// 绘制鼠标效果
const drawMouseEffect = () => {
  if (!mouse.isMoving) return

  // 外圈脉冲
  for (let i = 4; i > 0; i--) {
    const radius = mouse.radius * (1 + i * 0.15)
    const alpha = 0.08 / i

    ctx!.beginPath()
    ctx!.arc(mouse.x, mouse.y, radius, 0, Math.PI * 2)
    ctx!.strokeStyle = hexToRgba(colors.neonBlue, alpha)
    ctx!.lineWidth = 1.5
    ctx!.stroke()
  }

  // 内部光晕
  const gradient = ctx!.createRadialGradient(mouse.x, mouse.y, 0, mouse.x, mouse.y, mouse.radius)
  gradient.addColorStop(0, hexToRgba(colors.primary, 0.2))
  gradient.addColorStop(0.3, hexToRgba(colors.secondary, 0.1))
  gradient.addColorStop(1, 'transparent')

  ctx!.beginPath()
  ctx!.arc(mouse.x, mouse.y, mouse.radius, 0, Math.PI * 2)
  ctx!.fillStyle = gradient
  ctx!.fill()

  // 十字准星
  ctx!.beginPath()
  ctx!.moveTo(mouse.x - 20, mouse.y)
  ctx!.lineTo(mouse.x + 20, mouse.y)
  ctx!.moveTo(mouse.x, mouse.y - 20)
  ctx!.lineTo(mouse.x, mouse.y + 20)
  ctx!.strokeStyle = hexToRgba(colors.accent, 0.4)
  ctx!.lineWidth = 1
  ctx!.stroke()

  // 中心点
  ctx!.beginPath()
  ctx!.arc(mouse.x, mouse.y, 3, 0, Math.PI * 2)
  ctx!.fillStyle = hexToRgba(colors.accent, 0.8)
  ctx!.fill()
}

// 绘制背景渐变
const drawBackground = () => {
  if (isLightMode.value) {
    // 浅色模式背景
    const gradient = ctx!.createRadialGradient(
      canvasWidth / 2,
      canvasHeight / 2,
      0,
      canvasWidth / 2,
      canvasHeight / 2,
      Math.max(canvasWidth, canvasHeight)
    )
    gradient.addColorStop(0, '#f1f5f9')
    gradient.addColorStop(0.5, '#e2e8f0')
    gradient.addColorStop(1, '#f8fafc')

    ctx!.fillStyle = gradient
    ctx!.fillRect(0, 0, canvasWidth, canvasHeight)
  } else {
    // 深色模式背景
    const gradient = ctx!.createRadialGradient(
      canvasWidth / 2,
      canvasHeight / 2,
      0,
      canvasWidth / 2,
      canvasHeight / 2,
      Math.max(canvasWidth, canvasHeight)
    )
    gradient.addColorStop(0, '#0f0f2e')
    gradient.addColorStop(0.5, '#0a0a1a')
    gradient.addColorStop(1, '#050510')

    ctx!.fillStyle = gradient
    ctx!.fillRect(0, 0, canvasWidth, canvasHeight)
  }
}

// 十六进制转 RGBA（带缓存优化）
const hexToRgba = (hex: string, alpha: number): string => {
  // 检查缓存
  let hexCache = rgbaCache.get(hex)
  if (!hexCache) {
    hexCache = new Map()
    rgbaCache.set(hex, hexCache)
  }

  const cached = hexCache.get(alpha)
  if (cached) return cached

  // 计算并缓存
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const result = `rgba(${r}, ${g}, ${b}, ${alpha})`

  hexCache.set(alpha, result)
  return result
}

// 动画循环（带帧率控制）
const animate = (timestamp: number) => {
  // 帧率控制：确保不超过目标帧率
  const elapsed = timestamp - lastFrameTime
  if (elapsed < frameInterval) {
    animationId = requestAnimationFrame(animate)
    return
  }
  lastFrameTime = timestamp - (elapsed % frameInterval)

  time += 1

  // 清除画布
  ctx!.clearRect(0, 0, canvasWidth, canvasHeight)

  // 绘制各层
  drawBackground()
  drawDataStreams()
  updateParticles()

  // 性能优化：低性能设备跳过连接线绘制
  if (!isLowEndDevice) {
    drawConnections()
  }

  drawParticles()
  drawMeteors()
  drawMouseEffect()

  animationId = requestAnimationFrame(animate)
}

// 鼠标移动处理
const handleMouseMove = (e: MouseEvent) => {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect) {
    mouse.x = e.clientX - rect.left
    mouse.y = e.clientY - rect.top
    mouse.isMoving = true
    mouse.lastMoveTime = Date.now()
  }
}

// 鼠标离开处理
const handleMouseLeave = () => {
  mouse.isMoving = false
}

// 窗口大小变化
const handleResize = () => {
  if (canvasRef.value) {
    canvasWidth = window.innerWidth
    canvasHeight = window.innerHeight
    canvasRef.value.width = canvasWidth
    canvasRef.value.height = canvasHeight
    initParticles()
  }
}

// 触摸支持
const handleTouchMove = (e: TouchEvent) => {
  const touch = e.touches[0]
  if (!touch) return

  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect) {
    mouse.x = touch.clientX - rect.left
    mouse.y = touch.clientY - rect.top
    mouse.isMoving = true
    mouse.lastMoveTime = Date.now()
  }
}

const handleTouchEnd = () => {
  mouse.isMoving = false
}

onMounted(() => {
  if (!canvasRef.value) return

  ctx = canvasRef.value.getContext('2d')
  if (!ctx) return

  canvasWidth = window.innerWidth
  canvasHeight = window.innerHeight
  canvasRef.value.width = canvasWidth
  canvasRef.value.height = canvasHeight

  // 初始化
  initParticles()

  // 事件监听
  window.addEventListener('resize', handleResize)
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseleave', handleMouseLeave)
  window.addEventListener('touchmove', handleTouchMove, { passive: true })
  window.addEventListener('touchend', handleTouchEnd)

  // 开始动画
  animationId = requestAnimationFrame(animate)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }

  window.removeEventListener('resize', handleResize)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseleave', handleMouseLeave)
  window.removeEventListener('touchmove', handleTouchMove)
  window.removeEventListener('touchend', handleTouchEnd)
})
</script>

<style scoped lang="scss">
.background-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  z-index: 0;
  background: radial-gradient(ellipse at center, #0f0f2e 0%, #0a0a1a 50%, #050510 100%);
  transition: background 0.5s ease;

  /* 浅色模式背景 */
  &.light-mode {
    background: radial-gradient(ellipse at center, #f1f5f9 0%, #e2e8f0 50%, #f8fafc 100%);

    .grid-overlay {
      background-image:
        linear-gradient(rgba(59, 130, 246, 0.06) 1px, transparent 1px),
        linear-gradient(90deg, rgba(59, 130, 246, 0.06) 1px, transparent 1px);
    }

    .orb {
      opacity: 0.2;
    }

    .interaction-hint span {
      background: rgba(59, 130, 246, 0.1);
      border-color: rgba(59, 130, 246, 0.2);
      color: #3b82f6;
      text-shadow: none;
      box-shadow: 0 0 15px rgba(59, 130, 246, 0.15);
    }
  }
}

.bg-canvas {
  width: 100%;
  height: 100%;
  display: block;
  /* GPU加速提示 */
  will-change: transform;
  transform: translateZ(0);
}

/* 科技网格叠加层 */
.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  animation: grid-pulse 8s ease-in-out infinite;
}

@keyframes grid-pulse {
  0%,
  100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

/* 光晕效果层 */
.glow-orbs {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  animation: float 15s ease-in-out infinite;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.4) 0%, transparent 70%);
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.orb-2 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.4) 0%, transparent 70%);
  bottom: -50px;
  right: -50px;
  animation-delay: -5s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.3) 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -10s;
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(30px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-20px, 20px) scale(0.9);
  }
  75% {
    transform: translate(20px, 30px) scale(1.05);
  }
}

/* 扫描线效果 */
.scan-line {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(0, 212, 255, 0.3) 20%,
    rgba(0, 212, 255, 0.8) 50%,
    rgba(0, 212, 255, 0.3) 80%,
    transparent 100%
  );
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.5);
  animation: scan 4s linear infinite;
  pointer-events: none;
}

@keyframes scan {
  0% {
    top: -2px;
  }
  100% {
    top: 100%;
  }
}

/* 交互提示 */
.interaction-hint {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;

  span {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    background: rgba(59, 130, 246, 0.1);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(0, 212, 255, 0.3);
    border-radius: 25px;
    color: rgba(0, 212, 255, 0.9);
    font-size: 13px;
    font-weight: 500;
    text-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
    animation: hint-fade 4s ease-in-out forwards;
    animation-delay: 3s;
    box-shadow:
      0 0 20px rgba(0, 212, 255, 0.2),
      inset 0 0 20px rgba(0, 212, 255, 0.05);
  }
}

@keyframes hint-fade {
  0% {
    opacity: 1;
    transform: translateY(0);
  }
  100% {
    opacity: 0;
    transform: translateY(-15px);
  }
}
</style>
