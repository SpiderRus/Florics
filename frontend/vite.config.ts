import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/',
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    emptyOutDir: true,
    // Оптимизация chunk splitting
    rollupOptions: {
      output: {
        manualChunks: {
          // Vendor chunks - библиотеки меняются редко, кэшируются браузером
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ui-vendor': ['react-bootstrap', 'bootstrap'],
          'markdown-vendor': ['react-markdown', 'remark-gfm'],
          'utils-vendor': ['axios', 'react-toastify']
        }
      }
    },
    // Увеличиваем лимит размера chunk (для AdminPanel)
    chunkSizeWarningLimit: 1000,
    // Отключаем sourcemaps для production (уменьшает размер)
    sourcemap: false
  }
})
