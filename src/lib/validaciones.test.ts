import { describe, expect, it } from 'vitest';
import type { Alquiler, Cliente, Toldo } from '../types/modelos';
import {
  puedeCambiarEstadoAlquiler,
  parsearMontoEntrada,
  unidadesDisponibles,
  unidadesOcupadas,
  validarAlquiler,
  validarToldo
} from './validaciones';

const cliente: Cliente = {
  id: 'cliente-1',
  nombre: 'Ana',
  cedula: '',
  telefono: '',
  email: '',
  direccion: '',
  notas: '',
  creadoEn: '2026-01-01T00:00:00.000Z'
};

const toldo: Toldo = {
  id: 'toldo-1',
  nombre: 'Toldo 4x4',
  tamano: '4x4 m',
  tarifa: 100,
  unidades: 3,
  estado: 'disponible',
  notas: '',
  creadoEn: '2026-01-01T00:00:00.000Z'
};

function alquiler(
  id: string,
  estado: Alquiler['estado'],
  cantidad = 1,
  overrides: Partial<Alquiler> = {}
): Alquiler {
  return {
    id,
    folio: id,
    clienteId: cliente.id,
    items: [{ toldoId: toldo.id, cantidad, tarifa: 100 }],
    modalidad: '24h',
    direccion: 'Av. Principal',
    montoTotal: cantidad * 100,
    abono: 0,
    estado,
    notas: '',
    creadoEn: '2026-01-01T00:00:00.000Z',
    ...overrides
  };
}

describe('parsearMontoEntrada', () => {
  it('acepta coma decimal y rechaza texto no numérico', () => {
    expect(parsearMontoEntrada('100,50')).toBe(100.5);
    expect(parsearMontoEntrada('')).toBe(0);
    expect(parsearMontoEntrada('100abc')).toBeNull();
  });
});

describe('disponibilidad por unidades', () => {
  it('descuenta unidades de alquileres activos y entregados', () => {
    const alquileres = [alquiler('activo', 'activo', 1), alquiler('entregado', 'entregado', 1)];
    expect(unidadesOcupadas(toldo.id, alquileres)).toBe(2);
    expect(unidadesDisponibles(toldo, alquileres)).toBe(1);
    expect(unidadesDisponibles(toldo, alquileres, 'activo')).toBe(2);
  });

  it('no ofrece toldos en reparación o retirados', () => {
    expect(unidadesDisponibles({ ...toldo, estado: 'en_reparacion' }, [])).toBe(0);
    expect(unidadesDisponibles({ ...toldo, estado: 'retirado' }, [])).toBe(0);
  });
});

describe('validarToldo', () => {
  it('rechaza reducir unidades por debajo de las comprometidas', () => {
    const error = validarToldo({ ...toldo, unidades: 1 }, [alquiler('a', 'activo', 2)]);
    expect(error).toContain('menores');
  });

  it('rechaza marcar alquilado sin alquiler activo', () => {
    const error = validarToldo({ ...toldo, estado: 'alquilado' }, []);
    expect(error).toContain('automáticamente');
  });
});

describe('validarAlquiler', () => {
  it('acepta un alquiler con dirección y total coherente', () => {
    const draft = alquiler('nuevo', 'activo');
    expect(validarAlquiler(draft, [cliente], [toldo], [])).toBeNull();
  });

  it('rechaza GPS parcial y coordenadas fuera de rango', () => {
    expect(validarAlquiler(alquiler('gps', 'activo', 1, { direccion: '', lat: 10 }), [cliente], [toldo], [])).toContain('latitud');
    expect(validarAlquiler(alquiler('gps', 'activo', 1, { direccion: 'x', lat: 95, lng: 0 }), [cliente], [toldo], [])).toContain('latitud');
  });

  it('rechaza líneas duplicadas y sobreasignación', () => {
    const duplicado = alquiler('dup', 'activo', 1, {
      items: [
        { toldoId: toldo.id, cantidad: 1, tarifa: 100 },
        { toldoId: toldo.id, cantidad: 1, tarifa: 100 }
      ],
      montoTotal: 200
    });
    expect(validarAlquiler(duplicado, [cliente], [toldo], [])).toContain('repitas');
    expect(validarAlquiler(alquiler('over', 'activo', 2), [cliente], [toldo], [alquiler('other', 'activo', 2)])).toContain('disponibles');
  });

  it('rechaza toldos no alquilables y totales manipulados', () => {
    expect(validarAlquiler(alquiler('repair', 'activo'), [cliente], [{ ...toldo, estado: 'en_reparacion' }], [])).toContain('reparación');
    expect(validarAlquiler(alquiler('bad-total', 'activo', 1, { montoTotal: 1 }), [cliente], [toldo], [])).toContain('total');
  });
});

describe('puedeCambiarEstadoAlquiler', () => {
  it('permite el ciclo operativo y bloquea reabrir alquileres cerrados', () => {
    expect(puedeCambiarEstadoAlquiler('activo', 'entregado')).toBe(true);
    expect(puedeCambiarEstadoAlquiler('entregado', 'devuelto')).toBe(true);
    expect(puedeCambiarEstadoAlquiler('activo', 'devuelto')).toBe(false);
    expect(puedeCambiarEstadoAlquiler('devuelto', 'activo')).toBe(false);
    expect(puedeCambiarEstadoAlquiler('cancelado', 'entregado')).toBe(false);
  });
});
