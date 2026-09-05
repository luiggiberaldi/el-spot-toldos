import type { jsPDF } from 'jspdf';
import type { DatosRecibo } from '../types/modelos';
import { tarifaEfectiva } from './calculos';
import { formatearBsEquivalente, formatearFechaCorta, formatearMonto } from './formato';
import { enlaceMapa, formatearCoordenadas } from './geolocalizacion';
import { nombreArchivoSeguro } from './venezuela';

/** Generación del recibo digital en PDF A4 para EL SPOT. */

/**
 * jsPDF (con html2canvas y dompurify, ~374 kB) se carga bajo demanda: la PWA no
 * lo descarga al arrancar, solo al generar o ver el primer recibo. El módulo se
 * cachea para no repetir la carga en recibos sucesivos.
 */
let moduloJsPdf: Promise<typeof import('jspdf')> | null = null;

function cargarJsPdf(): Promise<typeof import('jspdf')> {
  moduloJsPdf ??= import('jspdf');
  return moduloJsPdf;
}

const MARGEN = 15;
const ANCHO_PAGINA = 210;
const ALTO_PAGINA = 297;
const ANCHO_CONTENIDO = ANCHO_PAGINA - MARGEN * 2;
const COLOR_MARCA: [number, number, number] = [56, 189, 248];
const COLOR_TEXTO: [number, number, number] = [24, 39, 58];
const COLOR_SUAVE: [number, number, number] = [91, 108, 127];
const COLOR_BORDE: [number, number, number] = [211, 222, 233];
const COLOR_FONDO: [number, number, number] = [247, 250, 253];
const COLOR_PAGINA: [number, number, number] = [255, 255, 255];

function cargarImagen(dataUrl: string): Promise<HTMLImageElement | null> {
  return new Promise((resolve) => {
    const imagen = new Image();
    imagen.onload = () => resolve(imagen);
    imagen.onerror = () => resolve(null);
    imagen.src = dataUrl;
  });
}

function formatoDeDataUrl(dataUrl: string): 'PNG' | 'JPEG' {
  if (/^data:image\/(png|webp)/i.test(dataUrl)) return 'PNG';
  return 'JPEG';
}

async function fuenteADataUrl(fuente: string): Promise<string> {
  if (fuente.startsWith('data:')) return fuente;
  const imagen = await cargarImagen(fuente);
  if (!imagen) return '';
  const canvas = document.createElement('canvas');
  canvas.width = imagen.naturalWidth;
  canvas.height = imagen.naturalHeight;
  const ctx = canvas.getContext('2d');
  if (!ctx) return '';
  ctx.drawImage(imagen, 0, 0);
  return canvas.toDataURL('image/png');
}

async function prepararLogo(dataUrl: string): Promise<{ dataUrl: string; formato: 'PNG' | 'JPEG' }> {
  if (!dataUrl) return { dataUrl: '', formato: 'PNG' };
  if (!/^data:image\/webp/i.test(dataUrl)) {
    return { dataUrl, formato: formatoDeDataUrl(dataUrl) };
  }
  const imagen = await cargarImagen(dataUrl);
  if (!imagen) return { dataUrl: '', formato: 'PNG' };
  const canvas = document.createElement('canvas');
  canvas.width = imagen.naturalWidth;
  canvas.height = imagen.naturalHeight;
  const ctx = canvas.getContext('2d');
  if (!ctx) return { dataUrl: '', formato: 'PNG' };
  ctx.drawImage(imagen, 0, 0);
  return { dataUrl: canvas.toDataURL('image/png'), formato: 'PNG' };
}

function ajustarTexto(doc: jsPDF, texto: string, ancho: number): string[] {
  const lineas = doc.splitTextToSize(texto || '—', ancho);
  return Array.isArray(lineas) ? lineas : [lineas];
}

function texto(doc: jsPDF, contenido: string, x: number, y: number, opciones: { size?: number; color?: [number, number, number]; bold?: boolean; align?: 'left' | 'center' | 'right' } = {}): void {
  const { size = 9, color = COLOR_TEXTO, bold = false, align = 'left' } = opciones;
  doc.setFont('helvetica', bold ? 'bold' : 'normal');
  doc.setFontSize(size);
  doc.setTextColor(...color);
  doc.text(contenido, x, y, { align });
}

function tituloSeccion(doc: jsPDF, titulo: string, x: number, y: number, ancho: number): number {
  texto(doc, titulo, x, y, { size: 8, color: COLOR_MARCA, bold: true });
  doc.setDrawColor(...COLOR_BORDE);
  doc.setLineWidth(0.25);
  doc.line(x, y + 3, x + ancho, y + 3);
  return y + 9;
}

function tarjeta(doc: jsPDF, x: number, y: number, ancho: number, alto: number): void {
  doc.setFillColor(...COLOR_FONDO);
  doc.setDrawColor(...COLOR_BORDE);
  doc.setLineWidth(0.25);
  doc.roundedRect(x, y, ancho, alto, 2.5, 2.5, 'FD');
}

function campoTarjeta(doc: jsPDF, clave: string, valor: string, x: number, y: number, ancho: number): number {
  texto(doc, clave.toUpperCase(), x, y, { size: 6.5, color: COLOR_SUAVE, bold: true });
  const lineas = ajustarTexto(doc, valor, ancho);
  texto(doc, lineas[0], x, y + 4.2, { size: 8.5, color: COLOR_TEXTO });
  for (let index = 1; index < lineas.length; index += 1) {
    texto(doc, lineas[index], x, y + 4.2 + index * 3.6, { size: 8.5, color: COLOR_TEXTO });
  }
  return y + 4.2 + lineas.length * 3.6 + 4;
}

/** Calcula el alto mínimo de una tarjeta según sus campos ya ajustados. */
function alturaTarjeta(doc: jsPDF, campos: Array<{ clave: string; valor: string }>, ancho: number): number {
  const altoCampos = campos.reduce((total, campo) => {
    const lineas = ajustarTexto(doc, campo.valor, ancho);
    return total + 4.2 + lineas.length * 3.6 + 4;
  }, 0);
  return Math.max(78, 8 + 9 + altoCampos + 6);
}

function montoFila(doc: jsPDF, etiqueta: string, valor: string, x: number, y: number, ancho: number, opciones: { bold?: boolean; color?: [number, number, number] } = {}): void {
  texto(doc, etiqueta, x, y, { size: 8.5, color: opciones.color ?? COLOR_SUAVE, bold: opciones.bold });
  texto(doc, valor, x + ancho, y, { size: 8.5, color: opciones.color ?? COLOR_TEXTO, bold: opciones.bold, align: 'right' });
}

/** Genera el PDF del recibo a partir de los datos congelados. */
export async function generarPdfRecibo(datos: DatosRecibo): Promise<jsPDF> {
  const { jsPDF } = await cargarJsPdf();
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
  const moneda = datos.negocio.moneda;
  doc.setFillColor(...COLOR_PAGINA);
  doc.rect(0, 0, ANCHO_PAGINA, ALTO_PAGINA, 'F');
  const estadoPagado = datos.estado === 'pagado' || (datos.alquiler.montoTotal > 0 && datos.alquiler.abono >= datos.alquiler.montoTotal);

  // Cabecera clara con el logo exclusivo del PDF centrado.
  const altoHeader = 49;
  doc.setFillColor(241, 246, 250);
  doc.rect(0, 0, ANCHO_PAGINA, altoHeader, 'F');
  const logo = await prepararLogo(await fuenteADataUrl('/logo-pdf.png'));
  if (logo.dataUrl) {
    const imagen = await cargarImagen(logo.dataUrl);
    if (imagen && imagen.naturalWidth > 0) {
      const altoLogo = 39;
      const anchoLogo = Math.min(38, (imagen.naturalWidth / imagen.naturalHeight) * altoLogo);
      doc.addImage(logo.dataUrl, logo.formato, (ANCHO_PAGINA - anchoLogo) / 2, 5, anchoLogo, altoLogo);
    }
  }
  texto(doc, `RECIBO N° ${datos.folio}`, ANCHO_PAGINA - MARGEN, 17, { size: 10.5, color: COLOR_TEXTO, bold: true, align: 'right' });
  texto(doc, `Emitido el ${formatearFechaCorta(datos.emitidoEn)}`, ANCHO_PAGINA - MARGEN, 24, { size: 7.5, color: COLOR_SUAVE, align: 'right' });
  texto(doc, estadoPagado ? 'PAGADO' : 'POR PAGAR', ANCHO_PAGINA - MARGEN, 32, { size: 8.5, color: estadoPagado ? [22, 130, 90] : [180, 115, 10], bold: true, align: 'right' });

  let y = altoHeader + 9;
  doc.setFillColor(...COLOR_MARCA);
  doc.rect(MARGEN, y, ANCHO_CONTENIDO, 1.2, 'F');
  y += 9;
  texto(doc, 'COMPROBANTE DE ALQUILER', MARGEN, y, { size: 7.5, color: COLOR_SUAVE, bold: true });
  texto(doc, datos.concepto || 'Servicio de alquiler de toldos', ANCHO_PAGINA - MARGEN, y, { size: 8, color: COLOR_TEXTO, align: 'right' });
  y += 6;

  // Bloques de información fáciles de escanear.
  const anchoTarjeta = (ANCHO_CONTENIDO - 5) / 2;
  const anchoCamposTarjeta = anchoTarjeta - 12;
  const camposCliente: Array<{ clave: string; valor: string }> = [
    { clave: 'Nombre', valor: datos.cliente.nombre || '—' },
    ...(datos.cliente.cedula ? [{ clave: 'Cédula / RIF', valor: datos.cliente.cedula }] : []),
    ...(datos.cliente.telefono ? [{ clave: 'Teléfono', valor: datos.cliente.telefono }] : []),
    ...(datos.cliente.direccion ? [{ clave: 'Dirección', valor: datos.cliente.direccion }] : [])
  ];
  const camposServicio: Array<{ clave: string; valor: string }> = [
    { clave: 'Folio de alquiler', valor: datos.alquiler.folio },
    { clave: 'Modalidad', valor: datos.alquiler.modalidad === '12h' ? '12 horas' : '24 horas' },
    ...(datos.alquiler.direccion ? [{ clave: 'Dirección del evento', valor: datos.alquiler.direccion }] : []),
    ...(datos.alquiler.referenciaUbicacion ? [{ clave: 'Referencia', valor: datos.alquiler.referenciaUbicacion }] : []),
    ...(datos.alquiler.lat !== undefined && datos.alquiler.lng !== undefined
      ? [{ clave: 'Ubicación GPS', valor: formatearCoordenadas(datos.alquiler.lat, datos.alquiler.lng) }]
      : [])
  ];
  const altoTarjeta = Math.max(
    alturaTarjeta(doc, camposCliente, anchoCamposTarjeta),
    alturaTarjeta(doc, camposServicio, anchoCamposTarjeta)
  );
  tarjeta(doc, MARGEN, y, anchoTarjeta, altoTarjeta);
  tarjeta(doc, MARGEN + anchoTarjeta + 5, y, anchoTarjeta, altoTarjeta);

  let yCliente = tituloSeccion(doc, 'CLIENTE', MARGEN + 6, y + 8, anchoCamposTarjeta);
  camposCliente.forEach((campo) => {
    yCliente = campoTarjeta(doc, campo.clave, campo.valor, MARGEN + 6, yCliente, anchoCamposTarjeta);
  });

  let yAlquiler = tituloSeccion(doc, 'DETALLE DEL SERVICIO', MARGEN + anchoTarjeta + 11, y + 8, anchoCamposTarjeta);
  camposServicio.forEach((campo) => {
    yAlquiler = campoTarjeta(doc, campo.clave, campo.valor, MARGEN + anchoTarjeta + 11, yAlquiler, anchoCamposTarjeta);
  });
  y += altoTarjeta + 10;

  // Tabla de conceptos.
  texto(doc, 'CONCEPTOS DEL ALQUILER', MARGEN, y, { size: 8, color: COLOR_MARCA, bold: true });
  y += 5;
  const xTabla = MARGEN;
  const anchoDesc = ANCHO_CONTENIDO - 69;
  const xCant = xTabla + anchoDesc + 13;
  const xTarifa = xCant + 25;
  const xSubtotal = ANCHO_PAGINA - MARGEN;
  doc.setFillColor(32, 91, 132);
  doc.roundedRect(xTabla, y, ANCHO_CONTENIDO, 8, 2, 2, 'F');
  texto(doc, 'Descripción', xTabla + 4, y + 5.2, { size: 7.5, color: [255, 255, 255], bold: true });
  texto(doc, 'CANT.', xCant, y + 5.2, { size: 7.5, color: [255, 255, 255], bold: true, align: 'right' });
  texto(doc, 'TARIFA', xTarifa, y + 5.2, { size: 7.5, color: [255, 255, 255], bold: true, align: 'right' });
  texto(doc, 'SUBTOTAL', xSubtotal - 4, y + 5.2, { size: 7.5, color: [255, 255, 255], bold: true, align: 'right' });
  y += 8;

  const factor = datos.alquiler.modalidad === '12h' ? 0.5 : 1;
  datos.alquiler.items.forEach((item, index) => {
    const lineas = ajustarTexto(doc, item.nombre, anchoDesc - 8);
    const altoFila = Math.max(9, lineas.length * 3.6 + 5);
    if (index % 2 === 0) {
      doc.setFillColor(...COLOR_FONDO);
      doc.rect(xTabla, y, ANCHO_CONTENIDO, altoFila, 'F');
    }
    lineas.forEach((linea, lineaIndex) => texto(doc, linea, xTabla + 4, y + 5 + lineaIndex * 3.6, { size: 8.5 }));
    const tarifa = tarifaEfectiva(item.tarifa, datos.alquiler.modalidad);
    texto(doc, String(item.cantidad), xCant, y + 5, { size: 8.5, align: 'right' });
    texto(doc, formatearMonto(tarifa, moneda), xTarifa, y + 5, { size: 8.5, align: 'right' });
    texto(doc, formatearMonto(tarifa * item.cantidad, moneda), xSubtotal - 4, y + 5, { size: 8.5, align: 'right' });
    y += altoFila;
  });
  doc.setDrawColor(...COLOR_BORDE);
  doc.setLineWidth(0.25);
  doc.line(xTabla, y, xTabla + ANCHO_CONTENIDO, y);
  if (factor === 0.5) {
    texto(doc, 'Modalidad de 12 horas: tarifa equivalente al 50% de la tarifa base de 24 horas.', MARGEN, y + 5, { size: 7, color: COLOR_SUAVE });
    y += 9;
  } else {
    y += 5;
  }

  // Resumen financiero: monto principal y desglose separados.
  const anchoResumen = 82;
  const xResumen = ANCHO_PAGINA - MARGEN - anchoResumen;
  const altoResumen = 38;
  doc.setFillColor(...COLOR_FONDO);
  doc.setDrawColor(...COLOR_BORDE);
  doc.roundedRect(xResumen, y, anchoResumen, altoResumen, 2.5, 2.5, 'FD');
  montoFila(doc, 'Total del alquiler', formatearMonto(datos.alquiler.montoTotal, moneda), xResumen + 6, y + 9, anchoResumen - 12);
  montoFila(doc, 'Abono recibido', formatearMonto(datos.alquiler.abono, moneda), xResumen + 6, y + 17, anchoResumen - 12);
  const saldo = Math.max(0, datos.alquiler.montoTotal - datos.alquiler.abono);
  montoFila(doc, 'Pendiente', formatearMonto(saldo, moneda), xResumen + 6, y + 27, anchoResumen - 12, { bold: true, color: saldo > 0 ? [190, 65, 30] : [22, 130, 90] });

  const xPago = MARGEN;
  const anchoPago = ANCHO_CONTENIDO - anchoResumen - 6;
  doc.setFillColor(32, 91, 132);
  doc.roundedRect(xPago, y, anchoPago, altoResumen, 2.5, 2.5, 'F');
  // Etiqueta inteligente: el abono de datos.alquiler ya es el POSTERIOR al recibo (lo fija emitirRecibo);
  // si este pago deja el alquiler saldado, el recuadro lo comunica y el monto se muestra en verde.
  const saldado = datos.alquiler.montoTotal > 0 && datos.alquiler.abono >= datos.alquiler.montoTotal;
  const etiquetaRecuadro = saldado
    ? datos.monto >= datos.alquiler.montoTotal ? 'ALQUILER PAGADO' : 'MONTO CANCELADO · ALQUILER SALDADO'
    : 'MONTO A CANCELAR';
  texto(doc, etiquetaRecuadro, xPago + 7, y + 10, { size: 7.5, color: [170, 210, 240], bold: true });
  texto(doc, formatearMonto(datos.monto, moneda), xPago + 7, y + 23, { size: 16, color: saldado ? [134, 239, 172] : [255, 255, 255], bold: true });
  const equivalente = formatearBsEquivalente(datos.monto, datos.negocio.tasaBs);
  if (equivalente) {
    texto(doc, `${equivalente} · ${formatearMonto(datos.negocio.tasaBs, 'Bs')} por 1 $`, xPago + 7, y + 31, { size: 7.5, color: [190, 202, 216] });
  }
  y += altoResumen + 9;

  // Estado y referencia del cliente en una sola línea de lectura rápida.
  const colorEstado = estadoPagado ? [22, 130, 90] as [number, number, number] : [180, 115, 10] as [number, number, number];
  texto(doc, estadoPagado ? 'PAGADO' : 'POR PAGAR', MARGEN, y, { size: 9, color: colorEstado, bold: true });
  texto(doc, `Cliente: ${datos.cliente.nombre || '—'}`, ANCHO_PAGINA - MARGEN, y, { size: 8.5, color: COLOR_TEXTO, bold: true, align: 'right' });
  y += 6;
  texto(doc, `Concepto: ${datos.concepto || 'Pago del alquiler'}`, MARGEN, y, { size: 7.5, color: COLOR_SUAVE });

  // Enlace de mapa en el pie del detalle, si existe GPS.
  if (datos.alquiler.lat !== undefined && datos.alquiler.lng !== undefined) {
    const enlace = enlaceMapa(datos.alquiler.lat, datos.alquiler.lng);
    texto(doc, 'Ubicación verificable:', MARGEN, y + 7, { size: 7.5, color: COLOR_SUAVE });
    doc.setTextColor(...COLOR_MARCA);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7.5);
    doc.textWithLink(enlace, MARGEN + 28, y + 7, { url: enlace });
    y += 8;
  }

  texto(doc, 'Este documento fue generado digitalmente.', ANCHO_PAGINA / 2, ALTO_PAGINA - 13, { size: 7, color: COLOR_SUAVE, align: 'center' });
  texto(doc, 'Gracias por su preferencia.', ANCHO_PAGINA / 2, ALTO_PAGINA - 8, { size: 7, color: COLOR_SUAVE, align: 'center' });

  return doc;
}

export function nombreArchivoRecibo(datos: DatosRecibo): string {
  const cliente = nombreArchivoSeguro(datos.cliente.nombre);
  return `Recibo-${cliente}-${datos.folio}.pdf`;
}

export async function descargarPdfRecibo(datos: DatosRecibo): Promise<void> {
  const doc = await generarPdfRecibo(datos);
  doc.save(nombreArchivoRecibo(datos));
}

export async function crearBlobPdf(datos: DatosRecibo): Promise<Blob> {
  const doc = await generarPdfRecibo(datos);
  return doc.output('blob');
}
