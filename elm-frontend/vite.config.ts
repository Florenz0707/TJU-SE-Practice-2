import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
// 1. 导入 Icons 和 IconsResolver
import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [
        ElementPlusResolver(),
        // 2. 添加 IconsResolver 到 AutoImport
        IconsResolver({
          prefix: 'Icon',
        }),
      ],
    }),
    Components({
      resolvers: [
        // 3. 配置 IconsResolver 以自动注册 'i-ep' 前缀的图标组件
        IconsResolver({
          prefix: 'i', // 自定义前缀
          enabledCollections: ['ep'], // 'ep' 表示 Element Plus icons
        }),
        ElementPlusResolver()
      ],
    }),
    // 4. 添加 Icons 插件
    Icons({
      autoInstall: true,
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      // 字符串简写写法
      // '/api': 'http://localhost:8080',
      // 选项写法
      '/api': {
        target: 'http://localhost:9000/elm', // 目标后端服务地址
        changeOrigin: true, // 需要虚拟主机站点
      }
    }
  }
})
