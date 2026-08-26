# Arquitectura de EL SPOT

## Resumen

EL SPOT tiene dos clientes que comparten reglas de negocio y formato de respaldo:

- **PWA web:** React, TypeScript, Vite, Tailwind, Zustand y jsPDF.
- **APK Android:** Kotlin, Jetpack Compose, Material 3, Room, DataStore y WorkManager.

Ambos clientes funcionan localmente. La PWA persiste en `localStorage`; Android persiste en Room/DataStore. No existe sincronización automática entre dispositivos.

## Navegación visible

La navegación de ambas plataformas expone únicamente:

1. Panel
2. Clientes
3. Toldos
4. Alquileres
5. Recibos
6. Configuración

La auditoría interna se conserva en el modelo y en la persistencia, pero no tiene pantalla ni entrada de menú. Esto reduce el ruido operativo sin perder registros para respaldos y diagnóstico.

## Estructura web

```text
src/
  App.tsx                         Raíz y navegación
  components/
    Layout.tsx                    Menús visibles y marca
    Modal.tsx                     Modal responsive
    Campos.tsx                    Campos y selectores
    SelectPersonalizado.tsx       Dropdown accesible propio
    Etiqueta.tsx                  Estados visuales
  data/
    store.ts                      Zustand, persistencia y auditoría
    respaldo.ts                   Respaldo JSON
  lib/
    calculos.ts                   Tarifas, modalidad, saldo y redondeo
    formato.ts                    Formatos monetarios y fechas es-VE
    folio.ts                      Folios REC/ALQ
    geolocalizacion.ts            GPS y mapas
    geocodificacion.ts            Dirección legible desde coordenadas
    pdf.ts                        PDF profesional A4
    whatsapp.ts                   WhatsApp y compartir
  modules/
    dashboard/                    Panel y tasa rápida
    clientes/                     Clientes sin correo
    toldos/                       Inventario
    alquileres/                   Alquileres, GPS y emisión
    recibos/                      Consulta, PDF y eliminación
    configuracion/                Configuración y respaldos
  types/modelos.ts                Contratos del dominio
```

## Estado y persistencia web

Zustand usa la clave `gestor-toldos-v1` con persistencia automática. La configuración contiene:

- Nombre, RIF, teléfono y dirección del negocio.
- Moneda principal `$`.
- `tasaBs`, cantidad de Bs por cada `$ 1`.
- Folio siguiente de recibo y alquiler.
- Logo visual oficial, tratado como marca fija.

La store mantiene las colecciones `clientes`, `toldos`, `alquileres`, `recibos` y `bitacora`. La auditoría se actualiza en acciones CRUD, emisión/eliminación de recibos, configuración, respaldos y restablecimiento.

## Contrato de negocio

- `ModalidadAlquiler`: `12h | 24h`.
- Tarifa del inventario: tarifa base por 24 horas.
- Tarifa de 12 horas: factor `0.5`.
- `EstadoAlquiler`: activo, entregado, devuelto o cancelado.
- `EstadoRecibo`: `pagado | por_pagar`.
- Inventario: unidades físicas y disponibilidad calculada.
- GPS: latitud y longitud opcionales; se exige el par completo si se usa.
- Cliente: nombre, cédula/RIF, teléfono, dirección y notas. El correo no forma parte del formulario operativo.

## Recibos y snapshots

Al emitir un recibo, la store asigna un folio correlativo y congela:

- Negocio y logo.
- Cliente.
- Alquiler, modalidad, líneas y dirección.
- Coordenadas GPS.
- Total, abono, saldo y tasa Bs usada.
- Estado `Pagado` o `Por pagar`.

Eliminar el recibo elimina el documento de la colección, registra la operación interna y no revierte automáticamente un abono. La anulación contable, si se necesita, debe ser una operación separada y explícita.

## PDF

La plantilla web y nativa usa header claro, el logo PDF centrado, jerarquía de información, tarjetas, tabla de conceptos, resumen financiero, estado de pago, equivalente en Bs y GPS. No incluye espacios de firma. La tasa se toma del snapshot, no de la configuración actual.

## Android

La APK se encuentra en `android-app/`:

- Room conserva clientes, toldos, alquileres, líneas, recibos y auditoría.
- DataStore conserva configuración, tasa, folios y preferencias.
- WorkManager programa recordatorios locales de devolución.
- FileProvider comparte PDFs sin exponer rutas privadas.
- El launcher conserva `elspot_logo.png`; la marca visual usa `marca_elspot.png`; los PDF usan el recurso exclusivo `logo_pdf.png`.
- Las migraciones Room son consecutivas y deben mantenerse para conservar datos.

## Respaldos

La PWA exporta un archivo JSON con `app: gestor-toldos`, `version: 2`, fecha y datos completos, incluida la auditoría interna. Android puede importar/exportar el mismo contrato y normaliza datos antiguos.

Una actualización Android debe conservar:

- `applicationId = com.elspot.toldos`.
- La misma keystore de producción.
- `versionCode` creciente.
- Migraciones Room consecutivas.
- Claves existentes de DataStore.

No se debe borrar la base de datos ni desinstalar la app para una actualización normal.

## Actualizaciones por GitHub

Repositorio oficial de distribución: [luiggiberaldi/el-spot-toldos](https://github.com/luiggiberaldi/el-spot-toldos).

El mecanismo integrado usa GitHub Releases, una APK firmada y el manifiesto HTTPS `update.json` con `versionCode`, `versionName`, URL de descarga, hash SHA-256, notas y si la actualización es obligatoria. La APK consulta el manifiesto cada 12 horas o manualmente, descarga la APK, verifica el hash y abre el instalador mediante `FileProvider`. Para cada release se debe incrementar `versionCode`, subir el asset a GitHub y actualizar `update.json` en `main`. La PWA tiene actualización automática de su service worker, pero eso no actualiza la APK.

## Calidad

Comandos web:

```bash
npm run typecheck
npm test
npm run build
```

Los flujos con GPS, permisos, WhatsApp, instalación Android y notificaciones requieren validación en un dispositivo físico o emulador además de las pruebas deterministas.
