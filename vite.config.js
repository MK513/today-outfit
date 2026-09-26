import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 백엔드(Spring Boot, context-path /api)로 API 요청을 넘긴다.
    // 백엔드를 다른 포트로 띄웠다면 BACKEND_URL=http://localhost:9080 npm run dev
    proxy: {
      '/api': process.env.BACKEND_URL || 'http://localhost:8080',
    },
  },
})
