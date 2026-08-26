/** Utilidades de formato de montos y fechas en español. */

const SIMBOLOS_MONEDA: Record<string, string> = {
  Bs: 'Bs',
  $: '$',
  'US$': 'US$'
};

/**
 * Convierte una fecha ISO a objeto Date.
 * Las cadenas solo de fecha ("YYYY-MM-DD") se interpretan como medianoche UTC
 * por el estándar de JavaScript, lo que cambia el día según la zona horaria;
 * por eso les forzamos la hora local.
 */
function parsearFecha(iso: string): Date {
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) return new Date(`${iso}T00:00:00`);
  return new Date(iso);
}

/** Formatea un monto con el símbolo de moneda, p. ej. "Bs 1.250,00". */
export function formatearMonto(monto: number, moneda: string): string {
  const simbolo = SIMBOLOS_MONEDA[moneda] ?? moneda;
  const numero = new Intl.NumberFormat('es-VE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(monto);
  return `${simbolo} ${numero}`;
}

/**
 * Formatea el equivalente en bolívares de un monto en dólares según la tasa
 * manual configurada. Devuelve cadena vacía si no hay tasa (tasa ≤ 0).
 */
export function formatearBsEquivalente(monto: number, tasaBs: number): string {
  if (!tasaBs || tasaBs <= 0) return '';
  return formatearMonto(monto * tasaBs, 'Bs');
}

/**
 * Formatea un monto en la moneda principal (por defecto $) con su equivalente
 * en Bs a la tasa manual, p. ej. "$ 100,00 (Bs 3.650,00)". Si no hay tasa
 * configurada, devuelve solo la moneda principal.
 */
export function formatearMontoDual(monto: number, moneda: string, tasaBs: number): string {
  const principal = formatearMonto(monto, moneda);
  const bs = formatearBsEquivalente(monto, tasaBs);
  return bs ? `${principal} (${bs})` : principal;
}

/** Formatea una fecha ISO a texto largo, p. ej. "12 de marzo de 2026". */
export function formatearFechaLarga(iso: string): string {
  const fecha = parsearFecha(iso);
  if (isNaN(fecha.getTime())) return iso || '';
  return fecha.toLocaleDateString('es-VE', {
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  });
}

/** Formatea una fecha ISO a "dd/mm/aaaa". */
export function formatearFechaCorta(iso: string): string {
  const fecha = parsearFecha(iso);
  if (isNaN(fecha.getTime())) return iso || '';
  return fecha.toLocaleDateString('es-VE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
}

/** Formatea una fecha ISO con hora, p. ej. "12/03/2026, 3:45 p. m.". */
export function formatearFechaHora(iso: string): string {
  const fecha = new Date(iso);
  if (isNaN(fecha.getTime())) return iso || '';
  return fecha.toLocaleString('es-VE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}
