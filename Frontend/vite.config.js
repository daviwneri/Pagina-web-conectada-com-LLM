import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// O proxy encaminha /api para o Gateway, então não há problema de CORS no desenvolvimento.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
