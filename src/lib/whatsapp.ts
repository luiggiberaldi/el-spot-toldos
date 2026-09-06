import type { DatosRecibo } from '../types/modelos';
import { formatearBsEquivalente, formatearFechaHora, formatearMonto } from './formato';
import { numeroWhatsAppVenezolano } from './venezuela';

/** Genera el texto del mensaje de WhatsApp con el resumen del recibo. */
export function textoReciboWhatsApp(datos: DatosRecibo): string {
  let fechaEntrega = '';
  if (datos.alquiler.fechaInicio) {
    fechaEntrega = formatearFechaHora(datos.alquiler.fechaInicio);
  } else if (datos.alquiler.creadoEn) {
    fechaEntrega = formatearFechaHora(datos.alquiler.creadoEn);
  }

  let fechaDevolucion = '';
  if (datos.alquiler.fechaDevolucion) {
    fechaDevolucion = formatearFechaHora(datos.alquiler.fechaDevolucion);
  } else if (datos.alquiler.fechaFin) {
    fechaDevolucion = formatearFechaHora(datos.alquiler.fechaFin);
  } else {
    const baseFecha = datos.alquiler.fechaInicio || datos.alquiler.creadoEn || datos.emitidoEn;
    if (baseFecha) {
      const horas = datos.alquiler.modalidad === '12h' ? 12 : 24;
      const ms = new Date(baseFecha).getTime() + horas * 3600 * 1000;
      if (!isNaN(ms)) {
        fechaDevolucion = formatearFechaHora(new Date(ms).toISOString());
      }
    }
  }

  const saldoPendiente = Math.max(0, datos.alquiler.montoTotal - datos.alquiler.abono);
  const estadoPagado = datos.estado === 'pagado' || (datos.alquiler.montoTotal > 0 && saldoPendiente === 0);
  const estadoTexto = estadoPagado ? 'PAGADO TOTALMENTE ✅' : `SE DEBE: ${formatearMonto(saldoPendiente, datos.negocio.moneda)} ⏳`;

  const lineas = [
    `🧾 *${datos.negocio.nombre}*`,
    `Recibo N° ${datos.folio}`,
    `Estado: ${estadoTexto}`,
    `Fecha: ${formatearFechaHora(datos.emitidoEn)}`,
    '',
    `Cliente: ${datos.cliente.nombre}`,
    `Concepto: ${datos.concepto}`,
    ...(fechaEntrega ? [`Fecha de entrega: ${fechaEntrega}`] : []),
    ...(fechaDevolucion ? [`Fecha de devolución: ${fechaDevolucion}`] : []),
    `Monto: ${formatearMonto(datos.monto, datos.negocio.moneda)}`,
    ...(datos.alquiler.flete && datos.alquiler.flete > 0
      ? [`Flete / Traslado: ${formatearMonto(datos.alquiler.flete, datos.negocio.moneda)}`]
      : []),
    ...(formatearBsEquivalente(datos.monto, datos.negocio.tasaBs)
      ? [`Equivalente: ${formatearBsEquivalente(datos.monto, datos.negocio.tasaBs)}`]
      : []),
    '',
    'Gracias por su preferencia. 🙌'
  ];
  return lineas.join('\n');
}

/** Abre WhatsApp con un mensaje listo para enviar al número indicado. */
export function abrirWhatsApp(telefono: string, mensaje: string): boolean {
  const numero = numeroWhatsAppVenezolano(telefono);
  if (!numero) return false;
  window.open(`https://wa.me/${numero}?text=${encodeURIComponent(mensaje)}`, '_blank', 'noopener,noreferrer');
  return true;
}

/** Comparte el PDF del recibo usando la API nativa de compartir (si está disponible). */
export async function compartirPdf(blob: Blob, nombreArchivo: string): Promise<boolean> {
  const archivo = new File([blob], nombreArchivo, { type: 'application/pdf' });
  const nav = navigator as Navigator & {
    canShare?: (datos: ShareData) => boolean;
    share?: (datos: ShareData) => Promise<void>;
  };
  if (nav.share && nav.canShare?.({ files: [archivo] })) {
    await nav.share({ files: [archivo], title: nombreArchivo });
    return true;
  }
  return false;
}
