/**
 * 路由管理测试辅助工具
 * 提供通用的测试辅助函数和常量
 *
 * @author binblink
 * @since 2026-04-12
 */

import { Page, expect } from '@playwright/test'

// 测试配置
export const TEST_CONFIG = {
  BASE_URL: process.env.BASE_URL || 'http://localhost:3001',
  API_BASE: process.env.API_BASE || 'http://localhost:8008',
  DEFAULT_TIMEOUT: 10000,
  LONG_TIMEOUT: 30000,
}

// 测试用户
export const TEST_USER = {
  loginName: process.env.TEST_USER || 'admin',
  password: process.env.TEST_PASSWORD || '123456',
}

// 测试路由数据模板
export const TEST_ROUTE_TEMPLATE = {
  routeId: 'test-e2e-route',
  routeName: 'E2E测试路由',
  uri: 'lb://test-service',
  predicates: [{ name: 'Path', args: { pattern: '/api/test/**' } }],
  filters: [{ name: 'StripPrefix', args: { parts: '1' } }],
  orderNum: 1,
  routesGroup: 'default',
  status: 1,
}

/**
 * 登录到系统
 */
export async function login(page: Page, user = TEST_USER) {
  await page.goto(`${TEST_CONFIG.BASE_URL}/login`)

  // 等待登录表单加载
  await page.waitForSelector('.login-form-wrapper', {
    timeout: TEST_CONFIG.LONG_TIMEOUT
  })

  // 填写登录表单
  await page.fill('.login-form-wrapper input[type="text"]', user.loginName)
  await page.fill('.login-form-wrapper input[type="password"]', user.password)

  // 点击登录按钮
  await page.click('.login-btn')

  // 等待跳转
  await page.waitForURL(/\/(dashboard|home)/, { timeout: TEST_CONFIG.LONG_TIMEOUT })
}

/**
 * 导航到路由管理页面
 */
export async function navigateToRouteManagement(page: Page) {
  await page.goto(`${TEST_CONFIG.BASE_URL}/route`)
  await page.waitForSelector('.route-management', { timeout: TEST_CONFIG.DEFAULT_TIMEOUT })
  await page.waitForTimeout(1000)
}

/**
 * 创建测试路由
 */
export async function createTestRoute(page: Page, routeData = TEST_ROUTE_TEMPLATE) {
  await page.click('button:has-text("新增路由")')
  await page.waitForSelector('.route-dialog', { timeout: TEST_CONFIG.DEFAULT_TIMEOUT })

  // 填写表单
  await page.fill('input[v-model="formData.routeId"]', routeData.routeId)
  await page.fill('input[v-model="formData.routeName"]', routeData.routeName || '')
  await page.fill('input[v-model="formData.uri"]', routeData.uri)
  await page.fill('input[v-model="formData.routesGroup"]', routeData.routesGroup || 'default')

  // 提交
  await page.click('.route-dialog button:has-text("确定")')

  // 等待响应
  const response = await page.waitForResponse(
    res => res.url().includes('/route/saveRoute') || res.url().includes('/route/updateRoute'),
    { timeout: TEST_CONFIG.DEFAULT_TIMEOUT }
  )

  // 等待弹窗关闭
  await page.waitForSelector('.route-dialog', { state: 'hidden', timeout: TEST_CONFIG.DEFAULT_TIMEOUT })

  return response
}

/**
 * 删除测试路由
 */
export async function deleteTestRoute(page: Page, routeId: string) {
  // 搜索路由
  await page.fill('input[v-model="searchForm.routeName"]', routeId)
  await page.click('button:has-text("查询")')
  await page.waitForTimeout(500)

  // 查找并删除
  const row = page.locator(`tr:has-text("${routeId}")`).first()
  if (await row.isVisible()) {
    await row.locator('button:has-text("删除")').click()
    await page.waitForSelector('.el-message-box', { timeout: 3000 })
    await page.click('.el-message-box button:has-text("确定")')

    await page.waitForResponse(
      res => res.url().includes('/route/deleteRoute'),
      { timeout: TEST_CONFIG.DEFAULT_TIMEOUT }
    )
  }

  // 清空搜索
  await page.fill('input[v-model="searchForm.routeName"]', '')
}

/**
 * 选择表格行
 */
export async function selectTableRow(page: Page, routeId: string) {
  const row = page.locator(`tr:has-text("${routeId}")`)
  await row.locator('input[type="checkbox"]').check()
}

/**
 * 等待API响应
 */
export async function waitForApiCall(page: Page, url: string, method = 'POST') {
  return page.waitForResponse(
    response => response.url().includes(url) && response.request().method() === method,
    { timeout: TEST_CONFIG.DEFAULT_TIMEOUT }
  )
}

/**
 * 验证成功消息
 */
export async function expectSuccessMessage(page: Page) {
  await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 })
}

/**
 * 验证错误消息
 */
export async function expectErrorMessage(page: Page) {
  await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 })
}

/**
 * 验证警告消息
 */
export async function expectWarningMessage(page: Page) {
  await expect(page.locator('.el-message--warning')).toBeVisible({ timeout: 5000 })
}

/**
 * 关闭所有弹窗
 */
export async function closeAllDialogs(page: Page) {
  const closeButtons = page.locator('.el-dialog__headerbtn')
  const count = await closeButtons.count()

  for (let i = 0; i < count; i++) {
    await closeButtons.nth(0).click()
    await page.waitForTimeout(200)
  }
}

/**
 * 生成唯一测试路由ID
 */
export function generateUniqueRouteId(prefix = 'test-route') {
  const timestamp = Date.now()
  const random = Math.floor(Math.random() * 1000)
  return `${prefix}-${timestamp}-${random}`
}

/**
 * Mock API响应（用于测试异常场景）
 */
export async function mockApiError(page: Page, url: string, status = 500) {
  await page.route(`**/api${url}`, route => {
    route.fulfill({
      status,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'ERROR',
        msg: '模拟错误响应',
      }),
    })
  })
}

/**
 * 取消所有路由拦截
 */
export async function unmockAll(page: Page) {
  await page.unrouteAll()
}
