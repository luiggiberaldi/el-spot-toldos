# EL SPOT TOLDOS

Sistema para administrar un emprendimiento de alquiler de toldos desde navegador o Android. Permite registrar clientes, inventario, alquileres de 12 o 24 horas, ubicación GPS, pagos y recibos profesionales.

## Estado actual

- **PWA web:** funcional, instalable y usable sin conexión.
- **APK Android:** implementación nativa con Kotlin, Jetpack Compose, Room, DataStore, GPS, notificaciones y PDF.
- **Moneda:** dólar (`$`) como moneda principal y bolívares (`Bs`) calculados con una tasa manual.
- **Marca:** la interfaz conserva su logo oficial; los PDF usan exclusivamente `logo-pdf.png`; el icono del teléfono se conserva por separado.
- **Auditoría:** los cambios se registran internamente, pero la Bitácora no aparece en la navegación del usuario.
- **Actualizaciones Android:** la APK consulta el manifiesto `update.json` del repositorio [`luiggiberaldi/el-spot-toldos`](https://github.com/luiggiberaldi/el-spot-toldos), descarga por HTTPS, verifica SHA-256 y abre el instalador oficial.

## Módulos visibles

| Módulo | Función |
| --- | --- |
| **Panel** | Indicadores del negocio, alquileres recientes y acceso rápido para cambiar la tasa Bs. |
| **Clientes** | Alta, búsqueda, edición y eliminación de clientes con nombre, cédula/RIF, teléfono, dirección y notas. |
| **Toldos** | Inventario, unidades físicas, tarifas y estados disponible, alquilado, en reparación o retirado. |
| **Alquileres** | Modalidad de 12/24 horas, múltiples toldos, dirección, GPS, abono, pendiente de pago y ciclo operativo. |
| **Recibos** | Emisión, vista previa, PDF, descarga, WhatsApp, compartir y eliminación controlada en la PWA. |
| **Configuración** | Datos del negocio, tasa Bs, recordatorios y respaldos JSON. |

La Bitácora no es un módulo visible. Sus registros permanecen en la store web, Room en Android y archivos de respaldo para conservar trazabilidad sin ocupar espacio en la operación diaria.

## Reglas del negocio

- La tarifa guardada en el inventario corresponde a 24 horas.
- La modalidad de 12 horas aplica el 50 % de la tarifa base.
- El dólar es la moneda principal; `tasaBs` indica cuántos bolívares equivalen a `$ 1`.
- La tasa se puede cambiar desde Configuración o desde el acceso rápido del Panel.
- Cédulas/RIF y teléfonos se formatean según convenciones venezolanas.
- La dirección del evento puede escribirse manualmente o complementarse con GPS.
- El inventario bloquea cantidades superiores a las unidades disponibles.
- Un recibo conserva un snapshot de cliente, alquiler, GPS, tasa, modalidad y montos.
- `Pagado` registra el monto como abono; `Por pagar` conserva el monto pendiente.
- Eliminar un recibo elimina el documento y no revierte automáticamente un abono ya registrado.

## Recibos PDFCada recibo incluye el logo exclusivo del PDF, header claro, folio, fecha, cliente, cédula/RIF, teléfono, modalidad, dirección del evento, coordenadas GPS, detalle de toldos, total, abono, pendiente de pago, estado de pago y monto a cancelar. No incluye espacios de firma.
 La tasa usada queda congelada en el snapshot para que un cambio posterior no modifique un recibo emitido.

## Tecnología

- React 18, TypeScript y Vite.
- Tailwind CSS y `lucide-react` para la interfaz web.
- Zustand con persistencia en `localStorage`.
- jsPDF para recibos web.
- `vite-plugin-pwa` y Workbox para instalación y modo offline.
- Kotlin, Jetpack Compose, Material 3, Room y DataStore para Android.
- WorkManager para recordatorios locales.
- Vitest para pruebas deterministas.

## Desarrollo web

Requisitos: Node.js 18 o superior y npm.

```bash
npm install
npm run dev
npm run typecheck
npm test
npm run build
npm run preview
```

El servidor de desarrollo usa normalmente `http://localhost:5173`.

Para instalar la PWA en un teléfono, publica `dist/` en un hosting HTTPS y usa "Agregar a la pantalla de inicio" desde el navegador. Los datos de la PWA son locales al navegador y no se sincronizan automáticamente con Android.

## Estructura

```text
src/
  components/       Componentes compartidos y navegación visible
  data/             Zustand, persistencia y respaldos
  lib/              Cálculos, formatos, GPS, PDF y WhatsApp
  modules/          Panel, clientes, toldos, alquileres, recibos y configuración
  types/            Modelos del dominio
android-app/        Aplicación Android nativa
public/             Marca e iconos de la PWA
docs/               Guías funcionales y técnicas
BITACORA.md         Registro interno de cambios, no visible en la app
```

## Respaldos y datos

La PWA exporta un JSON con clientes, toldos, alquileres, recibos, configuración y auditoría interna. Android usa el mismo contrato `app: gestor-toldos` y formato nativo compatible. Antes de cambiar de dispositivo, restablecer datos o instalar una primera versión release, exporta un respaldo.

Una actualización normal de Android conserva los datos si mantiene el mismo `applicationId`, firma, `versionCode` creciente y migraciones Room. La primera transición desde una APK debug puede requerir exportar, desinstalar, instalar la release e importar.

## Canal oficial de actualizaciones Android

Repositorio de distribución: [`github.com/luiggiberaldi/el-spot-toldos`](https://github.com/luiggiberaldi/el-spot-toldos).

El flujo previsto es:

1. Incrementar `versionCode` y `versionName` en la configuración Android.
2. Compilar y firmar la APK con la misma keystore de producción.
3. Crear una GitHub Release con un tag como `v1.1.0` y subir la APK.
4. Actualizar `update.json` en `main` con la URL HTTPS del asset, `versionCode`, SHA-256 y notas.
5. La APK consulta el manifiesto, compara versiones, valida la descarga y muestra una notificación.
6. El usuario confirma la instalación mediante el instalador de Android.

El mecanismo está integrado, pero requiere publicar cada release firmada y actualizar `update.json`. No debe publicarse una actualización sin keystore fija, `versionCode` mayor, URL HTTPS, SHA-256 y migración de datos verificada.

## Documentación

- [Módulos](docs/MODULOS.md)
- [Arquitectura](docs/ARQUITECTURA.md)
- [Manual de uso](docs/MANUAL_DE_USO.md)
- [Referencia UI Android](docs/APK_UI_REFERENCIA.md)
- [README de la APK](android-app/README.md)
- [Bitácora interna de cambios](BITACORA.md)

Proyecto privado de uso personal.
