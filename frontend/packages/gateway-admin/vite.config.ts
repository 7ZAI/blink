import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const port = parseInt(env.VITE_PORT || '4001')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:8003'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: port,
      proxy: {
        '/gateway-admin': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern-compiler',
          additionalData: `@use "@/styles/variables.scss" as *;`
        }
      }
    },
    // 性能优化配置
    build: {
      // 代码分割策略
      rollupOptions: {
        output: {
          // 分包策略
          manualChunks: {
            // Vue 核心库
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            // Element Plus UI库
            'element-plus': ['element-plus', '@element-plus/icons-vue'],
            // 国际化
            'i18n': ['vue-i18n'],
            // 图表和可视化
            'visualization': ['echarts', 'vue-echarts'],
            // 工具库
            'utils': ['axios', '@iconify/vue', '@vueuse/core'],
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
      chunkSizeWarningLimit: 1000,
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
        '@iconify/vue',
        'echarts',
        'vue-echarts',
      ],
    },
  }
})