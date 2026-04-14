import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  test: {
    // 测试环境
    environment: 'happy-dom',
    // 全局变量（如 describe、it、expect）
    globals: true,
    // 测试文件匹配模式
    include: ['src/**/*.test.ts', 'src/**/*.spec.ts'],
    // 排除文件
    exclude: ['node_modules', 'dist'],
    // 覆盖率配置
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/stores/**', 'src/api/**'],
      exclude: ['src/**/*.d.ts'],
    },
    // 设置文件（在测试前执行）
    setupFiles: ['src/test/setup.ts'],
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})