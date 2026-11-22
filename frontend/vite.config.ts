import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const backendTarget = process.env.AURORA_BACKEND_TARGET ?? 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    proxy: {
      '/api': { target: backendTarget, changeOrigin: true },
      '/actuator': { target: backendTarget, changeOrigin: true },
    },
  },
})
