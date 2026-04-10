import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import dts from 'vite-plugin-dts'
import VueMacros from 'vue-macros'

export default defineConfig({
  plugins: [
    vue(),
    dts({
      tsconfigPath: './tsconfig.app.json',
      outDir: 'dist',
      include: [
        'src/lib-index.ts',
        'src/components/**/*.vue',
        'src/composables/**/*.ts',
        'src/directives/**/*.ts',
        'src/config/**/*.ts',
      ],
      exclude: ['node_modules/**', 'src/views/**'],
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  build: {
    lib: {
      entry: resolve(__dirname, 'src/lib-index.ts'),
      name: 'BlinkComponents',
      formats: ['es', 'cjs'],
      fileName: (format) => `index.${format === 'es' ? 'js' : 'cjs'}`,
    },
    rollupOptions: {
      external: [
        'vue',
        'vue-router',
        'pinia',
        'element-plus',
        '@element-plus/icons-vue',
        '@iconify/vue',
        'vue-i18n',
      ],
      output: {
        globals: {
          vue: 'Vue',
          'vue-router': 'VueRouter',
          pinia: 'Pinia',
          'element-plus': 'ElementPlus',
          'vue-i18n': 'VueI18n',
        },
      },
    },
    cssCodeSplit: false,
  },
})
