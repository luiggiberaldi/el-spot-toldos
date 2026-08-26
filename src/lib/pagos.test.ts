import { describe, expect, it } from 'vitest';
import { abonoPosterior, pendienteDePago, validarMontoPago } from './pagos';

describe('reglas de pago', () => {
  it('calcula el pendiente sin negativos', () => {
    expect(pendienteDePago(100, 25)).toBe(75);
    expect(pendienteDePago(100, 100)).toBe(0);
  });

  it('permite pago parcial hasta el pendiente', () => {
    expect(validarMontoPago(50, 100, 0, true)).toBeNull();
    expect(validarMontoPago(50, 100, 60, true)).toContain('pendiente');
    expect(abonoPosterior(100, 0, 50, true)).toBe(50);
  });

  it('no permite montos inválidos o superiores al total', () => {
    expect(validarMontoPago(0, 100, 0, true)).toContain('mayor');
    expect(validarMontoPago(101, 100, 0, false)).toContain('total');
    expect(validarMontoPago(-1, 100, 0, false)).toContain('mayor');
  });

  it('no aumenta el abono cuando el recibo queda por pagar', () => {
    expect(abonoPosterior(100, 25, 50, false)).toBe(25);
  });
});
