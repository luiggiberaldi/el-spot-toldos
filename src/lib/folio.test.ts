import { describe, expect, it } from 'vitest';
import {
  numeroDeFolio,
  siguienteFolio,
  siguienteFolioAlquiler
} from './folio';

describe('siguienteFolio', () => {
  it('genera el primer folio con ceros a la izquierda', () => {
    expect(siguienteFolio(0)).toBe('REC-0001');
  });

  it('incrementa correctamente', () => {
    expect(siguienteFolio(6)).toBe('REC-0007');
    expect(siguienteFolio(999)).toBe('REC-1000');
  });
});

describe('siguienteFolioAlquiler', () => {
  it('genera folios de alquiler con su propio prefijo', () => {
    expect(siguienteFolioAlquiler(5)).toBe('ALQ-0006');
  });
});

describe('numeroDeFolio', () => {
  it('extrae el número de un folio', () => {
    expect(numeroDeFolio('REC-0007')).toBe(7);
    expect(numeroDeFolio('ALQ-0012')).toBe(12);
  });

  it('devuelve 0 para folios inválidos', () => {
    expect(numeroDeFolio('sin-formato')).toBe(0);
  });
});
