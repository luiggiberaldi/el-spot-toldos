import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

/**
 * Configuración exclusiva de la auditoría de responsividad.
 *
 * Usa el modo navegador de Vitest con el proveedor "playwright" apuntando al
 * Edge o Chrome ya instalado en el sistema (channel "msedge"/"chrome"), así
 * que no descarga navegadores de Playwright (instalado con
 * PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1).
 *
 *   npm run test:responsive
 *
 * El `npm test` normal (pruebas node en src) no se ve afectado: esta
 * configuración solo incluye el archivo de auditoría.
 */
export default defineConfig({
  plugins: [react()],
  test: {
    include: ['src/responsive.audit.test.tsx'],
    browser: {
      enabled: true,
      headless: true,
      provider: 'playwright',
      name: 'chromium',
      providerOptions: {
        launch: {
          // Usa el Edge (o Chrome) del sistema: sin descargas de Playwright.
          channel: process.env.NAVEGADOR_AUDITORIA ?? 'msedge'
        }
      }
    }
  }
});
