import { describe, expect, it } from 'vitest';
import { calcularEstadosToldos } from './estados';
import type { Alquiler, Toldo } from '../types/modelos';

function toldo(id: string, estado: Toldo['estado']): Toldo {
  return { id, nombre: id, tamano: '', tarifa: 0, unidades: 1, estado, notas: '', creadoEn: '' };
}

function alquiler(id: string, toldoId: string, estado: Alquiler['estado']): Alquiler {
  return {
    id,
    folio: id,
    clienteId: 'c',
    items: [{ toldoId, cantidad: 1, tarifa: 100 }],
    modalidad: '24h',
    direccion: '',
    montoTotal: 100,
    abono: 0,
    estado,
    notas: '',
    creadoEn: ''
  };
}

describe('calcularEstadosToldos', () => {
  it('marca como alquilado los toldos de alquileres activos', () => {
    const toldos = [toldo('a', 'disponible'), toldo('b', 'disponible')];
    const alquileres = [alquiler('a1', 'a', 'activo')];
    const resultado = calcularEstadosToldos(alquileres, toldos);
    expect(resultado.find((t) => t.id === 'a')?.estado).toBe('alquilado');
    expect(resultado.find((t) => t.id === 'b')?.estado).toBe('disponible');
  });

  it('considera entregado como ocupado', () => {
    const toldos = [toldo('a', 'disponible')];
    const alquileres = [alquiler('a1', 'a', 'entregado')];
    expect(calcularEstadosToldos(alquileres, toldos)[0].estado).toBe('alquilado');
  });

  it('libera el toldo al devolver o cancelar el alquiler', () => {
    const toldos = [toldo('a', 'alquilado')];
    const devueltos = [alquiler('a1', 'a', 'devuelto')];
    const cancelados = [alquiler('a1', 'a', 'cancelado')];
    expect(calcularEstadosToldos(devueltos, toldos)[0].estado).toBe('disponible');
    expect(calcularEstadosToldos(cancelados, toldos)[0].estado).toBe('disponible');
  });

  it('no libera un toldo que sigue en otro alquiler activo', () => {
    const toldos = [toldo('a', 'disponible')];
    const alquileres = [
      alquiler('a1', 'a', 'activo'),
      alquiler('a2', 'a', 'devuelto')
    ];
    expect(calcularEstadosToldos(alquileres, toldos)[0].estado).toBe('alquilado');
  });

  it('respeta los estados manuales en_reparacion y retirado', () => {
    const toldos = [toldo('a', 'en_reparacion'), toldo('b', 'retirado')];
    const resultado = calcularEstadosToldos([], toldos);
    expect(resultado[0].estado).toBe('en_reparacion');
    expect(resultado[1].estado).toBe('retirado');
  });
});
