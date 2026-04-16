/**
 * 认证测试辅助工具
 *
 * @author binblink
 * @since 2026-04-16
 */

import { Page, expect } from '@playwright/test'

// 测试配置
export const TEST_CONFIG = {
  BASE_URL: process.env.BASE_URL || 'http://localhost:3001',
  API_BASE: process.env.API_BASE || 'http://localhost:8080',
  DEFAULT_TIMEOUT: 10000,
  LONG_TIMEOUT: 30000,
}

// 测试用户
export const TEST_USER = {
  loginName: process.env.TEST_USER || 'admin',
  password: process.env.TEST_PASSWORD || '123456',
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

  // 填写登录表单 - Element Plus 输入框结构
  const usernameInput = page.locator('.login-form-wrapper .el-form-item').first().locator('input')
  const passwordInput = page.locator('.login-form-wrapper input[type="password"]')

  await usernameInput.fill(user.loginName)
  await passwordInput.fill(user.password)

  // 点击登录按钮
  await page.click('.login-btn')

  // 等待跳转
  await page.waitForURL(/\/(dashboard|home)/, { timeout: TEST_CONFIG.LONG_TIMEOUT })
}

/**
 * 退出登录
 */
export async function logout(page: Page) {
  // 点击用户头像/下拉菜单
  await page.click('.user-avatar')
  await page.click('button:has-text("退出登录")')

  // 等待跳转到登录页
  await page.waitForURL(/\/login/, { timeout: TEST_CONFIG.DEFAULT_TIMEOUT })
}

/**
 * 检查是否已登录
 */
export async function isLoggedIn(page: Page): Promise<boolean> {
  try {
    await page.waitForSelector('.user-avatar', { timeout: 3000 })
    return true
  } catch {
    return false
  }
}

/**
 * 等待请求完成
 */
export async function waitForResponse(page: Page, url: string, method: string = 'POST') {
  return page.waitForResponse(
    response => response.url().includes(url) && response.request().method() === method,
    { timeout: TEST_CONFIG.LONG_TIMEOUT }
  )
}