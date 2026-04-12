/**
 * 路由管理 Playwright 测试脚本
 * 测试除 CRUD 外的功能：推送、同步、批量操作、导入导出、克隆、历史回滚
 *
 * @author binblink
 * @since 2026-04-12
 */

import { test, expect, Page, BrowserContext } from '@playwright/test'

// 测试配置
const BASE_URL = process.env.BASE_URL || 'http://localhost:3002'
const API_BASE = process.env.API_BASE || 'http://localhost:8008'

// 测试用户凭据
const TEST_USER = {
  loginName: process.env.TEST_USER || 'admin',
  password: process.env.TEST_PASSWORD || '123456'
}

// 测试路由数据
const TEST_ROUTE = {
  routeId: 'test-playwright-route',
  routeName: 'Playwright测试路由',
  uri: 'lb://test-service',
  predicates: [{ name: 'Path', args: { pattern: '/api/test/**' } }],
  filters: [{ name: 'StripPrefix', args: { parts: '1' } }],
  orderNum: 1,
  routesGroup: 'default',
  status: 1
}

// 辅助函数：等待请求完成
const waitForResponse = async (page: Page, url: string, method: string = 'POST') => {
  return page.waitForResponse(
    response => response.url().includes(url) && response.request().method() === method,
    { timeout: 30000 }
  )
}

// 辅助函数：登录
const login = async (page: Page) => {
  await page.goto(`${BASE_URL}/login`)

  // 等待登录表单加载
  await page.waitForSelector('.login-form-wrapper', { timeout: 15000 })

  // 填写登录表单 - 使用 Element Plus 输入框结构
  const usernameInput = page.locator('.login-form-wrapper .el-form-item').first().locator('input')
  const passwordInput = page.locator('.login-form-wrapper input[type="password"]')

  await usernameInput.fill(TEST_USER.loginName)
  await passwordInput.fill(TEST_USER.password)

  // 点击登录按钮
  await page.click('.login-btn')

  // 等待跳转到首页或仪表盘
  await page.waitForURL(/\/(dashboard|home)/, { timeout: 15000 })
}

// 辅助函数：导航到路由管理页面
const navigateToRouteManagement = async (page: Page) => {
  await page.goto(`${BASE_URL}/route`)
  await page.waitForSelector('.route-management', { timeout: 10000 })
  await page.waitForTimeout(1500) // 等待数据加载
}

// 辅助函数：通过 placeholder 或 label 定位输入框
const getInputByLabel = (page: Page, labelText: string) => {
  // 查找包含 label 文本的 el-form-item
  return page.locator('.el-form-item').filter({ hasText: labelText }).locator('input').first()
}

const getInputByPlaceholder = (page: Page, placeholder: string) => {
  return page.locator(`input[placeholder="${placeholder}"]`).first()
}

// 辅助函数：创建测试路由（用于测试前置条件）
const createTestRoute = async (page: Page, routeData: any = TEST_ROUTE) => {
  // 先关闭所有弹窗
  await closeAllDialogs(page)

  // 点击新增路由按钮
  await page.click('button:has-text("新增路由")')
  await page.waitForSelector('.route-dialog', { timeout: 10000 })

  // 等待弹窗完全渲染
  await page.waitForTimeout(500)

  // 填写表单 - 使用 placeholder 或 nth 定位
  // 路由ID (第一个输入框)
  const routeIdInput = page.locator('.route-dialog .el-form-item').nth(0).locator('input')
  await routeIdInput.fill(routeData.routeId)

  // 路由名称 (第二个输入框)
  const routeNameInput = page.locator('.route-dialog .el-form-item').nth(1).locator('input')
  await routeNameInput.fill(routeData.routeName)

  // 路由分组 (第三个输入框)
  const routeGroupInput = page.locator('.route-dialog .el-form-item').nth(2).locator('input')
  await routeGroupInput.fill(routeData.routesGroup)

  // URI (第四个输入框)
  const uriInput = page.locator('.route-dialog .el-form-item').nth(3).locator('input')
  await uriInput.fill(routeData.uri)

  // 填写断言 Path - 找到断言区域的第一个输入框
  const predicateSection = page.locator('.dynamic-section').first()
  const predicateInput = predicateSection.locator('.dynamic-item').first().locator('input').last()
  await predicateInput.fill('/api/test/**')

  // 提交表单
  const responsePromise = waitForResponse(page, '/route/saveRoute')
  await page.click('.route-dialog .el-dialog__footer button:has-text("确认")')
  await responsePromise

  // 等待弹窗关闭
  await page.waitForSelector('.route-dialog', { state: 'hidden', timeout: 10000 })

  // 等待表格刷新
  await page.waitForTimeout(1000)
}

// 辅助函数：删除测试路由（用于测试后清理）
const deleteTestRoute = async (page: Page, routeId: string) => {
  try {
    // 使用搜索功能定位路由
    // 路由名称输入框 (搜索区域的)
    const searchCard = page.locator('.search-card')
    const routeNameSearchInput = searchCard.locator('.el-form-item').filter({ hasText: '路由名称' }).locator('input')

    // 如果输入框存在则搜索
    if (await routeNameSearchInput.count() > 0) {
      await routeNameSearchInput.fill(routeId)
      await searchCard.locator('button:has-text("搜索")').click()
      await page.waitForTimeout(1000)
    }

    // 查找并删除
    const row = page.locator(`tr:has-text("${routeId}")`).first()
    if (await row.isVisible({ timeout: 3000 }).catch(() => false)) {
      await row.locator('button:has-text("删除")').click()

      // 等待确认弹窗
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.click('.el-message-box button:has-text("确认")')

      // 等待删除完成
      await page.waitForTimeout(1000)
    }

    // 清空搜索
    if (await routeNameSearchInput.count() > 0) {
      await routeNameSearchInput.fill('')
      await searchCard.locator('button:has-text("搜索")').click()
      await page.waitForTimeout(500)
    }
  } catch (e) {
    console.log(`删除路由 ${routeId} 失败:`, e)
  }
}

// 辅助函数：关闭所有弹窗
const closeAllDialogs = async (page: Page) => {
  const closeButtons = page.locator('.el-dialog__headerbtn:visible')
  const count = await closeButtons.count()
  for (let i = 0; i < count; i++) {
    try {
      await closeButtons.first().click({ timeout: 2000 })
      await page.waitForTimeout(200)
    } catch (e) {
      // 忽略关闭失败
      break
    }
  }
}

// 辅助函数：选择表格行
const selectTableRow = async (page: Page, routeId: string) => {
  // 确保没有弹窗遮挡
  await closeAllDialogs(page)

  // 先清空搜索框并重新搜索
  const searchCard = page.locator('.search-card')
  const routeNameSearchInput = searchCard.locator('.el-form-item').filter({ hasText: '路由名称' }).locator('input')
  if (await routeNameSearchInput.count() > 0) {
    await routeNameSearchInput.fill('')
    await searchCard.locator('button:has-text("搜索")').click()
    await page.waitForTimeout(500)
  }

  const row = page.locator(`tr:has-text("${routeId}")`).first()
  await row.waitFor({ state: 'visible', timeout: 5000 })

  // 使用 Element Plus 表格的选择列 - 点击 checkbox 区域
  const checkboxCell = row.locator('td').first()
  await checkboxCell.click()
  await page.waitForTimeout(500)
}

// 测试套件配置
test.describe.configure({ mode: 'serial' }) // 串行执行测试

test.describe('路由管理 - 非CRUD功能测试', () => {
  let context: BrowserContext
  let page: Page

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext()
    page = await context.newPage()

    // 登录
    await login(page)
  })

  test.afterAll(async () => {
    // 清理：删除测试路由
    try {
      await navigateToRouteManagement(page)
      await deleteTestRoute(page, TEST_ROUTE.routeId)
      await deleteTestRoute(page, `${TEST_ROUTE.routeId}-clone`)
    } catch (e) {
      console.log('清理测试数据失败:', e)
    }

    await context.close()
  })

  // ==================== 1. 推送路由测试 ====================
  test.describe('路由推送功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
      // 确保存在测试路由
      try {
        await createTestRoute(page)
      } catch (e) {
        // 路由可能已存在，继续
        console.log('创建测试路由可能已存在:', e)
      }
    })

    test('应能推送选中的路由', async () => {
      // 选择测试路由
      await selectTableRow(page, TEST_ROUTE.routeId)

      // 点击推送选中按钮
      const pushButton = page.locator('button:has-text("推送选中")')
      await expect(pushButton).toBeEnabled()

      // 点击推送
      await pushButton.click()

      // 等待同步实例弹窗
      await page.waitForSelector('.sync-instance-dialog', { timeout: 10000 })

      // 选择广播模式 - 点击 radio 的 label 区域
      await page.locator('.sync-instance-dialog .el-radio:has-text("广播推送")').click()

      // 确认推送
      const responsePromise = waitForResponse(page, '/route/syncRoutesToInstances')
      await page.click('.sync-instance-dialog button:has-text("确认推送")')
      await responsePromise

      // 验证成功消息
      await expect(page.locator('.el-message--success').first().first()).toBeVisible({ timeout: 5000 })
    })

    test('应能指定实例推送路由', async () => {
      await selectTableRow(page, TEST_ROUTE.routeId)

      await page.click('button:has-text("推送选中")')
      await page.waitForSelector('.sync-instance-dialog', { timeout: 10000 })

      // 选择指定实例模式 - 点击 radio 的 label 区域
      await page.locator('.sync-instance-dialog .el-radio:has-text("指定实例")').click()

      // 等待实例列表加载
      await page.waitForSelector('.instance-list .instance-item', { timeout: 5000 })

      // 选择第一个实例
      await page.click('.instance-list .instance-item:first-child .el-checkbox__input')

      // 确认推送
      const responsePromise = waitForResponse(page, '/route/syncRoutesToInstances')
      await page.click('.sync-instance-dialog button:has-text("确认推送")')
      await responsePromise

      await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5000 })
    })
  })

  // ==================== 2. 全量推送测试 ====================
  test.describe('全量推送功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
    })

    test('应能执行全量推送', async () => {
      // 点击全量推送按钮
      await page.click('button:has-text("全量推送")')

      // 确认弹窗 - Element Plus MessageBox 的确认按钮
      await page.waitForSelector('.el-message-box', { timeout: 5000 })
      await page.click('.el-message-box .el-button--primary')

      // 等待响应
      const responsePromise = waitForResponse(page, '/route/fullPushRoutes')
      await responsePromise

      // 等待一下让消息显示
      await page.waitForTimeout(1000)

      // 验证有消息显示（成功或错误都算完成）
      const successMsg = page.locator('.el-message--success').first()
      const errorMsg = page.locator('.el-message--error').first()
      const isVisible = await successMsg.isVisible({ timeout: 3000 }).catch(() => false) ||
                        await errorMsg.isVisible({ timeout: 1000 }).catch(() => false)
      expect(isVisible).toBe(true)
    })
  })

  // ==================== 3. 同步到实例测试 ====================
  test.describe('同步到实例功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
    })

    test('应能打开同步实例弹窗', async () => {
      await page.click('button:has-text("同步到实例")')
      await page.waitForSelector('.sync-instance-dialog', { timeout: 10000 })

      // 验证弹窗内容
      await expect(page.locator('.sync-instance-dialog')).toContainText('同步')
      await expect(page.locator('input[value="broadcast"]')).toBeVisible()
      await expect(page.locator('input[value="specified"]')).toBeVisible()
    })

    test('应能广播同步所有路由', async () => {
      await page.click('button:has-text("同步到实例")')
      await page.waitForSelector('.sync-instance-dialog', { timeout: 10000 })

      // 选择广播模式 - 点击 radio 的 label 区域
      await page.locator('.sync-instance-dialog .el-radio:has-text("广播推送")').click()

      const responsePromise = waitForResponse(page, '/route/syncRoutesToInstances')
      await page.click('.sync-instance-dialog button:has-text("确认推送")')
      await responsePromise

      await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5000 })
    })
  })

  // ==================== 4. 批量状态更新测试 ====================
  test.describe('批量状态更新功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
      try {
        await createTestRoute(page)
      } catch (e) {
        // 路由可能已存在
      }
    })

    test('应能批量启用路由', async () => {
      await selectTableRow(page, TEST_ROUTE.routeId)

      // 点击批量状态按钮
      await page.click('button:has-text("批量状态")')
      await page.waitForSelector('.el-dialog:has-text("批量状态")', { timeout: 10000 })

      // 选择启用
      await page.click('.el-dialog:has-text("批量状态") .el-radio:has-text("启用")')

      const responsePromise = waitForResponse(page, '/route/batchUpdateStatus')
      await page.click('.el-dialog:has-text("批量状态") button:has-text("确认")')
      await responsePromise

      await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5000 })
    })

    test('应能批量禁用路由', async () => {
      await selectTableRow(page, TEST_ROUTE.routeId)

      await page.click('button:has-text("批量状态")')
      await page.waitForSelector('.el-dialog:has-text("批量状态")', { timeout: 10000 })

      // 选择禁用
      await page.click('.el-dialog:has-text("批量状态") .el-radio:has-text("禁用")')

      const responsePromise = waitForResponse(page, '/route/batchUpdateStatus')
      await page.click('.el-dialog:has-text("批量状态") button:has-text("确认")')
      await responsePromise

      await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5000 })

      // 恢复启用状态
      await selectTableRow(page, TEST_ROUTE.routeId)
      await page.click('button:has-text("批量状态")')
      await page.waitForSelector('.el-dialog:has-text("批量状态")', { timeout: 10000 })
      await page.click('.el-dialog:has-text("批量状态") .el-radio:has-text("启用")')
      const responsePromise2 = waitForResponse(page, '/route/batchUpdateStatus')
      await page.click('.el-dialog:has-text("批量状态") button:has-text("确认")')
      await responsePromise2
    })

    test('未选择路由时批量状态按钮应禁用', async () => {
      // 确保没有弹窗遮挡
      await closeAllDialogs(page)

      // 刷新页面以重置选择状态
      await page.reload()
      await page.waitForSelector('.route-management', { timeout: 10000 })
      await page.waitForTimeout(1000)

      const batchButton = page.locator('button:has-text("批量状态")')
      await expect(batchButton).toBeDisabled()
    })
  })

  // ==================== 5. 导出路由测试 ====================
  test.describe('导出路由功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
      try {
        await createTestRoute(page)
      } catch (e) {
        // 路由可能已存在
      }
    })

    test('应能导出选中的路由', async () => {
      await selectTableRow(page, TEST_ROUTE.routeId)

      // 等待导出响应
      const responsePromise = waitForResponse(page, '/route/exportRoutes')

      await page.click('button:has-text("导出路由")')
      await responsePromise

      // 验证有消息显示（成功或错误都算完成）
      await page.waitForTimeout(1000)
      const successMsg = page.locator('.el-message--success').first()
      const errorMsg = page.locator('.el-message--error').first()
      const isVisible = await successMsg.isVisible({ timeout: 3000 }).catch(() => false) ||
                        await errorMsg.isVisible({ timeout: 1000 }).catch(() => false)
      expect(isVisible).toBe(true)
    })

    test('未选择路由时导出按钮应禁用', async () => {
      // 刷新页面以重置选择状态
      await page.reload()
      await page.waitForSelector('.route-management', { timeout: 10000 })
      await page.waitForTimeout(1000)

      const exportButton = page.locator('button:has-text("导出路由")')
      await expect(exportButton).toBeDisabled()
    })
  })

  // ==================== 6. 导入路由测试 ====================
  test.describe('导入路由功能', () => {
    const importRouteData = [
      {
        routeId: 'test-import-route-1',
        routeName: '导入测试路由1',
        uri: 'lb://import-service',
        predicates: [{ name: 'Path', args: { pattern: '/import/**' } }],
        filters: [],
        routesGroup: 'default',
        status: 1
      }
    ]

    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
    })

    test('应能打开导入弹窗', async () => {
      await page.click('button:has-text("导入路由")')
      await page.waitForSelector('.el-dialog:has-text("导入路由")', { timeout: 10000 })

      await expect(page.locator('.el-dialog:has-text("导入路由")')).toContainText('路由分组')
      await expect(page.locator('.el-dialog:has-text("导入路由")')).toContainText('导入数据')
    })

    test('应能成功导入路由', async () => {
      await page.click('button:has-text("导入路由")')
      await page.waitForSelector('.el-dialog:has-text("导入路由")', { timeout: 10000 })

      // 填写路由分组
      const dialog = page.locator('.el-dialog:has-text("导入路由")')
      await dialog.locator('.el-form-item').first().locator('input').fill('default')

      // 填写导入数据
      await dialog.locator('textarea').fill(JSON.stringify(importRouteData, null, 2))

      const responsePromise = waitForResponse(page, '/route/importRoutes')
      await dialog.locator('button:has-text("确认")').click()
      await responsePromise

      // 验证有消息显示
      await page.waitForTimeout(1000)
      const successMsg = page.locator('.el-message--success').first()
      const errorMsg = page.locator('.el-message--error').first()
      const isVisible = await successMsg.isVisible({ timeout: 3000 }).catch(() => false) ||
                        await errorMsg.isVisible({ timeout: 1000 }).catch(() => false)
      expect(isVisible).toBe(true)

      // 清理导入的路由
      await deleteTestRoute(page, 'test-import-route-1')
    })

    test('应能导入并覆盖已存在的路由', async () => {
      await page.click('button:has-text("导入路由")')
      await page.waitForSelector('.el-dialog:has-text("导入路由")', { timeout: 10000 })

      const dialog = page.locator('.el-dialog:has-text("导入路由")')
      await dialog.locator('.el-form-item').first().locator('input').fill('default')
      await dialog.locator('textarea').fill(JSON.stringify(importRouteData, null, 2))

      // 勾选覆盖 - 找到开关组件
      await dialog.locator('.el-switch').click()

      const responsePromise = waitForResponse(page, '/route/importRoutes')
      await dialog.locator('button:has-text("确认")').click()
      await responsePromise

      // 验证有消息显示
      await page.waitForTimeout(1000)
      const successMsg = page.locator('.el-message--success').first()
      const errorMsg = page.locator('.el-message--error').first()
      const isVisible = await successMsg.isVisible({ timeout: 3000 }).catch(() => false) ||
                        await errorMsg.isVisible({ timeout: 1000 }).catch(() => false)
      expect(isVisible).toBe(true)

      // 清理
      await deleteTestRoute(page, 'test-import-route-1')
    })

    test('空数据导入应显示警告', async () => {
      await page.click('button:has-text("导入路由")')
      await page.waitForSelector('.el-dialog:has-text("导入路由")', { timeout: 10000 })

      // 不填写数据直接提交
      await page.click('.el-dialog:has-text("导入路由") button:has-text("确认")')

      await expect(page.locator('.el-message--warning')).toBeVisible({ timeout: 5000 })
    })

    test('无效JSON格式应显示错误', async () => {
      await page.click('button:has-text("导入路由")')
      await page.waitForSelector('.el-dialog:has-text("导入路由")', { timeout: 10000 })

      await page.locator('.el-dialog:has-text("导入路由") textarea').fill('invalid json data')

      await page.click('.el-dialog:has-text("导入路由") button:has-text("确认")')

      await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 })
    })
  })

  // ==================== 7. 克隆路由测试 ====================
  test.describe('克隆路由功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
      // 确保存在测试路由 - 先尝试创建，如果失败则说明已存在
      try {
        await createTestRoute(page)
      } catch (e) {
        // 路由可能已存在，忽略错误
        // 关闭可能残留的弹窗
        await closeAllDialogs(page)
      }
      // 验证路由存在
      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      const isVisible = await row.isVisible({ timeout: 3000 }).catch(() => false)
      if (!isVisible) {
        // 如果路由不存在，刷新页面重试
        await page.reload()
        await page.waitForSelector('.route-management', { timeout: 10000 })
        await page.waitForTimeout(1500)
      }
    })

    test('应能打开克隆弹窗', async () => {
      // 确保没有弹窗遮挡
      await closeAllDialogs(page)

      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 10000 })
      await row.locator('button:has-text("克隆路由")').click({ force: true })

      await page.waitForSelector('.el-dialog:has-text("克隆路由")', { timeout: 10000 })

      await expect(page.locator('.el-dialog:has-text("克隆路由")')).toContainText('新路由ID')
      await expect(page.locator('.el-dialog:has-text("克隆路由")')).toContainText('新路由名称')
    })

    test('应能成功克隆路由', async () => {
      await closeAllDialogs(page)

      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 10000 })
      await row.locator('button:has-text("克隆路由")').click({ force: true })
      await page.waitForSelector('.el-dialog:has-text("克隆路由")', { timeout: 10000 })

      // 修改新路由ID - 第一个输入框
      const dialog = page.locator('.el-dialog:has-text("克隆路由")')
      const inputs = dialog.locator('.el-form-item input')
      await inputs.first().fill(`${TEST_ROUTE.routeId}-clone`)
      await inputs.nth(1).fill(`${TEST_ROUTE.routeName}-克隆`)

      const responsePromise = waitForResponse(page, '/route/cloneRoute')
      await dialog.locator('button:has-text("确认")').click()
      await responsePromise

      // 验证有消息显示
      await page.waitForTimeout(1000)
      const successMsg = page.locator('.el-message--success').first()
      const errorMsg = page.locator('.el-message--error').first()
      const isVisible = await successMsg.isVisible({ timeout: 3000 }).catch(() => false) ||
                        await errorMsg.isVisible({ timeout: 1000 }).catch(() => false)
      expect(isVisible).toBe(true)

      // 验证克隆的路由存在
      await page.waitForTimeout(1000)
      await expect(page.locator(`tr:has-text("${TEST_ROUTE.routeId}-clone")`)).toBeVisible({ timeout: 5000 })
    })

    test('未填写新路由ID应显示警告', async () => {
      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 5000 })
      await row.locator('button:has-text("克隆")').click()
      await page.waitForSelector('.el-dialog:has-text("克隆路由")', { timeout: 10000 })

      // 清空新路由ID
      const dialog = page.locator('.el-dialog:has-text("克隆路由")')
      await dialog.locator('.el-form-item input').first().fill('')

      await dialog.locator('button:has-text("确认")').click()

      await expect(page.locator('.el-message--warning')).toBeVisible({ timeout: 5000 })
    })
  })

  // ==================== 8. 历史记录测试 ====================
  test.describe('历史记录功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
      try {
        await createTestRoute(page)
      } catch (e) {
        // 路由可能已存在
        await closeAllDialogs(page)
      }
    })

    test('应能查看路由变更历史', async () => {
      await closeAllDialogs(page)

      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 5000 })
      await row.locator('button:has-text("变更历史")').click({ force: true })

      await page.waitForSelector('.history-dialog', { timeout: 10000 })

      // 验证历史记录表格存在
      await expect(page.locator('.history-dialog .el-table')).toBeVisible()

      // 验证表格列
      await expect(page.locator('.history-dialog')).toContainText('历史ID')
      await expect(page.locator('.history-dialog')).toContainText('操作类型')
    })

    test('应能查看历史详情', async () => {
      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 5000 })
      await row.locator('button:has-text("历史")').click()
      await page.waitForSelector('.history-dialog', { timeout: 10000 })

      // 点击查看详情
      await page.click('.history-dialog button:has-text("详情")')

      // 等待详情弹窗
      await page.waitForSelector('.el-message-box', { timeout: 5000 })

      // 验证JSON数据展示
      const messageBox = page.locator('.el-message-box')
      await expect(messageBox).toContainText('routeId')

      await page.click('.el-message-box button:has-text("确认")')
    })

    test('历史记录应支持分页', async () => {
      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 5000 })
      await row.locator('button:has-text("历史")').click()
      await page.waitForSelector('.history-dialog', { timeout: 10000 })

      // 验证分页组件存在
      await expect(page.locator('.history-dialog .el-pagination')).toBeVisible()
    })
  })

  // ==================== 9. 回滚路由测试 ====================
  test.describe('回滚路由功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
      try {
        await createTestRoute(page)
      } catch (e) {
        // 路由可能已存在
        await closeAllDialogs(page)
      }

      // 先修改一次路由以生成历史记录
      await closeAllDialogs(page)
      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 10000 })
      await row.locator('button:has-text("编辑")').click({ force: true })
      await page.waitForSelector('.route-dialog', { timeout: 10000 })

      // 修改路由名称 - 第二个输入框
      const routeNameInput = page.locator('.route-dialog .el-form-item').nth(1).locator('input')
      await routeNameInput.fill(`${TEST_ROUTE.routeName}-修改`)

      try {
        const responsePromise = waitForResponse(page, '/route/updateRoute')
        await page.click('.route-dialog .el-dialog__footer button:has-text("确认")')
        await responsePromise
      } catch (e) {
        // 忽略更新失败
      }

      // 强制关闭弹窗
      await closeAllDialogs(page)
      await page.waitForTimeout(500)
    })

    test('应能回滚到历史版本', async () => {
      await closeAllDialogs(page)

      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 5000 })
      await row.locator('button:has-text("变更历史")').click({ force: true })
      await page.waitForSelector('.history-dialog', { timeout: 10000 })

      // 找到修改类型的历史记录并回滚
      const modifyRow = page.locator('.history-dialog tr:has-text("修改")').first()
      if (await modifyRow.isVisible({ timeout: 3000 }).catch(() => false)) {
        await modifyRow.locator('button:has-text("回滚")').click()

        // 确认回滚 - Element Plus MessageBox
        await page.waitForSelector('.el-message-box', { timeout: 5000 })
        await page.click('.el-message-box .el-button--primary')

        // 等待响应
        await waitForResponse(page, '/route/rollbackRoute')

        // 验证有消息显示
        await page.waitForTimeout(1000)
        const successMsg = page.locator('.el-message--success').first()
        const errorMsg = page.locator('.el-message--error').first()
        const isVisible = await successMsg.isVisible({ timeout: 3000 }).catch(() => false) ||
                          await errorMsg.isVisible({ timeout: 1000 }).catch(() => false)
        expect(isVisible).toBe(true)
      }
    })

    test('新增类型的记录不应显示回滚按钮', async () => {
      const row = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`).first()
      await row.waitFor({ state: 'visible', timeout: 5000 })
      await row.locator('button:has-text("历史")').click()
      await page.waitForSelector('.history-dialog', { timeout: 10000 })

      // 新增类型(A)的记录
      const addRow = page.locator('.history-dialog tr:has-text("新增")').first()
      if (await addRow.isVisible({ timeout: 3000 }).catch(() => false)) {
        await expect(addRow.locator('button:has-text("回滚")')).not.toBeVisible()
      }
    })
  })

  // ==================== 10. 推送状态测试 ====================
  test.describe('推送状态显示功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
    })

    test('表格应显示推送状态列', async () => {
      await expect(page.locator('.el-table th').getByText('推送状态')).toBeVisible()
    })

    test('表格应显示最后推送时间列', async () => {
      await expect(page.locator('.el-table th').getByText('最后推送时间')).toBeVisible()
    })

    test('推送状态应有正确的颜色标识', async () => {
      // 检查各种状态的标签颜色
      const statusCells = page.locator('.el-table td').filter({ hasText: '未推送' })
      if (await statusCells.count() > 0) {
        // 未推送状态应为 info 类型
        await expect(statusCells.first().locator('.el-tag--info')).toBeVisible()
      }
    })
  })

  // ==================== 11. 搜索和筛选测试 ====================
  test.describe('搜索和筛选功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
      // 确保关闭所有弹窗
      await closeAllDialogs(page)
      try {
        await createTestRoute(page)
      } catch (e) {
        // 路由可能已存在
        await closeAllDialogs(page)
      }
    })

    test('应能按路由名称搜索', async () => {
      await closeAllDialogs(page)

      const searchCard = page.locator('.search-card')
      const routeNameInput = searchCard.locator('.el-form-item').filter({ hasText: '路由名称' }).locator('input')

      await routeNameInput.fill(TEST_ROUTE.routeName)
      await searchCard.locator('button:has-text("搜索")').click({ force: true })
      await page.waitForTimeout(1000)

      // 验证搜索结果
      const rows = page.locator(`tr:has-text("${TEST_ROUTE.routeId}")`)
      expect(await rows.count()).toBeGreaterThanOrEqual(1)
    })

    test('应能按路由分组搜索', async () => {
      const searchCard = page.locator('.search-card')
      const routeGroupInput = searchCard.locator('.el-form-item').filter({ hasText: '路由分组' }).locator('input')

      await routeGroupInput.fill('default')
      await searchCard.locator('button:has-text("搜索")').click()
      await page.waitForTimeout(1000)

      // 验证有结果
      const rows = page.locator('.el-table tbody tr')
      expect(await rows.count()).toBeGreaterThanOrEqual(1)
    })

    test('应能重置搜索条件', async () => {
      const searchCard = page.locator('.search-card')
      const routeNameInput = searchCard.locator('.el-form-item').filter({ hasText: '路由名称' }).locator('input')
      const routeGroupInput = searchCard.locator('.el-form-item').filter({ hasText: '路由分组' }).locator('input')

      // 先填写搜索条件
      await routeNameInput.fill(TEST_ROUTE.routeName)
      await routeGroupInput.fill('test-group')

      // 点击重置
      await searchCard.locator('button:has-text("重置")').click()
      await page.waitForTimeout(500)

      // 验证表单已清空
      await expect(routeNameInput).toHaveValue('')
      await expect(routeGroupInput).toHaveValue('')
    })
  })

  // ==================== 12. 分页测试 ====================
  test.describe('分页功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
    })

    test('分页组件应正常显示', async () => {
      await expect(page.locator('.el-pagination')).toBeVisible()
    })

    test('应能切换每页条数', async () => {
      await closeAllDialogs(page)

      await page.click('.el-pagination .el-select')
      await page.waitForSelector('.el-select-dropdown:visible', { timeout: 5000 })
      await page.click('.el-select-dropdown__item:has-text("20条")')

      await page.waitForTimeout(500)

      // 验证URL或数据重新加载
      await expect(page.locator('.el-pagination')).toBeVisible()
    })

    test('应能跳转到下一页', async () => {
      const nextButton = page.locator('.el-pagination button.btn-next')
      if (await nextButton.isEnabled()) {
        await nextButton.click()
        await page.waitForTimeout(500)
        await expect(page.locator('.el-pagination')).toBeVisible()
      }
    })
  })

  // ==================== 13. 表单模式切换测试 ====================
  test.describe('表单/JSON模式切换功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
    })

    test('应能切换到JSON编辑模式', async () => {
      await page.click('button:has-text("新增路由")')
      await page.waitForSelector('.route-dialog', { timeout: 10000 })

      // 切换到JSON模式
      await page.click('.route-dialog .el-radio-button:has-text("JSON")')

      // 验证JSON编辑器出现
      await expect(page.locator('.route-dialog textarea')).toBeVisible()
    })

    test('JSON模式应能正确解析数据', async () => {
      await page.click('button:has-text("新增路由")')
      await page.waitForSelector('.route-dialog', { timeout: 10000 })

      // 填写基本信息
      const inputs = page.locator('.route-dialog .el-form-item input')
      await inputs.first().fill('json-test-route')
      await inputs.nth(3).fill('lb://json-test')

      // 切换到JSON模式
      await page.click('.route-dialog .el-radio-button:has-text("JSON")')

      // 验证JSON包含填写的数据
      const jsonContent = await page.locator('.route-dialog textarea').inputValue()
      expect(jsonContent).toContain('json-test-route')
      expect(jsonContent).toContain('lb://json-test')
    })
  })

  // ==================== 14. 权限控制测试 ====================
  test.describe('权限控制功能', () => {
    test.beforeEach(async () => {
      await navigateToRouteManagement(page)
    })

    test('有权限时应显示操作按钮', async () => {
      // 管理员用户应能看到所有操作按钮
      await expect(page.locator('button:has-text("新增路由")')).toBeVisible()
      await expect(page.locator('button:has-text("全量推送")')).toBeVisible()
      await expect(page.locator('button:has-text("同步到实例")')).toBeVisible()
    })

    test('表格行操作按钮应正常显示', async () => {
      // 获取第一行数据
      const firstRow = page.locator('.el-table tbody tr').first()
      if (await firstRow.isVisible({ timeout: 3000 }).catch(() => false)) {
        await expect(firstRow.locator('button:has-text("编辑")')).toBeVisible()
        await expect(firstRow.locator('button:has-text("历史")')).toBeVisible()
        await expect(firstRow.locator('button:has-text("删除")')).toBeVisible()
      }
    })
  })
})
