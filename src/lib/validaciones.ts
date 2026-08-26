import type {
  Alquiler,
  Cliente,
  EstadoAlquiler,
  EstadoToldo,
  Toldo
} from '../types/modelos';
import { redondearMonto } from './calculos';

/** Convierte una entrada monetaria de formulario a número sin aceptar basura parcial. */
export function parsearMontoEntrada(valor: string): number | null {
  const normalizado = valor.trim().replace(',', '.');
  if (!normalizado) return 0;
  const numero = Number(normalizado);
  return Number.isFinite(numero) ? numero : null;
}

/** Unidades comprometidas por alquileres activos o entregados. */
export function unidadesOcupadas(
  toldoId: string,
  alquileres: Alquiler[],
  excluirAlquilerId?: string
): number {
  return alquileres
    .filter(
      (alquiler) =>
        alquiler.id !== excluirAlquilerId &&
        (alquiler.estado === 'activo' || alquiler.estado === 'entregado')
    )
    .flatMap((alquiler) => alquiler.items)
    .filter((item) => item.toldoId === toldoId)
    .reduce((total, item) => total + Math.max(0, Math.trunc(item.cantidad)), 0);
}

/** Unidades libres de un toldo, descontando opcionalmente el alquiler en edición. */
export function unidadesDisponibles(
  toldo: Toldo,
  alquileres: Alquiler[],
  excluirAlquilerId?: string
): number {
  if (toldo.estado === 'en_reparacion' || toldo.estado === 'retirado') return 0;
  return Math.max(0, Math.trunc(toldo.unidades || 1) - unidadesOcupadas(toldo.id, alquileres, excluirAlquilerId));
}

/** Valida un registro de inventario antes de persistirlo. */
export function validarToldo(toldo: Toldo, alquileres: Alquiler[]): string | null {
  if (!toldo.nombre.trim()) return 'El nombre del toldo es obligatorio.';
  if (!Number.isFinite(toldo.tarifa) || toldo.tarifa < 0) {
    return 'El precio de 24 horas debe ser un número mayor o igual a 0.';
  }
  if (toldo.tarifa12h !== undefined && (!Number.isFinite(toldo.tarifa12h) || toldo.tarifa12h < 0)) {
    return 'El precio de 12 horas debe ser un número mayor o igual a 0.';
  }
  if (!Number.isInteger(toldo.unidades) || toldo.unidades < 1) {
    return 'Las unidades deben ser un número entero mayor que 0.';
  }
  const ocupadas = unidadesOcupadas(toldo.id, alquileres);
  if (ocupadas > toldo.unidades) {
    return 'Las unidades no pueden ser menores que las ya comprometidas en alquileres activos.';
  }
  if (ocupadas > 0 && (toldo.estado === 'en_reparacion' || toldo.estado === 'retirado')) {
    return 'No puedes poner en reparación o retirar un toldo con unidades alquiladas.';
  }
  if (toldo.estado === 'alquilado' && ocupadas === 0) {
    return 'El estado alquilado se asigna automáticamente al crear un alquiler activo.';
  }
  return null;
}

/** Valida un alquiler completo contra el inventario y las relaciones actuales. */
export function validarAlquiler(
  alquiler: Alquiler,
  clientes: Cliente[],
  toldos: Toldo[],
  alquileresExistentes: Alquiler[]
): string | null {
  if (!clientes.some((cliente) => cliente.id === alquiler.clienteId)) {
    return 'El cliente seleccionado ya no existe.';
  }
  if (alquiler.items.length === 0) return 'Agrega al menos un toldo.';
  if (alquiler.items.some((item) => !item.toldoId)) return 'Selecciona un toldo en cada línea.';
  if (new Set(alquiler.items.map((item) => item.toldoId)).size !== alquiler.items.length) {
    return 'No repitas el mismo toldo en varias líneas.';
  }
  if (
    alquiler.items.some(
      (item) =>
        !Number.isInteger(item.cantidad) ||
        item.cantidad < 1 ||
        !Number.isFinite(item.tarifa) ||
        item.tarifa < 0
    )
  ) {
    return 'Revisa la cantidad y tarifa de cada toldo.';
  }
  if ((alquiler.lat === undefined) !== (alquiler.lng === undefined)) {
    return 'La ubicación GPS debe incluir latitud y longitud.';
  }
  if (!alquiler.direccion.trim() && (alquiler.lat === undefined || alquiler.lng === undefined)) {
    return 'Indica la dirección o captura la ubicación GPS.';
  }
  if (
    alquiler.lat !== undefined &&
    (!Number.isFinite(alquiler.lat) || alquiler.lat < -90 || alquiler.lat > 90)
  ) {
    return 'La latitud GPS no es válida.';
  }
  if (
    alquiler.lng !== undefined &&
    (!Number.isFinite(alquiler.lng) || alquiler.lng < -180 || alquiler.lng > 180)
  ) {
    return 'La longitud GPS no es válida.';
  }
  if (!Number.isFinite(alquiler.montoTotal) || alquiler.montoTotal < 0) {
    return 'El total del alquiler no es válido.';
  }
  if (!Number.isFinite(alquiler.abono) || alquiler.abono < 0 || alquiler.abono > alquiler.montoTotal) {
    return 'El abono no puede ser negativo ni superar el total.';
  }

  const inventario = new Map(toldos.map((toldo) => [toldo.id, toldo]));
  const alquileresParaDisponibilidad = alquileresExistentes.filter((item) => item.id !== alquiler.id);
  for (const item of alquiler.items) {
    const toldo = inventario.get(item.toldoId);
    if (!toldo) return 'Uno de los toldos seleccionados ya no existe.';
    const estadoActivo = alquiler.estado === 'activo' || alquiler.estado === 'entregado';
    if (estadoActivo && (toldo.estado === 'en_reparacion' || toldo.estado === 'retirado')) {
      return 'No puedes alquilar un toldo en reparación o retirado.';
    }
    if (item.cantidad > Math.max(1, Math.trunc(toldo.unidades || 1))) {
      return `La cantidad de ${toldo.nombre} supera sus unidades registradas.`;
    }
    if (estadoActivo && item.cantidad > unidadesDisponibles(toldo, alquileresParaDisponibilidad)) {
      return `La cantidad de ${toldo.nombre} supera las unidades disponibles.`;
    }
  }

  const subtotal = alquiler.items.reduce((total, item) => total + item.tarifa * item.cantidad, 0);
  const totalEsperado = redondearMonto(subtotal);
  if (Math.abs(totalEsperado - alquiler.montoTotal) > 0.01) {
    return 'El total no coincide con los precios de la modalidad seleccionada.';
  }
  return null;
}

/** Estados que puede recorrer un alquiler sin reabrir una operación cerrada. */
export function puedeCambiarEstadoAlquiler(
  actual: EstadoAlquiler,
  siguiente: EstadoAlquiler
): boolean {
  if (actual === siguiente) return true;
  if (actual === 'cancelado' || actual === 'devuelto') return false;
  if (actual === 'activo') return siguiente === 'entregado' || siguiente === 'cancelado';
  if (actual === 'entregado') return siguiente === 'devuelto' || siguiente === 'cancelado';
  return false;
}

/** Normaliza estados manuales después de importar o modificar inventario. */
export function estadoToldoSinAlquiler(toldo: Toldo, ocupadas: number): EstadoToldo {
  if (ocupadas > 0) return 'alquilado';
  return toldo.estado === 'alquilado' ? 'disponible' : toldo.estado;
}
