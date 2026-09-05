import { describe, expect, it } from 'vitest';
import {
  calcularDias,
  calcularMontoModalidad,
  calcularSaldo,
  calcularSubtotal,
  factorModalidad,
  precioToldo,
  redondearMonto,
  tarifaEfectiva
} from './calculos';

describe('calcularSubtotal', () => {
  it('suma tarifa × cantidad de cada toldo', () => {
    const items = [
      { toldoId: 'a', cantidad: 2, tarifa: 100 },
      { toldoId: 'b', cantidad: 1, tarifa: 250.5 }
    ];
    expect(calcularSubtotal(items)).toBe(450.5);
  });

  it('devuelve 0 con una lista vacía', () => {
    expect(calcularSubtotal([])).toBe(0);
  });
});

describe('calcularSaldo', () => {
  it('resta el abono del total', () => {
    expect(calcularSaldo(1000, 300)).toBe(700);
  });

  it('nunca devuelve un saldo negativo', () => {
    expect(calcularSaldo(500, 600)).toBe(0);
    expect(calcularSaldo(500, 500)).toBe(0);
  });
});

describe('calcularDias', () => {
  it('calcula días entre dos fechas', () => {
    expect(calcularDias('2026-08-25', '2026-08-28')).toBe(3);
  });

  it('devuelve 0 para el mismo día', () => {
    expect(calcularDias('2026-08-25', '2026-08-25')).toBe(0);
  });

  it('devuelve 0 si la fecha final es anterior', () => {
    expect(calcularDias('2026-08-28', '2026-08-25')).toBe(0);
  });

  it('devuelve 0 con fechas vacías o inválidas', () => {
    expect(calcularDias('', '2026-08-28')).toBe(0);
    expect(calcularDias('no-es-fecha', '2026-08-28')).toBe(0);
  });
});

describe('redondearMonto', () => {
  it('redondea a 2 decimales', () => {
    expect(redondearMonto(10.005)).toBe(10.01);
    expect(redondearMonto(10.004)).toBe(10.0);
  });
});

describe('factorModalidad', () => {
  it('la modalidad 24h cobra el 100% de la tarifa', () => {
    expect(factorModalidad('24h')).toBe(1);
  });

  it('la modalidad 12h no divide automáticamente la tarifa', () => {
    expect(factorModalidad('12h')).toBe(1);
  });
});

describe('calcularMontoModalidad', () => {
  it('mantiene el subtotal en modalidad 24h', () => {
    expect(calcularMontoModalidad(200, '24h')).toBe(200);
  });

  it('mantiene el subtotal calculado en modalidad 12h', () => {
    expect(calcularMontoModalidad(200, '12h')).toBe(200);
  });

  it('redondea correctamente montos con decimales', () => {
    expect(calcularMontoModalidad(150.5, '12h')).toBe(150.5);
    expect(calcularMontoModalidad(149.99, '12h')).toBe(149.99);
  });
});

describe('precioToldo', () => {
  it('usa precios configurados por modalidad y conserva compatibilidad', () => {
    expect(precioToldo({ tarifa: 200, tarifa12h: 80 }, '12h')).toBe(80);
    expect(precioToldo({ tarifa: 200, tarifa12h: 80 }, '24h')).toBe(200);
    expect(precioToldo({ tarifa: 200 }, '12h')).toBe(100);
  });
});

describe('tarifaEfectiva', () => {
  it('devuelve la tarifa asignada a la línea en cualquier modalidad', () => {
    expect(tarifaEfectiva(200, '24h')).toBe(200);
    expect(tarifaEfectiva(200, '12h')).toBe(200);
  });
});
