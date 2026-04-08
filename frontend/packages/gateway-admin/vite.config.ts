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
        },
        '/notification': {
          target: proxyTarget,
          changeOrigin: true
        }
      },
      // 预热常用文件，加快首次访问速度
      warmup: {
        clientFiles: [
          './src/main.ts',
          './src/App.vue',
          './src/router/index.ts',
          './src/stores/theme.ts',
          './src/views/login/index.vue',
        ],
      },
      // 文件系统缓存
      fs: {
        cachedChecks: true,
      },
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
    // 优化依赖预构建 - 强制预构建大型依赖
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'element-plus',
        'element-plus/es',
        'element-plus/es/components/message/style/css',
        'element-plus/es/components/notification/style/css',
        'element-plus/es/components/message-box/style/css',
        'element-plus/es/components/loading/style/css',
        'vue-i18n',
        'axios',
        '@iconify/vue',
        '@vueuse/core',
        'echarts',
        'vue-echarts',
      ],
      // 强制预构建，不等待首次访问
      force: false,
    },
  }
})