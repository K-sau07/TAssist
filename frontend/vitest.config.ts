import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config'

// Keep the app's Vite config (aliases, plugins) and layer test settings on top,
// in a separate file so `vite build` never sees the `test` key (avoids type clashes).
export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      css: false,
    },
  }),
)
