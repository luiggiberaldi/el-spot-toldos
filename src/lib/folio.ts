/**
 * Generación de folios correlativos.
 * El siguiente folio se calcula a partir del último utilizado, de forma pura
 * para poder probarlo sin depender del almacenamiento.
 */

export const PREFIJO_FOLIO_RECIBO = 'REC';
export const PREFIJO_FOLIO_ALQUILER = 'ALQ';

/** Devuelve el siguiente folio de recibo, p. ej. "REC-0007". */
export function siguienteFolio(ultimo: number): string {
  return `${PREFIJO_FOLIO_RECIBO}-${String(ultimo + 1).padStart(4, '0')}`;
}

/** Devuelve el siguiente folio de alquiler, p. ej. "ALQ-0012". */
export function siguienteFolioAlquiler(ultimo: number): string {
  return `${PREFIJO_FOLIO_ALQUILER}-${String(ultimo + 1).padStart(4, '0')}`;
}

/** Extrae el número de un folio, p. ej. "REC-0007" → 7. */
export function numeroDeFolio(folio: string): number {
  const numero = folio.split('-')[1];
  const parsed = numero ? parseInt(numero, 10) : NaN;
  return isNaN(parsed) ? 0 : parsed;
}
