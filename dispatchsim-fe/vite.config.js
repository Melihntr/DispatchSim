import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  // Vite'a global değişkenini window olarak tanımlamasını söylüyoruz:
  define: {
    global: 'window',
  },
})