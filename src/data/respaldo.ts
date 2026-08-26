import type { DatosCompletos } from './store';

/**
 * Respaldo de todos los datos de la aplicación en un archivo JSON.
 * Permite exportar (descargar) e importar (restaurar) la información.
 */

export interface ArchivoRespaldo {
  app: 'gestor-toldos';
  version: 1 | 2;
  exportadoEn: string;
  datos: DatosCompletos;
}

/** Construye el archivo de respaldo con la versión y fecha de exportación. */
export function crearArchivoRespaldo(datos: DatosCompletos): ArchivoRespaldo {
  return {
    app: 'gestor-toldos',
    version: 2,
    exportadoEn: new Date().toISOString(),
    datos
  };
}

/** Descarga un archivo JSON con todos los datos. */
export function descargarRespaldo(datos: DatosCompletos): void {
  const archivo = crearArchivoRespaldo(datos);
  const blob = new Blob([JSON.stringify(archivo, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = `respaldo-toldos-${new Date().toISOString().slice(0, 10)}.json`;
  enlace.click();
  URL.revokeObjectURL(url);
}

/** Valida que un objeto leído de un archivo sea un respaldo correcto. */
export function esRespaldoValido(valor: unknown): valor is ArchivoRespaldo {
  if (typeof valor !== 'object' || valor === null) return false;
  const v = valor as Record<string, unknown>;
  const datos = v.datos as Record<string, unknown> | undefined;
  return (
    v.app === 'gestor-toldos' &&
    (v.version === 1 || v.version === 2) &&
    typeof v.exportadoEn === 'string' &&
    typeof datos === 'object' &&
    datos !== null &&
    Array.isArray(datos.clientes) &&
    Array.isArray(datos.toldos) &&
    Array.isArray(datos.alquileres) &&
    Array.isArray(datos.recibos) &&
    typeof datos.config === 'object' &&
    datos.config !== null
  );
}
