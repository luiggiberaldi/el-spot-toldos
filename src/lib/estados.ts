import type { Alquiler, EstadoToldo, Toldo } from '../types/modelos';

/**
 * Sincronización del estado del inventario con los alquileres.
 * Función pura: a partir de los alquileres activos/entregados calcula qué toldos
 * deben estar 'alquilado' y cuáles vuelven a 'disponible'.
 * Respeta los estados manuales 'en_reparacion' y 'retirado'.
 */

export function calcularEstadosToldos(alquileres: Alquiler[], toldos: Toldo[]): Toldo[] {
  const ocupados = new Set<string>();
  alquileres
    .filter((a) => a.estado === 'activo' || a.estado === 'entregado')
    .forEach((a) => a.items.forEach((item) => ocupados.add(item.toldoId)));

  return toldos.map((toldo) => {
    // Reparación y retiro son estados manuales y tienen prioridad sobre la sincronización.
    if (toldo.estado === 'en_reparacion' || toldo.estado === 'retirado') return toldo;
    if (ocupados.has(toldo.id)) return { ...toldo, estado: 'alquilado' as EstadoToldo };
    if (toldo.estado === 'alquilado') return { ...toldo, estado: 'disponible' as EstadoToldo };
    return toldo;
  });
}
