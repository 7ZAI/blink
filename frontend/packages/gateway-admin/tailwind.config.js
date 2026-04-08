/** @type {import('tailwindcss').Config} */
export default {
  // 扫描的文件路径
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
    // 扫描 @blink/components 组件库 - 使用精确路径避免匹配过多文件
    "../components/src/**/*.{vue,js,ts}",
  ],
  // 使用 class 模式控制暗黑模式
  darkMode: 'class',
  theme: {
    extend: {
      // 颜色配置 - 引用 CSS 变量确保主题一致性
      colors: {
        // 主题色
        primary: {
          DEFAULT: 'var(--primary-color)',
          light: 'var(--primary-color-light)',
          dark: 'var(--primary-color-dark)',
          'light-5': 'var(--primary-color-light-5)',
          'light-7': 'var(--primary-color-light-7)',
          'light-8': 'var(--primary-color-light-8)',
          'light-9': 'var(--primary-color-light-9)',
        },
        success: 'var(--success-color)',
        warning: 'var(--warning-color)',
        danger: 'var(--danger-color)',
        info: 'var(--info-color)',
        // 文本颜色
        'text-primary': 'var(--text-color-primary)',
        'text-regular': 'var(--text-color-regular)',
        'text-secondary': 'var(--text-color-secondary)',
        'text-placeholder': 'var(--text-color-placeholder)',
        // 背景颜色
        'bg-page': 'var(--bg-color-page)',
        'bg-card': 'var(--card-bg)',
        'bg-sidebar': 'var(--sidebar-bg)',
        'bg-header': 'var(--header-bg)',
        // 边框颜色
        border: 'var(--border-color-base)',
        'border-light': 'var(--border-color-light)',
        'border-lighter': 'var(--border-color-lighter)',
        // 表格相关
        'table-header': 'var(--table-header-bg)',
        'table-hover': 'var(--table-row-hover)',
        'table-stripe': 'var(--table-stripe-bg)',
        'table-border': 'var(--table-border-color)',
        // 输入框相关
        'input-bg': 'var(--input-bg)',
        'input-border': 'var(--input-border)',
        'input-disabled': 'var(--input-disabled-bg)',
        // 侧边栏相关
        'sidebar-hover': 'var(--sidebar-bg-hover)',
        'sidebar-text': 'var(--sidebar-text)',
        'sidebar-active': 'var(--sidebar-text-active)',
        'sidebar-active-bg': 'var(--sidebar-active-bg)',
        'sidebar-border': 'var(--sidebar-border)',
      },
      // 字体配置
      fontFamily: {
        sans: 'var(--font-family)',
      },
      // 字体大小配置
      fontSize: {
        'xs': 'var(--font-size-small)',
        'base': 'var(--font-size-base)',
        'lg': 'var(--font-size-large)',
        'xl': 'var(--font-size-extra-large)',
        '2xl': 'var(--font-size-title)',
      },
      // 间距配置
      spacing: {
        '18': '4.5rem',
        '22': '5.5rem',
      },
      // 圆角配置
      borderRadius: {
        'sm': '4px',
        'DEFAULT': '6px',
        'md': '8px',
        'lg': '12px',
      },
      // 阴影配置
      boxShadow: {
        'card': 'var(--card-shadow)',
        'header': 'var(--header-shadow)',
      },
      // 过渡动画
      transitionDuration: {
        'DEFAULT': '300ms',
      },
      // z-index 配置
      zIndex: {
        '60': '60',
        '70': '70',
        '80': '80',
      },
    },
  },
  plugins: [],
  // 禁用 Tailwind 的 base 样式，避免与 Element Plus 冲突
  corePlugins: {
    preflight: false,
  },
}