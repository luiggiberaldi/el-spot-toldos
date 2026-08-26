import { describe, expect, it } from 'vitest';
import {
  formatearBsEquivalente,
  formatearFechaCorta,
  formatearFechaLarga,
  formatearMonto,
  formatearMontoDual
} from './formato';

describe('formatearMonto', () => {
  it('formatea bolívares con separadores venezolanos', () => {
    expect(formatearMonto(1250.5, 'Bs')).toBe('Bs 1.250,50');
  });

  it('formatea dólares con su símbolo', () => {
    expect(formatearMonto(99, '$')).toBe('$ 99,00');
    expect(formatearMonto(10.5, 'US$')).toBe('US$ 10,50');
  });

  it('usa el símbolo tal cual si no está mapeado', () => {
    expect(formatearMonto(5, '€')).toBe('€ 5,00');
  });
});

describe('formatearBsEquivalente', () => {
  it('calcula el equivalente en Bs a la tasa manual', () => {
    expect(formatearBsEquivalente(100, 36.5)).toBe('Bs 3.650,00');
  });

  it('devuelve cadena vacía si no hay tasa configurada', () => {
    expect(formatearBsEquivalente(100, 0)).toBe('');
    expect(formatearBsEquivalente(100, -1)).toBe('');
  });
});

describe('formatearMontoDual', () => {
  it('muestra solo la moneda principal sin tasa', () => {
    expect(formatearMontoDual(100, '$', 0)).toBe('$ 100,00');
  });

  it('muestra dólar y equivalente en Bs con tasa', () => {
    expect(formatearMontoDual(100, '$', 40)).toBe('$ 100,00 (Bs 4.000,00)');
  });
});

describe('formatearFechaLarga', () => {
  it('formatea una fecha ISO a texto largo en español', () => {
    expect(formatearFechaLarga('2026-03-12T00:00:00')).toBe('12 de marzo de 2026');
  });

  it('devuelve la cadena original si no es una fecha válida', () => {
    expect(formatearFechaLarga('')).toBe('');
    expect(formatearFechaLarga('invalido')).toBe('invalido');
  });
});

describe('formatearFechaCorta', () => {
  it('formatea una fecha solo de día sin cambiar la fecha por zona horaria', () => {
    // "YYYY-MM-DD" no debe descontar un día según la zona horaria local.
    expect(formatearFechaCorta('2026-08-28')).toBe('28/08/2026');
  });

  it('formatea una fecha ISO completa', () => {
    expect(formatearFechaCorta('2026-03-12T14:30:00')).toBe('12/03/2026');
  });
});
