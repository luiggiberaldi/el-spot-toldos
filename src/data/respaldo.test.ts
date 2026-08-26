import { describe, expect, it } from 'vitest';
import { crearArchivoRespaldo, esRespaldoValido } from './respaldo';
import type { DatosCompletos } from './store';

const datos: DatosCompletos = {
  clientes: [],
  toldos: [],
  alquileres: [],
  recibos: [],
  bitacora: [],
  config: {
    negocio: { nombre: 'EL SPOT', rif: '', telefono: '', direccion: '', moneda: '$', logo: '' },
    tasaBs: 36.5,
    ultimoFolio: 0,
    ultimoFolioAlquiler: 0
  }
};

describe('respaldo de datos', () => {
  it('genera un respaldo v2 completo y válido', () => {
    const archivo = crearArchivoRespaldo(datos);
    expect(archivo.version).toBe(2);
    expect(esRespaldoValido(archivo)).toBe(true);
    expect(archivo.datos.bitacora).toEqual([]);
  });

  it('mantiene compatibilidad con respaldos v1', () => {
    const archivo = crearArchivoRespaldo(datos);
    expect(esRespaldoValido({ ...archivo, version: 1 })).toBe(true);
  });

  it('rechaza datos incompletos o de otra aplicación', () => {
    expect(esRespaldoValido(null)).toBe(false);
    expect(esRespaldoValido({ ...crearArchivoRespaldo(datos), app: 'otra-app' })).toBe(false);
    expect(esRespaldoValido({ app: 'gestor-toldos', version: 2, exportadoEn: new Date().toISOString() })).toBe(false);
  });
});
