# Módulos de EL SPOT

La aplicación tiene seis módulos visibles. Todos comparten la store de datos y respetan la paleta oscura, controles accesibles e iconos profesionales.

La auditoría interna no es un módulo visible: cada operación importante sigue generando registros en `bitacora`, que se conservan localmente y se incluyen en respaldos. Las futuras releases de Android se distribuirán desde [`luiggiberaldi/el-spot-toldos`](https://github.com/luiggiberaldi/el-spot-toldos).

## Panel

Archivo: `src/modules/dashboard/Dashboard.tsx`.

- Muestra alquileres activos, ingresos del mes, pendiente de cobro y unidades disponibles.
- Lista los cinco alquileres recientes.
- Permite cambiar rápidamente la tasa manual de Bs por `$ 1`.
- Valida valores mayores o iguales a cero, acepta coma o punto decimal y permite guardar con `Enter`.
- Después de guardar, los equivalentes en Bs se actualizan en toda la aplicación.

## Clientes

Archivo: `src/modules/clientes/Clientes.tsx`.

- Registra nombre, cédula/RIF, teléfono, dirección y notas.
- El teléfono y el documento se normalizan al formato venezolano.
- No se solicita correo electrónico.
- Permite buscar por nombre, cédula o teléfono.
- La eliminación solicita confirmación y conserva los alquileres históricos asociados.

## Toldos

Archivo: `src/modules/toldos/Toldos.tsx`.

- Mantiene inventario por modelo y cantidad de unidades físicas.
- Guarda nombre, tamaño, tarifa base de 24 horas, estado y notas.
- Estados: disponible, alquilado, en reparación y retirado.
- **Alquilado** es un estado automático: se muestra cuando existen unidades comprometidas en alquileres activos o entregados.
- La disponibilidad se recalcula según alquileres activos o entregados; al devolverlos o cancelarlos, el toldo vuelve a disponible.
- No permite reducir unidades por debajo de las ya comprometidas.

## Alquileres

Archivo: `src/modules/alquileres/Alquileres.tsx`.

- Selecciona cliente y uno o más toldos con cantidad.
- Usa modalidad de **12 horas** o **24 horas**; 12 horas aplica la mitad de la tarifa base.
- Captura dirección manual y ubicación GPS, con coordenadas y enlace al mapa.
- Calcula subtotal, total, abono y pendiente de pago en dólares y Bs.
- Bloquea clientes inexistentes, toldos no disponibles, duplicados, cantidades inválidas, GPS parcial y totales manipulados.
- Gestiona los estados activo, entregado, devuelto y cancelado.
- Permite emitir recibos desde el detalle del alquiler.

## Recibos

Archivo: `src/modules/recibos/Recibos.tsx`.

- Lista recibos con folio, cliente, fecha, concepto, estado y monto.
- Genera el PDF desde un snapshot congelado.
- Permite ver, descargar, enviar por WhatsApp y compartir.
- Permite **eliminar un recibo** mediante un botón de papelera.
- Antes de eliminar muestra una confirmación con el folio y explica que el abono del alquiler no se revierte automáticamente.
- La eliminación registra la operación en la auditoría interna.
- Los estados financieros son `Pagado` y `Por pagar`.

## Configuración

Archivo: `src/modules/configuracion/Configuracion.tsx`.

- Mantiene nombre, RIF, teléfono y dirección del negocio.
- Muestra el logo oficial fijo del sistema y de los recibos.
- Configura la tasa manual Bs por `$ 1`; el dólar es siempre la moneda principal.
- Exporta e importa respaldos JSON.
- Restablece todos los datos con doble confirmación.
- La auditoría interna no se muestra aquí ni en la navegación.

## Auditoría interna y respaldos

La interfaz oculta la Bitácora para mantener una operación simple. Esto no elimina la trazabilidad:

- La PWA conserva `bitacora` dentro de Zustand y `localStorage`.
- Android conserva los registros en Room.
- Los respaldos exportan y restauran esos registros.
- Crear, editar, eliminar o restaurar datos genera entradas internas cuando corresponde.

## Utilidades principales

- `src/data/store.ts`: CRUD, persistencia, folios, validaciones y auditoría.
- `src/data/respaldo.ts`: exportación/importación JSON versión 2.
- `src/lib/calculos.ts`: tarifas por modalidad, pendientes y redondeo.
- `src/lib/formato.ts`: formatos `es-VE` para dinero y fechas.
- `src/lib/geolocalizacion.ts`: GPS y enlace a mapas.
- `src/lib/pdf.ts`: generación de recibos A4.
- `src/lib/whatsapp.ts`: mensaje y compartir del recibo.
