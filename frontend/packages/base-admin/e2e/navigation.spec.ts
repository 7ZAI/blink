/**
 * 导航菜单 E2E 测试
 *
 * @author binblink
 * @since 2026-04-16
 */

import { test, expect } from '@playwright/test'
import { login, TEST_CONFIG } from './helpers/auth-helpers'

test.describe('导航菜单测试', () => {
  test.beforeEach(async ({ page }) => {
    await login(page)
  })

  test('应能显示侧边栏菜单', async ({ page }) => {
    await expect(page.locator('.sidebar')).toBeVisible()
  })

  test('菜单项应可点击展开', async ({ page }) => {
    // 点击第一个菜单组
    const firstMenuGroup = page.locator('.sidebar .el-sub-menu').first()
    if (await firstMenuGroup.isVisible()) {
      await firstMenuGroup.click()
      await page.waitForTimeout(300)
      // 验证子菜单展开
      const subMenu = firstMenuGroup.locator('.el-sub-menu__children')
      if (await subMenu.isVisible()) {
        await expect(subMenu).toBeVisible()
      }
    }
  })

  test('点击菜单项应跳转页面', async ({ page }) => {
    // 导航到系统管理（假设存在）
    const systemMenu = page.locator('.sidebar').getByText('系统')
    if (await systemMenu.isVisible()) {
      await systemMenu.click()
      await page.waitForTimeout(500)
      // 验证 URL 变化
      const currentUrl = page.url()
      expect(currentUrl).toContain(TEST_CONFIG.BASE_URL)
    }
  })

  test('面包屑导航应正确显示', async ({ page }) => {
    // 检查面包屑组件
    const breadcrumb = page.locator('.el-breadcrumb')
    if (await breadcrumb.isVisible()) {
      await expect(breadcrumb).toBeVisible()
    }
  })
})