/**
 * Modelos de datos del dominio.
 *
 * Todos los registros se guardan de forma local (localStorage) y se exportan
 * como respaldo en JSON. Las fechas se almacenan como cadenas ISO 8601.
 */

/** Cliente del negocio de alquiler de toldos. */
export interface Cliente {
  id: string;
  nombre: string;
  /** Cédula, RIF o documento de identidad. */
  cedula: string;
  telefono: string;
  email: string;
  direccion: string;
  notas: string;
  creadoEn: string;
}

/** Estado físico de un toldo del inventario. */
export type EstadoToldo = 'disponible' | 'alquilado' | 'en_reparacion' | 'retirado';

/** Toldo (unidad de inventario) disponible para alquilar. */
export interface Toldo {
  id: string;
  nombre: string;
  /** Descripción del tamaño, p. ej. "4x4 m". */
  tamano: string;
  /** Precio configurado para alquiler de 24 horas. */
  tarifa: number;
  /** Precio configurado para alquiler de 12 horas. Si falta, se calcula como la mitad de tarifa. */
  tarifa12h?: number;
  /** Cantidad de unidades físicas de este modelo. Los respaldos antiguos usan 1. */
  unidades: number;
  estado: EstadoToldo;
  notas: string;
  creadoEn: string;
}

/** Línea de un alquiler: un toldo con cantidad y tarifa (congelada al momento). */
export interface ItemAlquiler {
  toldoId: string;
  cantidad: number;
  /** Precio congelado según la modalidad elegida al crear el alquiler. */
  tarifa: number;
}

/** Estado del ciclo de vida de un alquiler. */
export type EstadoAlquiler = 'activo' | 'entregado' | 'devuelto' | 'cancelado';

/** Modalidad de duración del alquiler: 12 horas o 24 horas. */
export type ModalidadAlquiler = '12h' | '24h';

/** Alquiler de uno o más toldos a un cliente. */
export interface Alquiler {
  id: string;
  /** Folio correlativo del alquiler (p. ej. "ALQ-0001"). */
  folio: string;
  clienteId: string;
  items: ItemAlquiler[];
  /** Modalidad de duración: 12 horas o 24 horas. 12h usa el precio 12 h de cada toldo (por defecto, la mitad de la base 24 h). */
  modalidad: ModalidadAlquiler;
  /** Campos legacy (alquileres antiguos con fechas); opcionales por compatibilidad. */
  fechaInicio?: string;
  fechaFin?: string;
  tiempoUso?: string;
  /** Dirección donde se instala el toldo. */
  direccion: string;
  /** Referencia libre de la ubicación (punto de entrega), opcional. */
  referenciaUbicacion?: string;
  /** Coordenadas GPS capturadas en el sitio (opcional). */
  lat?: number;
  lng?: number;
  montoTotal: number;
  abono: number;
  estado: EstadoAlquiler;
  notas: string;
  creadoEn: string;
}

/** Registro auditable de un cambio realizado en el sistema. */
export interface BitacoraEntrada {
  id: string;
  fecha: string;
  tipo: 'Nuevo' | 'Cambio' | 'Corrección';
  entidad: string;
  descripcion: string;
}

/** Estado financiero del recibo. */
export type EstadoRecibo = 'pagado' | 'por_pagar';

/** Copia congelada de los datos del recibo, para que el PDF no cambie al editar. */
export interface DatosRecibo {
  folio: string;
  emitidoEn: string;
  concepto: string;
  /** Estado congelado al emitir; opcional para compatibilidad con recibos antiguos. */
  estado?: EstadoRecibo;
  monto: number;
  negocio: {
    nombre: string;
    rif: string;
    telefono: string;
    direccion: string;
    moneda: string;
    logo: string;
    /** Tasa de cambio manual (Bs por 1 $) congelada al emitir, para el equivalente en Bs. */
    tasaBs: number;
  };
  cliente: {
    nombre: string;
    cedula: string;
    telefono: string;
    direccion: string;
  };
  alquiler: {
    folio: string;
    items: Array<{ nombre: string; cantidad: number; tarifa: number }>;
    modalidad: ModalidadAlquiler;
    fechaInicio?: string;
    fechaFin?: string;
    tiempoUso?: string;
    direccion: string;
    referenciaUbicacion?: string;
    lat?: number;
    lng?: number;
    montoTotal: number;
    abono: number;
  };
}

/** Recibo digital emitido, con folio correlativo y datos congelados. */
export interface Recibo {
  id: string;
  folio: string;
  alquilerId: string;
  emitidoEn: string;
  concepto: string;
  monto: number;
  /** Estado de pago del recibo; recibos antiguos se tratan como por pagar. */
  estado?: EstadoRecibo;
  datos: DatosRecibo;
}

/** Datos del negocio dueño de la app. */
export interface DatosNegocio {
  nombre: string;
  rif: string;
  telefono: string;
  direccion: string;
  /** Símbolo de la moneda principal (por defecto "$"). */
  moneda: string;
  /** Logo en formato data URL (base64), opcional. */
  logo: string;
}

/** Configuración general de la aplicación. */
export interface Config {
  negocio: DatosNegocio;
  /**
   * Tasa de cambio manual: cuántos Bs vale 1 $.
   * Se usa para mostrar el equivalente en Bs de todo monto en dólares.
   */
  tasaBs: number;
  /** Último folio de recibo utilizado (el siguiente es ultimoFolio + 1). */
  ultimoFolio: number;
  /** Último folio de alquiler utilizado (el siguiente es ultimoFolioAlquiler + 1). */
  ultimoFolioAlquiler: number;
}
