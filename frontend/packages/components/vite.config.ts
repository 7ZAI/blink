import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { channelConfig, HEADER_CONSTANTS } from './src/config/channel'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const apiMode = env.VITE_API_MODE || 'gateway'
  const gatewayUrl = env.VITE_GATEWAY_URL || channelConfig.gatewayUrl || 'http://localhost:8002'
  const directUrl = env.VITE_DIRECT_URL || 'http://localhost:8001'

  const proxyTarget = apiMode === 'gateway' ? gatewayUrl : directUrl

  console.log(`[Vite Config] API模式: ${apiMode}`)
  console.log(`[Vite Config] 代理目标: ${proxyTarget}`)

  return {
    plugins: [
      vue(),
      // 自动导入 Element Plus API
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/auto-imports.d.ts',
      }),
      // 自动注册 Element Plus 组件
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts',
      }),
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 4000,
      proxy: {
        '/base': {
          target: proxyTarget,
          changeOrigin: true,
          headers: {
            [HEADER_CONSTANTS.X_BLINK_APPKEY]: channelConfig.appKey,
          },
        },
      },
    },
    // 性能优化配置
    build: {
      // 代码分割策略
      rollupOptions: {
        output: {
          // 分包策略 - Element Plus 已按需引入，无需单独 chunk
          manualChunks: {
            // Vue 核心库
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            // 国际化
            'i18n': ['vue-i18n'],
            // 图表和可视化（按需加载）
            'visualization': ['@logicflow/core', '@logicflow/extension'],
            // 工具库
            'utils': ['axios', '@iconify/vue'],
          },
          // 文件命名
          chunkFileNames: 'assets/js/[name]-[hash].js',
          entryFileNames: 'assets/js/[name]-[hash].js',
          assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
        },
      },
      // 压缩配置
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: mode === 'production',
          drop_debugger: mode === 'production',
        },
      },
      // 启用 CSS 代码分割
      cssCodeSplit: true,
      // 设置 chunk 大小警告限制
      chunkSizeWarningLimit: 500,
    },
    // 优化依赖预构建
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'element-plus',
        'vue-i18n',
        'axios',
      ],
    },
  }
})
