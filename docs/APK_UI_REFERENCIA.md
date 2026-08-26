# Referencia UI para la APK de EL SPOT

## Fuente

- Catálogo: [awesome-android-ui](https://github.com/wasabeef/awesome-android-ui)
- Copia local: `references/awesome-android-ui`
- Commit revisado: `312f9be3c50b3ea33ff3ba7eb9aa1c21b52de8b2`

El catálogo se usa como referencia visual y de patrones, no como conjunto de dependencias para copiar. Muchas librerías son antiguas; cualquier incorporación debe validar compatibilidad, licencia, mantenimiento y accesibilidad.

## Principios aplicados

- Tema oscuro de EL SPOT con fondo profundo, superficies slate, azul acero, cian, ámbar y rojo para acciones irreversibles.
- Jetpack Compose y Material 3 como base moderna.
- Drawer principal con seis destinos visibles: Panel, Clientes, Toldos, Alquileres, Recibos y Configuración.
- Bitácora no visible: la auditoría se conserva en Room y respaldos, pero no ocupa un destino de operación.
- Acciones de recibos mediante botones con iconos y diálogos de confirmación.
- Estados de carga, éxito, error, reintento y permisos explicados dentro del flujo.
- Componentes táctiles y compatibles con TalkBack.

## Equivalencias de patrones

| Necesidad | Solución actual |
| --- | --- |
| Navegación | `ModalNavigationDrawer` y `NavigationDrawerItem` de Material 3 |
| Listas | `LazyColumn` con tarjetas operativas |
| Formularios | `OutlinedTextField`, validación inline y controles Material 3 |
| Confirmaciones | Dialogs Material 3 para reset, eliminación y acciones sensibles |
| Notificaciones | WorkManager, canales y Snackbar para feedback inmediato |
| PDF | FileProvider y Sharesheet sin exponer rutas privadas |

## Flujos de producto

1. Configuración del negocio, tasa Bs y recordatorios.
2. Alta de clientes con cédula/RIF y teléfono venezolanos, sin correo.
3. Inventario de toldos con unidades físicas y estados.
4. Alquiler de 12/24 horas, GPS, abono y pendiente de pago.
5. Emisión de recibo con estado Pagado/Por pagar y snapshot.
6. Compartir por PDF, mensaje o WhatsApp.
7. Exportar/importar respaldo, incluyendo auditoría interna no visible.

## Identidad y recibos

El launcher usa `elspot_logo.png`. La marca interna y los PDF usan `marca_elspot.png`, centrada en header oscuro. El PDF muestra cliente, modalidad, dirección, GPS, tasa congelada, total, abono y estado de pago, sin espacios de firma.

## Actualizaciones

El canal oficial será [GitHub Releases de `luiggiberaldi/el-spot-toldos`](https://github.com/luiggiberaldi/el-spot-toldos). La APK deberá consultar un manifiesto HTTPS, comparar `versionCode`, notificar y abrir el instalador Android. El flujo aún requiere implementación dentro de la APK y una firma de producción estable.

Para conservar datos se debe mantener el mismo `applicationId`, la misma keystore, un `versionCode` creciente y migraciones Room consecutivas.

## Criterios de calidad

- Contraste suficiente en modo oscuro.
- Acciones destructivas con confirmación.
- Errores claros sin perder formularios.
- Permisos solicitados en contexto.
- Pruebas en teléfono pequeño, tablet, modo oscuro y TalkBack.
- Pruebas deterministas para dominio, dinero, snapshots y respaldos.
