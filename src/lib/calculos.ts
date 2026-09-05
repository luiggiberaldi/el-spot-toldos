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
 * Las líneas ya llevan el precio de su modalidad (12 h configurado, o 50 % de la base si el
 * toldo no lo tiene). El total es la suma de las líneas SIN volver a dividir: la antigua doble
 * división del 50 % (v1.0.4 web) cobraba la cuarta parte en vez de la mitad.
 */
export function factorModalidad(_modalidad: ModalidadAlquiler): number {
  return 1;
}

/** Monto total según la modalidad: subtotal de las líneas ya calculadas. */
export function calcularMontoModalidad(subtotal: number, _modalidad: ModalidadAlquiler): number {
  return redondearMonto(subtotal);
}

/**
 * Obtiene el precio configurado para una modalidad. El toldo tiene precio 12 h propio;
 * si no quedó configurado (inventario antiguo), se usa el 50 % del precio de 24 h.
 */
export function precioToldo(toldo: { tarifa: number; tarifa12h?: number }, modalidad: ModalidadAlquiler): number {
  if (modalidad === '12h' && toldo.tarifa12h !== undefined) return redondearMonto(toldo.tarifa12h);
  return modalidad === '12h' ? redondearMonto(toldo.tarifa * 0.5) : redondearMonto(toldo.tarifa);
}

/** Tarifa efectiva de una línea de alquiler. */
export function tarifaEfectiva(tarifa: number, _modalidad: ModalidadAlquiler): number {
  return redondearMonto(tarifa);
}
