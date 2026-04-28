import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/ai': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/rag': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/upload': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/image': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
