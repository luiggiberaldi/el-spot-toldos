import type { DatosRecibo } from '../types/modelos';
import { formatearBsEquivalente, formatearFechaHora, formatearMonto } from './formato';
import { numeroWhatsAppVenezolano } from './venezuela';

/** Genera el texto del mensaje de WhatsApp con el resumen del recibo. */
export function textoReciboWhatsApp(datos: DatosRecibo): string {
  const lineas = [
    `🧾 *${datos.negocio.nombre}*`,
    `Recibo N° ${datos.folio}`,
    `Estado: ${datos.estado === 'pagado' ? 'PAGADO' : 'POR PAGAR'}`,
    `Fecha: ${formatearFechaHora(datos.emitidoEn)}`,
    '',
    `Cliente: ${datos.cliente.nombre}`,
    `Concepto: ${datos.concepto}`,
    `Monto: ${formatearMonto(datos.monto, datos.negocio.moneda)}`,
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
