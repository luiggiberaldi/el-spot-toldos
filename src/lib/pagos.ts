import { calcularSaldo, redondearMonto } from './calculos';

export function pendienteDePago(total: number, abono: number): number {
  return redondearMonto(calcularSaldo(total, abono));
}

export function validarMontoPago(
  monto: number,
  total: number,
  abono: number,
  registrarAbono: boolean
): string | null {
  if (!Number.isFinite(monto) || monto <= 0) return 'El monto debe ser mayor que 0.';
  if (!Number.isFinite(total) || total < 0) return 'El total del alquiler no es válido.';
  if (!Number.isFinite(abono) || abono < 0 || abono > total) return 'El abono actual no es válido.';
  if (monto > total) return 'El monto no puede superar el total del alquiler.';
  if (registrarAbono && monto > pendienteDePago(total, abono)) {
    return 'El abono no puede superar el pendiente de pago.';
  }
  return null;
}

export function abonoPosterior(
  total: number,
  abono: number,
  monto: number,
  registrarAbono: boolean
): number {
  return registrarAbono ? redondearMonto(Math.min(total, abono + monto)) : redondearMonto(abono);
}
