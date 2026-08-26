import type { EstadoAlquiler, EstadoToldo } from '../types/modelos';

const COLORES_ALQUILER: Record<EstadoAlquiler, string> = {
  activo: 'bg-blue-500/15 text-blue-300',
  entregado: 'bg-amber-500/15 text-amber-300',
  devuelto: 'bg-cyan-500/15 text-cyan-300',
  cancelado: 'bg-slate-600/30 text-slate-400'
};

const ETIQUETAS_ALQUILER: Record<EstadoAlquiler, string> = {
  activo: 'Activo',
  entregado: 'Entregado',
  devuelto: 'Devuelto',
  cancelado: 'Cancelado'
};

export function EtiquetaAlquiler({ estado }: { estado: EstadoAlquiler }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${COLORES_ALQUILER[estado]}`}
    >
      {ETIQUETAS_ALQUILER[estado]}
    </span>
  );
}

const COLORES_TOLDO: Record<EstadoToldo, string> = {
  disponible: 'bg-cyan-500/15 text-cyan-300',
  alquilado: 'bg-blue-500/15 text-blue-400',
  en_reparacion: 'bg-amber-500/15 text-amber-300',
  retirado: 'bg-slate-600/30 text-slate-400'
};

const ETIQUETAS_TOLDO: Record<EstadoToldo, string> = {
  disponible: 'Disponible',
  alquilado: 'Alquilado',
  en_reparacion: 'En reparación',
  retirado: 'Retirado'
};

export function EtiquetaToldo({ estado }: { estado: EstadoToldo }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${COLORES_TOLDO[estado]}`}
    >
      {ETIQUETAS_TOLDO[estado]}
    </span>
  );
}
