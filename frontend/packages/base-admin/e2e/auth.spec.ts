/**
 * 认证相关 E2E 测试
 *
 * @author binblink
 * @since 2026-04-16
 */

import { test, expect, Page, BrowserContext } from '@playwright/test'
import { login, logout, TEST_USER, TEST_CONFIG } from './helpers/auth-helpers'

test.describe.configure({ mode: 'serial' })

test.describe('认证功能测试', () => {
  let context: BrowserContext
  let page: Page

  test.beforeAll(async ({ browser }) => {
    context = await browser.newContext()
    page = await context.newPage()
  })

  test.afterAll(async () => {
    await context.close()
  })

  test('应能显示登录页面', async () => {
    await page.goto(`${TEST_CONFIG.BASE_URL}/login`)
    await expect(page.locator('.login-form-wrapper')).toBeVisible()
    await expect(page.locator('.login-btn')).toBeVisible()
  })

  test('空用户名应显示提示', async () => {
    await page.goto(`${TEST_CONFIG.BASE_URL}/login`)
    await page.fill('.login-form-wrapper .el-form-item').first().locator('input'), ''
    await page.fill('.login-form-wrapper input[type="password"]', TEST_USER.password)
    await page.click('.login-btn')

    // 等待提示消息
    await expect(page.locator('.el-message--warning')).toBeVisible({ timeout: 5000 })
  })

  test('错误密码应显示错误', async () => {
    await page.goto(`${TEST_CONFIG.BASE_URL}/login`)
    const usernameInput = page.locator('.login-form-wrapper .el-form-item').first().locator('input')
    await usernameInput.fill(TEST_USER.loginName)
    await page.fill('.login-form-wrapper input[type="password"]', 'wrong-password')
    await page.click('.login-btn')

    await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 })
  })

  test('正确凭据应成功登录', async () => {
    await login(page)
    await expect(page).toHaveURL(/\/(dashboard|home)/)
    await expect(page.locator('.user-avatar')).toBeVisible()
  })

  test('登录后应显示用户信息', async () => {
    // 假设已登录，检查用户名显示
    await expect(page.locator('.user-name')).toBeVisible()
  })

  test('应能成功退出登录', async () => {
    await logout(page)
    await expect(page).toHaveURL(/\/login/)
  })
})