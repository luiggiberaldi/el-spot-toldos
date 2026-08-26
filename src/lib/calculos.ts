import type { ItemAlquiler, ModalidadAlquiler } from '../types/modelos';

/**
 * Funciones puras de cálculo de montos y tiempos.
 * Al ser puras, son fáciles de probar con Vitest.
 */

/** Subtotal del alquiler: suma de tarifa × cantidad por cada toldo. */
export function calcularSubtotal(items: ItemAlquiler[]): number {
  return items.reduce((acc, item) => acc + item.tarifa * item.cantidad, 0);
}

/** Pendiente de pago: total menos abono, sin nunca ser negativo. */
export function calcularSaldo(total: number, abono: number): number {
  return Math.max(0, total - abono);
}

/**
 * Días de uso entre dos fechas (YYYY-MM-DD).
 * Devuelve 0 si falta alguna fecha o la fecha final es anterior a la inicial.
 */
export function calcularDias(fechaInicio: string, fechaFin: string): number {
  if (!fechaInicio || !fechaFin) return 0;
  const inicio = new Date(`${fechaInicio}T00:00:00`);
  const fin = new Date(`${fechaFin}T00:00:00`);
  if (isNaN(inicio.getTime()) || isNaN(fin.getTime())) return 0;
  const diff = Math.round((fin.getTime() - inicio.getTime()) / 86_400_000);
  return Math.max(0, diff);
}

/** Redondea un monto a 2 decimales para evitar errores de coma flotante. */
export function redondearMonto(monto: number): number {
  return Math.round((monto + Number.EPSILON) * 100) / 100;
}

/**
 * Factor de precio según la modalidad.
 * La tarifa base es por 24 horas; la modalidad de 12 horas cobra la mitad.
 */
export function factorModalidad(modalidad: ModalidadAlquiler): number {
  return modalidad === '12h' ? 0.5 : 1;
}

/** Monto total según la modalidad: subtotal (24h) × factor. */
export function calcularMontoModalidad(subtotal: number, modalidad: ModalidadAlquiler): number {
  return redondearMonto(subtotal * factorModalidad(modalidad));
}

/**
 * Obtiene el precio configurado para una modalidad.
 * Mantiene compatibilidad con inventario antiguo que solo tenía tarifa de 24h.
 */
export function precioToldo(toldo: { tarifa: number; tarifa12h?: number }, modalidad: ModalidadAlquiler): number {
  if (modalidad === '12h' && toldo.tarifa12h !== undefined) return redondearMonto(toldo.tarifa12h);
  return modalidad === '12h' ? redondearMonto(toldo.tarifa * 0.5) : redondearMonto(toldo.tarifa);
}

/** Tarifa efectiva histórica de una línea de alquiler. */
export function tarifaEfectiva(tarifa: number, modalidad: ModalidadAlquiler): number {
  return redondearMonto(tarifa * factorModalidad(modalidad));
}
