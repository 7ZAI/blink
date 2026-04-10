# 验证码组件重构设计文档

## 概述

将现有的 `CaptchaSlider` 组件拆分为三个独立的子组件，提高可维护性、复用性和可测试性。

## 背景

### 现状
- 组件位于 `src/components/CaptchaSlider/`
- 单文件 `index.vue` 约 1340 行，包含所有逻辑
- 支持两种验证模式：滑块拼图(blockPuzzle)和点选文字(clickWord)
- 弹窗逻辑与验证逻辑耦合

### 问题
- 代码量过大，难以维护
- 滑块验证和点选验证逻辑混杂
- 难以单独测试各部分功能
- 不符合项目组件拆分规范

## 设计决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| 使用模式 | 组合使用 | 保持向后兼容，外部只通过入口组件使用 |
| 触发器定位 | 保持现状 | 继续使用 `showTrigger` prop 控制 |
| 状态管理 | Composable 集中管理 | 状态统一，调试方便 |
| 样式处理 | 组件内 scoped 样式 | 每个子组件保留自己的样式，复制共享部分 |
| 架构模式 | 三层架构 | 职责清晰，符合项目规范 |

## 目录结构

```
src/components/CaptchaSlider/
├── index.vue                    # 入口组合组件
├── types.ts                     # 共享类型定义
├── composables/
│   └── useCaptchaCore.ts        # 核心状态和逻辑
└── components/
    ├── CaptchaDialog.vue        # 弹窗容器组件
    ├── BlockPuzzle.vue          # 滑块拼图验证
    └── ClickWord.vue            # 点选文字验证
```

## 组件设计

### 1. 入口组件 (index.vue)

**职责**：组合触发器和弹窗，暴露公开方法，保持向后兼容。

**Props**：保持现有 Props 不变
```typescript
interface CaptchaSliderProps {
  captchaType?: CaptchaType
  enabled?: boolean
  verified?: boolean
  dialogTitle?: string
  dialogWidth?: string | number
  getCaptchaApi?: (params: CaptchaRequestParams) => Promise<CaptchaData>
  checkCaptchaApi?: (params: CaptchaCheckParams) => Promise<CaptchaCheckResult>
  sliderMaxDistance?: number
  imageWidth?: number
  imageHeight?: number
  autoCloseOnSuccess?: boolean
  showTrigger?: boolean
  triggerText?: string
  sliderHint?: string
  clickWordHint?: string
  disabled?: boolean
  locale?: CaptchaLocale
  customClass?: string
  sliderClass?: string
  dialogClass?: string
}
```

**Emits**：保持现有 Emits 不变
```typescript
interface CaptchaSliderEmits {
  (e: 'update:verified', value: boolean): void
  (e: 'success', result: CaptchaCheckResult): void
  (e: 'fail', result: CaptchaCheckResult): void
  (e: 'refresh'): void
  (e: 'open'): void
  (e: 'close'): void
  (e: 'trigger-click'): void
}
```

**公开方法**：保持现有方法不变
- `open()` - 打开弹窗
- `close()` - 关闭弹窗
- `refresh()` - 刷新验证码
- `reset()` - 重置验证状态
- `getVerificationData()` - 获取验证结果数据

**实现要点**：
- 使用 `useCaptchaCore` composable 管理核心状态
- 触发器部分保持原有实现，使用 `showTrigger` 控制显示
- 引用 `CaptchaDialog` 子组件，传递必要 props
- 处理弹窗事件，转发到外部

---

### 2. useCaptchaCore Composable

**职责**：管理验证码的核心状态和逻辑，作为入口组件和子组件之间的"状态中心"。

**接口定义**：
```typescript
interface UseCaptchaCoreOptions {
  captchaType: CaptchaType
  getCaptchaApi: (params: CaptchaRequestParams) => Promise<CaptchaData>
  checkCaptchaApi: (params: CaptchaCheckParams) => Promise<CaptchaCheckResult>
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  locale: CaptchaLocale
  onSuccess: (result: CaptchaCheckResult) => void
  onFail: (result: CaptchaCheckResult) => void
}

interface UseCaptchaCoreReturn {
  // 状态
  captchaData: Ref<CaptchaData>
  currentCaptchaType: Ref<CaptchaType>
  loading: Ref<boolean>
  isRefreshing: Ref<boolean>
  imageLoaded: Ref<boolean>
  clientUid: Ref<string>

  // 滑块专用状态
  sliderLeft: Ref<number>
  jigsawLeft: Ref<number>

  // 点选专用状态
  clickedPoints: Ref<ClickPoint[]>

  // 核心方法
  fetchCaptcha: () => Promise<void>
  refreshCaptcha: () => Promise<void>
  submitSliderCaptcha: () => Promise<void>
  submitWordCaptcha: () => Promise<void>

  // 工具方法
  formatImageData: (base64: string) => string
  handleImageLoad: () => void

  // 拖动方法（供 BlockPuzzle 使用）
  startDrag: (e: MouseEvent | TouchEvent) => void
  onDrag: (e: MouseEvent | TouchEvent) => void
  stopDrag: () => Promise<void>

  // 点击方法（供 ClickWord 使用）
  handleWordClick: (e: MouseEvent) => void
}
```

**核心逻辑**：
1. **fetchCaptcha**：调用 API 获取验证码数据，解析 pointJson 获取 y 坐标
2. **refreshCaptcha**：设置刷新状态，重新获取验证码
3. **submitSliderCaptcha**：组装 pointJson，调用校验 API，处理结果
4. **submitWordCaptcha**：组装点击点 JSON，调用校验 API，处理结果
5. **startDrag/onDrag/stopDrag**：滑块拖动交互逻辑，使用 requestAnimationFrame
6. **handleWordClick**：记录点击坐标，达到数量自动提交

---

### 3. CaptchaDialog.vue - 弹窗容器组件

**职责**：包装 el-dialog，根据验证类型切换显示子组件。

**Props**：
```typescript
interface CaptchaDialogProps {
  modelValue: boolean              // 弹窗显示状态（双向绑定）
  captchaData: CaptchaData         // 验证码数据
  currentCaptchaType: CaptchaType  // 当前验证类型
  loading: boolean                 // 加载状态
  isRefreshing: boolean            // 刷新状态
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  sliderLeft: number               // 滑块位置（传给 BlockPuzzle）
  jigsawLeft: number               // 拼图位置（传给 BlockPuzzle）
  clickedPoints: ClickPoint[]      // 点击点（传给 ClickWord）
  locale: CaptchaLocale
  dialogTitle: string
  dialogWidth: string | number
  sliderHint: string
  clickWordHint: string
  sliderClass: string
  dialogClass: string
}
```

**Emits**：
```typescript
interface CaptchaDialogEmits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'start-drag', event: MouseEvent | TouchEvent): void
  (e: 'on-drag', event: MouseEvent | TouchEvent): void
  (e: 'stop-drag'): void
  (e: 'word-click', event: MouseEvent): void
  (e: 'open'): void
  (e: 'closed'): void
}
```

**模板结构**：
```vue
<el-dialog v-model="visible" :title="dialogTitle" ...>
  <!-- 加载状态 -->
  <div v-if="loading">...</div>

  <!-- 滑块验证 -->
  <BlockPuzzle v-else-if="currentCaptchaType === 'blockPuzzle'"
    :captchaData="captchaData"
    :sliderLeft="sliderLeft"
    :jigsawLeft="jigsawLeft"
    @refresh="$emit('refresh')"
    @image-load="$emit('image-load')"
    @start-drag="$emit('start-drag', $event)"
    @on-drag="$emit('on-drag', $event)"
    @stop-drag="$emit('stop-drag')"
  />

  <!-- 点选验证 -->
  <ClickWord v-else-if="currentCaptchaType === 'clickWord'"
    :captchaData="captchaData"
    :clickedPoints="clickedPoints"
    @refresh="$emit('refresh')"
    @image-load="$emit('image-load')"
    @word-click="$emit('word-click', $event)"
  />
</el-dialog>
```

---

### 4. BlockPuzzle.vue - 滑块拼图验证组件

**职责**：纯 UI 层，显示背景图、拼图块、滑块轨道，绑定拖动事件。

**Props**：
```typescript
interface BlockPuzzleProps {
  captchaData: CaptchaData         // 包含 originalImageBase64、jigsawImageBase64
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  sliderLeft: number               // 滑块位置（由父组件控制）
  jigsawLeft: number               // 拼图位置（由父组件控制）
  isRefreshing: boolean
  locale: CaptchaLocale
  sliderClass: string
}
```

**Emits**：
```typescript
interface BlockPuzzleEmits {
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'start-drag', event: MouseEvent | TouchEvent): void
  (e: 'on-drag', event: MouseEvent | TouchEvent): void
  (e: 'stop-drag'): void
}
```

**模板结构**：
```vue
<div class="block-puzzle-captcha">
  <!-- 图片容器 -->
  <div class="captcha-image-wrapper" :style="{ width, height }">
    <img :src="bgImage" class="captcha-bg-image" @load="$emit('image-load')" />
    <img :src="jigsawImage" class="captcha-jigsaw-image" :style="{ left: jigsawLeft }" />
    <!-- 刷新遮罩 -->
    <div v-if="isRefreshing" class="refresh-overlay">...</div>
  </div>

  <!-- 滑块轨道 -->
  <div class="slider-container">
    <div class="slider-track">
      <div class="slider-fill" :style="{ width: sliderLeft + 36 }" />
    </div>
    <div class="slider-thumb" :style="{ transform: `translateX(${sliderLeft}px)` }"
      @mousedown="$emit('start-drag', $event)"
      @touchstart="$emit('start-drag', $event)">
      <svg>...</svg>
    </div>
    <span class="slider-hint">{{ locale.dragToVerify }}</span>
    <button class="refresh-btn" @click="$emit('refresh')">...</button>
  </div>
</div>
```

**样式**：从现有组件提取滑块验证相关样式，保持 scoped。

---

### 5. ClickWord.vue - 点选文字验证组件

**职责**：纯 UI 层，显示图片、文字提示、点击标记点，绑定点击事件。

**Props**：
```typescript
interface ClickWordProps {
  captchaData: CaptchaData         // 包含 originalImageBase64、wordList
  imageWidth: number
  imageHeight: number
  clickedPoints: ClickPoint[]      // 已点击的点（由父组件控制）
  isRefreshing: boolean
  locale: CaptchaLocale
}
```

**Emits**：
```typescript
interface ClickWordEmits {
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'word-click', event: MouseEvent): void
}
```

**模板结构**：
```vue
<div class="click-word-captcha">
  <!-- 文字提示 -->
  <div class="word-hint">
    {{ locale.clickWordHint }}: {{ captchaData.wordList?.join(', ') }}
  </div>

  <!-- 图片容器 -->
  <div class="captcha-image-wrapper" :style="{ width, height }"
    @click="$emit('word-click', $event)">
    <img :src="bgImage" class="captcha-bg-image" @load="$emit('image-load')" />
    <!-- 点击标记点 -->
    <div v-for="(point, index) in clickedPoints" class="click-point"
      :style="{ left: point.x, top: point.y }">
      {{ index + 1 }}
    </div>
    <!-- 刷新遮罩 -->
    <div v-if="isRefreshing" class="refresh-overlay">...</div>
  </div>

  <!-- 操作按钮 -->
  <div class="click-actions">
    <el-button type="primary" @click="handleConfirm">{{ locale.confirm }}</el-button>
    <el-button @click="$emit('refresh')">{{ locale.refresh }}</el-button>
  </div>
</div>
```

**样式**：从现有组件提取点选验证相关样式，保持 scoped。

---

## 类型更新 (types.ts)

新增子组件类型定义：

```typescript
// ========== 子组件 Props ==========

export interface CaptchaDialogProps {
  modelValue: boolean
  captchaData: CaptchaData
  currentCaptchaType: CaptchaType
  loading: boolean
  isRefreshing: boolean
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  sliderLeft: number
  jigsawLeft: number
  clickedPoints: ClickPoint[]
  locale: CaptchaLocale
  dialogTitle: string
  dialogWidth: string | number
  sliderHint: string
  clickWordHint: string
  sliderClass: string
  dialogClass: string
}

export interface BlockPuzzleProps {
  captchaData: CaptchaData
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  sliderLeft: number
  jigsawLeft: number
  isRefreshing: boolean
  locale: CaptchaLocale
  sliderClass: string
}

export interface ClickWordProps {
  captchaData: CaptchaData
  imageWidth: number
  imageHeight: number
  clickedPoints: ClickPoint[]
  isRefreshing: boolean
  locale: CaptchaLocale
}

// ========== 子组件 Emits ==========

export interface CaptchaDialogEmits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'start-drag', event: MouseEvent | TouchEvent): void
  (e: 'on-drag', event: MouseEvent | TouchEvent): void
  (e: 'stop-drag'): void
  (e: 'word-click', event: MouseEvent): void
  (e: 'open'): void
  (e: 'closed'): void
}

export interface BlockPuzzleEmits {
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'start-drag', event: MouseEvent | TouchEvent): void
  (e: 'on-drag', event: MouseEvent | TouchEvent): void
  (e: 'stop-drag'): void
}

export interface ClickWordEmits {
  (e: 'refresh'): void
  (e: 'image-load'): void
  (e: 'word-click', event: MouseEvent): void
}

// ========== Composable 类型 ==========

export interface UseCaptchaCoreOptions {
  captchaType: CaptchaType
  getCaptchaApi: (params: CaptchaRequestParams) => Promise<CaptchaData>
  checkCaptchaApi: (params: CaptchaCheckParams) => Promise<CaptchaCheckResult>
  imageWidth: number
  imageHeight: number
  sliderMaxDistance: number
  locale: CaptchaLocale
  onSuccess: (result: CaptchaCheckResult) => void
  onFail: (result: CaptchaCheckResult) => void
}
```

## 向后兼容性

- `CaptchaSlider` 入口组件的 Props/Emits 保持不变
- 现有 API 接口 (`getCaptchaApi`, `checkCaptchaApi`) 保持不变
- 公开方法 (`open`, `close`, `refresh`, `reset`, `getVerificationData`) 保持不变
- `lib-index.ts` 的导出保持不变，不导出子组件

## 数据流

```
用户交互                    事件流                        状态更新
─────────────────────────────────────────────────────────────────────
点击触发器    →    handleTriggerClick()    →    dialogVisible = true
                                    ↓
                               fetchCaptcha()
                                    ↓
                            captchaData 更新
                                    ↓
                          CaptchaDialog 显示
                                    ↓
┌───────────────────────────────────────────────────────────────────┐
│  BlockPuzzle                        ClickWord                     │
│  ─────────────                     ─────────────                  │
│  @mousedown → startDrag()          @click → handleWordClick()     │
│  @mousemove → onDrag()                    ↓                       │
│  @mouseup → stopDrag()             clickedPoints.push()           │
│        ↓                                  ↓                       │
│  sliderLeft 更新                  自动/手动 submit                 │
│        ↓                                  ↓                       │
│  松手后 submitSliderCaptcha()      submitWordCaptcha()            │
└───────────────────────────────────────────────────────────────────┘
                                    ↓
                            checkCaptchaApi()
                                    ↓
                         handleCheckResult()
                                    ↓
                    success → emit('success'), close
                    fail → emit('fail'), refresh
```

## 验证方式

1. 运行开发服务器：`npm run dev`
2. 访问验证码组件演示页面
3. 测试滑块验证功能：
   - 拖动滑块
   - 验证成功/失败
   - 刷新验证码
4. 测试点选验证功能：
   - 点击文字位置
   - 自动提交/手动确认
   - 验证成功/失败
5. 检查深色模式样式适配
6. 检查国际化文本显示
7. 检查触发器显示/隐藏切换

## 文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/components/CaptchaSlider/index.vue` | 重构 | 简化为组合层，使用 composable |
| `src/components/CaptchaSlider/types.ts` | 更新 | 新增子组件类型定义 |
| `src/components/CaptchaSlider/composables/useCaptchaCore.ts` | 新建 | 核心状态和逻辑 |
| `src/components/CaptchaSlider/components/CaptchaDialog.vue` | 新建 | 弹窗容器组件 |
| `src/components/CaptchaSlider/components/BlockPuzzle.vue` | 新建 | 滑块拼图验证组件 |
| `src/components/CaptchaSlider/components/ClickWord.vue` | 新建 | 点选文字验证组件 |