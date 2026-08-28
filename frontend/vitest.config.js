import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config.js'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      coverage: {
        provider: 'v8',
        // Solo reporte, sin umbrales todavía: se fijarán cuando haya código real.
        reporter: ['text', 'html'],
        include: ['src/**/*.{js,vue}'],
        exclude: ['src/main.js', 'src/**/__tests__/**', 'src/router/**'],
      },
    },
  }),
)
