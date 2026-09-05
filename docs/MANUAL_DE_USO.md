# Manual de uso de EL SPOT

Guía rápida para administrar alquileres de toldos desde la PWA o la APK Android.

## 1. Configuración inicial

1. Abre **Configuración**.
2. Introduce el nombre del negocio, RIF, teléfono y dirección.
3. Configura la tasa en Bs por cada `$ 1`. La moneda principal siempre es el dólar.
4. El logo oficial se muestra automáticamente; no se cambia desde la aplicación.
5. En Android, activa los recordatorios si deseas avisos de devolución.
6. Exporta un respaldo después de completar la configuración.

También puedes cambiar la tasa rápidamente desde la tarjeta **Tasa de cambio** del Panel. El campo acepta decimales con punto o coma y actualiza los equivalentes en Bs de toda la aplicación.

## 2. Registrar clientes

1. Abre **Clientes**.
2. Pulsa **Nuevo cliente**.
3. Completa el nombre. Cédula/RIF, teléfono, dirección y notas son opcionales.
4. El teléfono y el documento se adaptan al formato venezolano.
5. Pulsa **Guardar**.

El sistema no solicita correo electrónico. La búsqueda funciona por nombre, cédula o teléfono.

## 3. Registrar toldos

1. Abre **Toldos** y pulsa **Nuevo toldo**.
2. Indica nombre, tamaño, tarifa base de 24 horas y cantidad de unidades.
3. Usa los estados **Disponible**, **En reparación** o **Retirado** según corresponda.
4. Guarda el toldo.

El estado **Alquilado** no se selecciona manualmente: aparece automáticamente cuando una o más unidades están comprometidas en un alquiler activo o entregado. Al devolver o cancelar los alquileres, el toldo vuelve a **Disponible**. No se puede asignar más unidades de las existentes.

## 4. Crear un alquiler

1. Abre **Alquileres** y pulsa **Nuevo alquiler**.
2. Selecciona el cliente.
3. Agrega uno o más toldos, cantidad y tarifa.
4. Elige una modalidad:
   - **12 horas:** cobra el precio de 12 horas del toldo. Si el toldo no tiene precio de 12 horas configurado, equivale al 50 % del precio de 24 horas.
   - **24 horas:** cobra la tarifa base (24 horas).
5. Escribe la dirección del evento o captura la ubicación con **Capturar ubicación GPS**.
6. Revisa total, abono y pendiente de pago.
7. Selecciona el estado inicial y pulsa **Guardar alquiler**.

El GPS necesita permiso del dispositivo. Si se capturan coordenadas, se guardan latitud y longitud y se incluye un enlace al mapa en el recibo.

## 5. Emitir un recibo

1. En Alquileres, abre **Ver** en el alquiler correspondiente.
2. Pulsa **Emitir recibo**.
3. Selecciona pendiente de pago, total, abono ya recibido u otro monto.
4. Revisa el concepto.
5. Elige el estado:
   - **Pagado:** el monto se registra como abono.
   - **Por pagar:** el recibo se emite sin modificar el abono.
6. Pulsa **Confirmar y emitir**.

El recibo recibe un folio correlativo y conserva una copia de los datos en el momento de emisión. Los cambios posteriores del cliente o alquiler no alteran ese PDF.

## 6. Consultar y administrar recibos

En **Recibos** puedes:

| Acción | Resultado |
| --- | --- |
| Ver | Abre el PDF dentro de la aplicación. |
| Descargar | Guarda el PDF para imprimir o archivar. |
| WhatsApp | Abre WhatsApp con el resumen profesional y el contacto del cliente. |
| Compartir | Envía el PDF mediante el selector del sistema o del navegador. |
| Eliminar | Solicita confirmación y quita únicamente el documento de recibo. |

Eliminar un recibo no revierte automáticamente el abono del alquiler. Conserva el PDF antes de eliminarlo si necesitas respaldo documental.

El PDF usa header claro, el logo exclusivo del PDF centrado, cliente, modalidad, dirección, GPS, montos, estado de pago y equivalente en Bs. No incluye espacios de firma.

## 7. Estados del alquiler

- **Activo:** pendiente o en curso.
- **Entregado:** los toldos fueron instalados.
- **Devuelto:** los toldos regresaron y se liberaron las unidades.
- **Cancelado:** la operación no se realizará y se liberan las unidades.

## 8. Respaldos

Los datos se guardan localmente en cada dispositivo. Exporta respaldos con frecuencia:

1. Abre **Configuración**.
2. En **Respaldo de datos**, pulsa **Exportar**.
3. Guarda el JSON en un lugar seguro.

Para restaurar:

1. Abre Configuración en el dispositivo destino.
2. Pulsa **Importar** y selecciona el archivo.
3. Confirma el reemplazo de los datos actuales.

El respaldo incluye clientes, inventario, alquileres, recibos, configuración y auditoría interna. La auditoría no se muestra en la navegación, pero se conserva para trazabilidad y migración.

**Restablecer todos los datos** es irreversible. Exporta antes de utilizarlo.

## 9. Actualizaciones Android

El canal oficial es [GitHub Releases de EL SPOT](https://github.com/luiggiberaldi/el-spot-toldos). La APK consulta `update.json` automáticamente cada 12 horas y también permite usar **Configuración > Actualizaciones > Buscar actualización**.

Cuando encuentra un `versionCode` mayor, descarga la APK por HTTPS, valida su SHA-256, muestra una notificación y abre el instalador oficial de Android para que confirmes. Si Android solicita autorización para instalar desde esta fuente, actívala en Ajustes y vuelve a EL SPOT; la descarga queda pendiente y el proceso continúa.

Una actualización normal conserva datos si mantiene el mismo identificador `com.elspot.toldos`, firma de producción, `versionCode` creciente y migraciones Room. No desinstales la aplicación para actualizar. La primera instalación release puede requerir exportar el respaldo de una versión debug e importarlo después.

## 10. Preguntas frecuentes

**¿La aplicación funciona sin internet?**

Sí para la gestión local, siempre que los datos ya estén cargados. GPS, mapas, WhatsApp y futuras comprobaciones de actualización pueden necesitar conexión.

**¿La PWA y la APK comparten los datos?**

No automáticamente. Usa el respaldo JSON para migrarlos.

**¿Puedo cambiar la tasa desde el Panel?**

Sí. La tarjeta de tasa rápida guarda el valor en la misma configuración de Configuración y actualiza los equivalentes.

**¿Dónde está la Bitácora?**

Está oculta para mantener la operación simple. Sus registros siguen guardándose y exportándose internamente.
